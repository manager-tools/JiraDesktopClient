package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.ConnectionException;
import com.almworks.api.http.HttpMaterial;
import com.almworks.http.HttpMaterialFactory;
import com.almworks.http.errors.SNIErrorHandler;
import com.almworks.jira.provider3.app.connection.JiraProvider3;
import com.almworks.jira.provider3.app.connection.setup.JiraBaseUri;
import com.almworks.restconnector.CookieJiraCredentials;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.login.AuthenticationRegister;
import com.almworks.restconnector.operations.LoadServerInfo;
import com.almworks.restconnector.operations.LoadUserInfo;
import com.almworks.restconnector.operations.RestAuth1Session;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LocalLog;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.http.WebCookieManager;
import org.almworks.util.Util;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class JiraServerDetector {
  private static final LocalLog log = LocalLog.topLevel("JIRA-detector");

  private final AtomicReference<MyDetection> myDetection = new AtomicReference<>(null);
  private final Supplier<HttpMaterial> myMaterialFactory;
  private final ConcurrentHashMap<Pair<String, String>, String> myDisplayableUserNames = new ConcurrentHashMap<>();
  private final SNIErrorHandler mySNIErrorHandler;
  private final AuthenticationRegister myAuthenticationRegister;

  private JiraServerDetector(Supplier<HttpMaterial> materialFactory, SNIErrorHandler sniErrorHandler, AuthenticationRegister authenticationRegister) {
    myMaterialFactory = materialFactory;
    mySNIErrorHandler = sniErrorHandler;
    myAuthenticationRegister = authenticationRegister;
  }

  public static JiraServerDetector create(HttpMaterialFactory factory, boolean ignoreProxy, SNIErrorHandler sniErrorHandler, AuthenticationRegister authenticationRegister) {
    return new JiraServerDetector(() -> factory.create(null, ignoreProxy, JiraProvider3.getUserAgent()), sniErrorHandler, authenticationRegister);
  }

  public void detect(URI uri, List<Cookie> cookies, Consumer<DetectProcess> consumer) {
    String uriHost = Util.lower(uri.getHost());
    List<Cookie> hostCookies = cookies.stream().filter(c -> {
      String domain = Util.lower(c.getDomain());
      return domain == null || uriHost.endsWith(domain);
    }).collect(Collectors.toList());
    List<JiraBaseUri> baseUris = JiraBaseUri.fromCookie(uri, hostCookies);
    if (baseUris.isEmpty()) baseUris = JiraBaseUri.fromCookie(uri, cookies);
    if (baseUris.isEmpty()) {
      log.debug("No base URL at:", uri, cookies);
      consumer.accept(DetectProcess.INVALID);
      return;
    }
    MyDetection detection = new MyDetection(this, baseUris, cookies, consumer, myMaterialFactory, mySNIErrorHandler, myAuthenticationRegister);
    while (true) {
      MyDetection prev = myDetection.get();
      if (prev != null) {
        prev.cancel();
        myDetection.compareAndSet(prev, null);
        continue;
      }
      if (myDetection.compareAndSet(null, detection)) break;
    }
    ThreadGate.LONG.execute(detection);
  }

  public void cancel() {
    MyDetection detection = myDetection.get();
    if (detection != null) {
      detection.cancel();
      myDetection.compareAndSet(detection, null);
    }
  }

  private String getDisplayableUsername(RestSession session, String username) throws ConnectorException {
    String baseUrl = session.getBaseUrl();
    Pair<String, String> key = Pair.create(baseUrl, username);
    String displayable = myDisplayableUserNames.get(key);
    if (displayable == null) {
      LoadUserInfo info = LoadUserInfo.loadUser(session, username);
      if (info == null) return null;
      displayable = info.getDisplayName();
      myDisplayableUserNames.put(key, displayable);
    }
    return displayable;
  }

  public interface DetectProcess {
    /**
     * A detection process that cannot be started or surely won't find JIRA. It is always complete with "no-JIRA" result.
     */
    DetectProcess INVALID = new DetectProcess() {

      @Nullable
      @Override
      public DetectedJiraServer getDetectedServer() {
        return null;
      }

      @Override
      public boolean isComplete() {
        return true;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }
    };

    /**
     * Result of server check. If the process is {@link #isComplete() complete} returns either detected JIRA or null if no JIRA is detected.<br>
     * If the process is not complete - returns null.
     * @return result of JIRA detection process
     */
    @Nullable
    DetectedJiraServer getDetectedServer();

    /**
     * This process is complete when it has checked the server and has a result (either JIRA detected or no-JIRA detected).
     * If the process is cancelled it may never get to "completed" state.
     * @return true if the process has result.
     */
    boolean isComplete();

    /**
     * Checks if this process is already cancelled. If the process is cancelled it still might has a result or run on and get
     * the result in the future.
     * @return true if the process is cancelled.
     */
    boolean isCancelled();
  }

  private static class MyDetection implements Runnable, DetectProcess {
    private static final String ATLAS_COOKIE = "atlassian.xsrf.token";

    private final JiraServerDetector myMaster;
    private final List<JiraBaseUri> myBaseUris;
    private final List<Cookie> myCookies;
    private final Consumer<DetectProcess> myConsumer;
    private final Supplier<HttpMaterial> myMaterialFactory;
    private final SNIErrorHandler mySNIErrorHandler;
    private final AuthenticationRegister myAuthenticationRegister;
    private boolean myCancelled = false;
    private DetectedJiraServer myResult;
    private boolean myComplete = false;

    public MyDetection(JiraServerDetector master, List<JiraBaseUri> baseUris, List<Cookie> cookies, Consumer<DetectProcess> consumer,
                       Supplier<HttpMaterial> materialFactory, SNIErrorHandler sniErrorHandler, AuthenticationRegister authenticationRegister) {
      myMaster = master;
      myBaseUris = baseUris;
      myCookies = cookies;
      myConsumer = consumer;
      myMaterialFactory = materialFactory;
      mySNIErrorHandler = sniErrorHandler;
      myAuthenticationRegister = authenticationRegister;
    }

    private DetectedJiraServer doDetect() throws Exception {
      List<Cookie> altasCookies = WebCookieManager.findCookie(myCookies, ATLAS_COOKIE);
      if (altasCookies.isEmpty()) {
        log.debug("No ATLAS_COOKIE", myBaseUris);
        return null;
      }
      List<DetectedJiraServer> jiras = new ArrayList<>();
      for (JiraBaseUri baseUri : myBaseUris) {
        DetectedJiraServer detectedJira = tryBaseUri(baseUri);
        if (detectedJira != null) jiras.add(detectedJira);
      }
      if (jiras.isEmpty()) return null;
      if (jiras.size() > 1)
        LogHelper.error("Several Jiras detected:", jiras); // todo choose the authenticated one (if this even happen)
      return jiras.get(0);
    }

    @Nullable
    private DetectedJiraServer tryBaseUri(JiraBaseUri baseUri) throws InterruptedException, ConnectorException {
      checkCancelled();
      RestSession session = baseUri.createSession(CookieJiraCredentials.establishConnection(myCookies, myAuthenticationRegister), myMaterialFactory.get(), mySNIErrorHandler);
      RestServerInfo serverInfo = null;
      String username = null;
      String displayableUsername = null;
      try {
        checkCancelled();
        RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.SAFE_TO_RETRY, true);
        if (!auth.hasUsername()) {
          log.debug("No JIRA session", session.getBaseUrl(), auth.getResult(), auth.getFailure());
          return null;
        }
        username = auth.getUsername();
        checkCancelled();
        RestResponse response = session.restGet(LoadServerInfo.PATH, RequestPolicy.SAFE_TO_RETRY);
        serverInfo = LoadServerInfo.fromResponse(response);
        if (username != null) {
          checkCancelled();
          displayableUsername = myMaster.getDisplayableUsername(session, username);
        }
      } catch (ConnectionException e) {
        log.debug("No JIRA at", session.getBaseUrl(), e.getMessage());
        // Ignore
      } finally {
        session.dispose();
      }
      return serverInfo == null ? null : new DetectedJiraServer(baseUri, myCookies, serverInfo, username, displayableUsername);
    }

    @Override
    public void run() {
      log.debug("Starting detection at", myBaseUris);
      synchronized (this) {
        if (myCancelled) {
          log.debug("Already cancelled", myBaseUris);
          return;
        }
      }
      DetectedJiraServer server;
      try {
        log.debug("Running detector", myBaseUris);
        server = doDetect();
        log.debug("Complete", myBaseUris, server);
      } catch (InterruptedException e) {
        log.debug("Interrupted", myBaseUris, e);
        server = null;
      } catch (Throwable e) {
        log.debug("Exception", myBaseUris, e);
        server = null;
      }
      synchronized (this) {
        myResult = server;
        myComplete = true;
      }
      myConsumer.accept(this);
    }

    @Nullable
    @Override
    public DetectedJiraServer getDetectedServer() {
      synchronized (this) {
        return myResult;
      }
    }

    @Override
    public boolean isComplete() {
      synchronized (this) {
        return myComplete;
      }
    }

    @Override
    public boolean isCancelled() {
      synchronized (this) {
        return myCancelled;
      }
    }

    private void checkCancelled() throws InterruptedException {
      synchronized (this) {
        if (myCancelled) throw new InterruptedException();
      }
    }

    public void cancel() {
      synchronized (this) {
        myCancelled = true;
      }
    }
  }
}
