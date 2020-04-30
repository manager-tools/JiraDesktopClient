package com.almworks.jira.provider3.app.connection.setup;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.HttpFailureConnectionException;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.util.SyncNotAllowedException;
import com.almworks.api.http.FeedbackHandler;
import com.almworks.api.http.HttpMaterial;
import com.almworks.api.http.HttpUtils;
import com.almworks.http.HttpMaterialFactory;
import com.almworks.http.errors.SNIErrorHandler;
import com.almworks.http.errors.SSLProblemHandler;
import com.almworks.jira.connector2.JiraCaptchaRequired;
import com.almworks.jira.connector2.JiraCredentialsRequiredException;
import com.almworks.jira.provider3.app.connection.JiraProvider3;
import com.almworks.jira.provider3.app.connection.ServerVersionCheck;
import com.almworks.jira.provider3.sync.download2.rest.JRProject;
import com.almworks.jira.provider3.sync.download2.rest.RestOperations;
import com.almworks.restconnector.*;
import com.almworks.restconnector.operations.LoadServerInfo;
import com.almworks.restconnector.operations.LoadUserInfo;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.spi.provider.CancelFlag;
import com.almworks.spi.provider.util.BasicHttpAuthHandler;
import com.almworks.spi.provider.util.HttpCancelledByFeedbackException;
import com.almworks.util.LocalLog;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Factory;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.net.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ConnectionChecker {
  private static final LocalLog log = LocalLog.topLevel("ConnTest");
  private static final LocalizedAccessor.MessageStr MESSAGE_CONNECTED = JiraConnectionWizard.LOCAL.messageStr("wizard.message.connected");
  private static final Factory<String> MESSAGE_ANONYMOUS = JiraConnectionWizard.LOCAL.getFactory("wizard.message.anonymous");
  private static final Factory<String> MESSAGE_PRIVATE_MODE_SHORT = JiraConnectionWizard.LOCAL.getFactory("wizard.message.privateMode.short");
  private static final Factory<String> MESSAGE_PRIVATE_MODE_FULL = JiraConnectionWizard.LOCAL.getFactory("wizard.message.privateMode.full");
  private static final Factory<String> MESSAGE_NO_INFO_SHORT = JiraConnectionWizard.LOCAL.getFactory("wizard.message.noinfo.short");
  private static final LocalizedAccessor.MessageStr MESSAGE_NO_INFO_FULL = JiraConnectionWizard.LOCAL.messageStr("wizard.message.noinfo.full");
  private static final LocalizedAccessor.MessageStr MESSAGE_REDIRECT = JiraConnectionWizard.LOCAL.messageStr("wizard.message.redirected");
  private static final Factory<String> MESSAGE_LOGIN_FAILED = JiraConnectionWizard.LOCAL.getFactory("wizard.message.loginFailed.short");
  private static final Factory<String> MESSAGE_NO_JIRA_SHORT = JiraConnectionWizard.LOCAL.getFactory("wizard.message.noJiraAtUrl.short");
  private static final LocalizedAccessor.MessageStr MESSAGE_NO_JIRA_FULL = JiraConnectionWizard.LOCAL.messageStr("wizard.message.noJiraAtUrl.full");

  private final ComponentContainer myContainer;
  private final CancelFlag myCancelFlag = new CancelFlag();
  private final ServerConfig myServerConfig;

  private final Callback myCallback;

  public ConnectionChecker(ComponentContainer container, Callback callback, ServerConfig serverConfig) {
    myContainer = container;
    myCallback = callback;
    myServerConfig = serverConfig;
  }

  @Nullable
  private String loadServerInfo(RestSession session) throws ConnectorException {
    myCancelFlag.checkCancelled();
    RestServerInfo info;
    try {
      info = RestServerInfo.get(session);
    } catch (ConnectorException e) {
      addNoServerInfoError();
      return null;
    }
    String version = info.getVersionText();
    if (!version.isEmpty()) myCallback.addMessage(false, MESSAGE_CONNECTED.formatMessage(version), null);
    if (myServerConfig.getCredentials().isAnonymous()) myCallback.addMessage(false, MESSAGE_ANONYMOUS.create(), null);
    return info.getServerTitle();
  }

  private void addNoServerInfoError() {
    myCallback.addMessage(true, MESSAGE_NO_INFO_SHORT.create(), MESSAGE_NO_INFO_FULL.formatMessage(ServerVersionCheck.EARLIEST_SUPPORTED_VERSION));
  }

  @Nullable
  private List<ConnectionTestController.Project> loadProjects(RestSession session) throws ConnectorException {
    myCancelFlag.checkCancelled();
    List<JSONObject> list = RestOperations.projectsBrief(session);
    final List<ConnectionTestController.Project> projects = Collections15.arrayList();
    for (JSONObject project : list) {
      Integer id = JRProject.ID.getValue(project);
      String name = JRProject.NAME.getValue(project);
      String key = JRProject.KEY.getValue(project);
      if (id == null || name == null || key == null) {
        log.warning("Failed to get project data", project, id, name, key);
        continue;
      }
      projects.add(new ConnectionTestController.Project(id, name, key));
    }
    if (projects.isEmpty() && myServerConfig.getCredentials().isAnonymous()) {
      myCallback.addMessage(true, MESSAGE_PRIVATE_MODE_SHORT.create(), MESSAGE_PRIVATE_MODE_FULL.create());
      return null;
    }
    Collections.sort(projects, ConnectionTestController.Project.COMPARATOR);
    reportLoadedProjects(projects.size());
    return projects;
  }

  private static final LoadServerInfo CHECK_SERVER = new LoadServerInfo(RequestPolicy.NEEDS_LOGIN);
  @Nullable
  public ServerConnectionInfo checkConnection() {
    try {
      log.debug("Starting connection test", myServerConfig.getRawBaseUrl(), myServerConfig.isSureBaseUrl());
      RestSession session = createInitSession();
      if (session == null) {
        log.debug("Connection test failed: no session");
        return null;
      }
      log.debug("Test connection session created", session.getBaseUrl());
      try {
        boolean success = true;
        boolean supported;
        RestServerInfo info = CHECK_SERVER.get(session);
        Boolean later = info.getVersion().isVersionOrLater(ServerVersionCheck.EARLIEST_SUPPORTED_VERSION);
        if (later != null)
          supported = later;
        else {
          log.error("Wrong version", ServerVersionCheck.EARLIEST_SUPPORTED_VERSION, info.getVersion().getVersion());
          supported = false;
        }
        if (!supported) throw ServerVersionCheck.notSupported();
        JiraCredentials credentials = myServerConfig.getCredentials().createUpdated(session);
        session.updateCredentials(credentials);
        log.debug("Loading projects from", session.getBaseUrl());
        List<ConnectionTestController.Project> projects = loadProjects(session);
        String title = loadServerInfo(session);
        if (projects == null || title == null) {
          log.debug("Missing projects or title", title, projects);
          success = false;
        }
        String userDisplayName = null;
        if (!credentials.isAnonymous()) {
          LoadUserInfo userInfo = LoadUserInfo.loadMe(session);
          if (userInfo == null) log.error("Failed to load userInfo");
          else userDisplayName = userInfo.getDisplayName();
        }
        log.debug("Connection complete", success, session.getBaseUrl(), userDisplayName);
        return new ServerConnectionInfo(session.getBaseUrl(), title, credentials, userDisplayName, projects, success);
      } finally {
        session.dispose();
      }
    } catch (ConnectorException e) {
      log.warning("Failed to check connection", e);
      reportException(e);
      return null;
    }
  }

  private static final String DEFAULT_PATH = "secure/dashboard.jspa";
  @Nullable
  private RestSession createInitSession() throws ConnectorException {
    RestSession session;
    String url = myServerConfig.getRawBaseUrl();
    try {
      if (!myServerConfig.isSureBaseUrl()) url = appendProtocol(url);
      session = sessionForUrl(url);
    } catch (MalformedURLException e) {
      myCallback.addMessage(true, "Bad URL: " + url, null);
      return null;
    }
    if (myServerConfig.isSureBaseUrl()) { // Skip further URL correction because of it may reach another server (via redirects)
      log.debug("Assuming base URL is correct", myServerConfig.getRawBaseUrl(), session.getBaseUrl());
      return session;
    }
    boolean success = false;
    try {
      try { // Check if REST API is available with current base URL
        CHECK_SERVER.get(session);
        // The API is available, do no further base URL updates, keep this one
        success = true;
        return session;
      } catch (ConnectorException e) {
        // ignore
      }
      RestResponse rootResponse;
      try {
        rootResponse = session.doGet("", RequestPolicy.FAILURE_ONLY);
      } catch (ConnectorException e) {
        // If user has provided wrong base URL (for example Dashboard page), a LoginCredentials fails to init the session
        // (there is no login resource at the expected URL). So we retry without login (on the second request session does not run init)
        log.debug("First attempt to load root resource has failed. Retrying", e);
        rootResponse = session.doGet("", RequestPolicy.FAILURE_ONLY);
      }
      String lastUrl = rootResponse.getLastUrl();
      if (lastUrl == null) {
        log.error("Missing url", rootResponse.getStatusCode(), session.getBaseUrl());
        success = true;
        return session;
      }
      lastUrl = Util.lower(lastUrl);
      String jiraUrl = jiraUrlFromCookies(session, rootResponse);
      if (jiraUrl != null) {
        if (session.getBaseUrl().equalsIgnoreCase(jiraUrl)) {
          success = true;
          return session;
        }
        if (!isSameHost(session.getBaseUrl(), jiraUrl)) {
          // A SSO may redirect to a different host if the session is not authenticated. And the different host may set JSESSION cookie.
          log.debug("Base URL from cookies points to a different host, ignoring", session.getBaseUrl(), jiraUrl);
          success = true;
          return session;
        }
        log.debug("Base URL from cookie differs from a configured one", session.getBaseUrl(), jiraUrl);
        try {
          log.debug("Trying cookie URL ", jiraUrl);
          RestSession newSession = sessionForUrl(jiraUrl);
          session.dispose();
          session = newSession;
          success = true;
          myCallback.addMessage(false, MESSAGE_REDIRECT.formatMessage(jiraUrl), null);
          return session;
        } catch (MalformedURLException e) {
          log.error(e, jiraUrl, lastUrl);
          success = true;
          return session;
        }
      }
      log.warning("No cookie URL detected");
      if (lastUrl.endsWith(DEFAULT_PATH)) lastUrl = lastUrl.substring(0, lastUrl.length() - DEFAULT_PATH.length());
      else { // FIX
        int index = lastUrl.indexOf("/secure/");
        if (index < 0) {
          log.warning("Unknown redirect. Assuming URL is right", lastUrl);
          success = true;
          return session;
        }
        lastUrl = lastUrl.substring(0, index + 1);
      }
      Pair<String, String> original = getJiraSite(session.getBaseUrl());
      Pair<String, String> last = getJiraSite(lastUrl);
      if (original == null || last == null) {
        success = true;
        return session;
      }
      if (!Util.equals(original.getSecond(), last.getSecond()))
        log.warning("Strange redirect to different site", session.getBaseUrl(), lastUrl);
      else if (Util.equals(original.getFirst(), last.getFirst())) {
        success = true;
        return session;
      }
      String newUrl = last.getFirst() + "://" + last.getSecond();
      try {
        log.debug("Trying redirected URL ", newUrl);
        RestSession newSession = sessionForUrl(newUrl);
        session.dispose();
        session = newSession;
      } catch (MalformedURLException e) {
        log.error(e, newUrl, lastUrl);
        success = true;
        return session;
      }
      myCallback.addMessage(false, MESSAGE_REDIRECT.formatMessage(newUrl), null);
      success = true;
      return session;
    } finally {
      if (!success) session.dispose();
    }
  }

  private static boolean isSameHost(String url1, String url2) {
    try {
      URI u1 = new URI(url1);
      URI u2 = new URI(url2);
      return u1.getHost().equalsIgnoreCase(u2.getHost()) && (u1.getPort() == u2.getPort() || u1.getPort() == -1 || u2.getPort() == -1);
    } catch (URISyntaxException e) {
      log.warning("Failed to check URLs", url1, url2);
      return false;
    }
  }

  @Nullable
  private String jiraUrlFromCookies(RestSession session, RestResponse rootResponse) {
    List<Cookie> cookies = Arrays.asList(session.getCookies());
    List<JiraBaseUri> jiraBaseUris;
    try {
      jiraBaseUris = JiraBaseUri.fromCookie(new URI(rootResponse.getLastUrl()), cookies);
    } catch (URISyntaxException e) {
      LogHelper.error(e);
      return null;
    }
    if (jiraBaseUris.isEmpty()) return null;
    if (jiraBaseUris.size() > 1)
      LogHelper.error("More than one Jira detected:", jiraBaseUris);
    return jiraBaseUris.get(0).getBaseUri().toString();
  }

  private static final Pattern URL_WITH_PROTOCOL = Pattern.compile("^https?://.*");
  private String appendProtocol(String url) throws MalformedURLException {
    if (url == null) throw new MalformedURLException("Empty url");
    if (URL_WITH_PROTOCOL.matcher(Util.lower(url)).matches()) return url;
    String httpsUrl = HttpUtils.normalizeBaseUrl(url, "https");
    RestSession session = sessionForUrl(httpsUrl);
    try {
      session.restGet("", RequestPolicy.FAILURE_ONLY);
      return httpsUrl;
    } catch (ConnectorException e) {
      log.debug("Tested HTTPS protocol. No Jira at", httpsUrl);
    }
    return HttpUtils.normalizeBaseUrl(url);
  }

  @NotNull
  private RestSession sessionForUrl(String baseUrl) throws MalformedURLException {
    if (baseUrl.trim().length() == 0) throw new MalformedURLException("Empty url");
    HttpMaterialFactory materialFactory = myContainer.getActor(HttpMaterialFactory.ROLE);
    FeedbackHandler feedbackHandler = myContainer.instantiate(BasicHttpAuthHandler.class);
    //noinspection ConstantConditions
    HttpMaterial material = materialFactory.create(feedbackHandler, myServerConfig.isIgnoreProxy(), JiraProvider3.getUserAgent());
    SSLProblemHandler sslProblemHandler = myContainer.getActor(SSLProblemHandler.ROLE);
    SNIErrorHandler sniErrorHandler = sslProblemHandler != null ? sslProblemHandler.getSNIErrorHandler() : new SNIErrorHandler(null);
    return RestSession.create(baseUrl, myServerConfig.getCredentials(), material, null, sniErrorHandler);
  }

  private static final Pattern JIRA_URL = Pattern.compile("((http|https)://)?(.*)");
  @Nullable
  private Pair<String, String> getJiraSite(String baseUrl) {
    baseUrl = Util.lower(baseUrl);
    Matcher m = JIRA_URL.matcher(baseUrl);
    if (!m.matches()) {
      log.error("Unknown url", baseUrl);
      return null;
    }
    String protocol = m.group(1).trim();
    if (protocol.isEmpty()) protocol = "http";
    else if (protocol.endsWith("://")) protocol = protocol.substring(0, protocol.length() - "://".length());
    else {
      log.error("Strange protocol", protocol, baseUrl);
      return null;
    }
    String site = m.group(3).trim();
    while (site.endsWith("/")) site = site.substring(0, site.length() - 1);
    return Pair.create(protocol, site);
  }

  private void reportLoadedProjects(final int projectsCount) {
    String message = "Accessible projects: " + projectsCount;
    myCallback.addMessage(false, message, null);
  }

  private void reportException(ConnectorException exception) {
    try {
      if (exception != null) {
        Throwable cause = exception.getCause();
        Throwable grandcause = cause == null ? null : cause.getCause();
        int httpCode = exception instanceof HttpFailureConnectionException ?
          ((HttpFailureConnectionException) exception).getStatusCode() : 0;
        if (exception instanceof CancelledException) {
          if (cause instanceof HttpCancelledByFeedbackException) reportError(cause.getMessage(), null);
        } else if (exception instanceof JiraCredentialsRequiredException) {
          if (myServerConfig != null && (myServerConfig.getCredentials() instanceof CookieJiraCredentials))
            reportError("Cannot connection Jira. Probably the session has expired.",
                    "Please, open the browser and log into Jira with your credentials to restore the connection session.");
          else
            reportError("Jira seems to be in private mode. Anonymous access is not allowed.", null);
        } else if (exception instanceof JiraCaptchaRequired) {
          reportError(exception.getShortDescription(), exception.getLongDescription());
        } else if (grandcause instanceof UnknownHostException) {
          reportError("<html><body>Cannot find host <b>" + grandcause.getMessage() + "</b>. Please check URL.", null);
        } else if (httpCode == 404) {
          reportError(MESSAGE_NO_JIRA_SHORT.create(), MESSAGE_NO_JIRA_FULL.formatMessage(myServerConfig.getRawBaseUrl()));
        } else if (httpCode == 503) {
          // proxy cannot resolve short hostname?
          reportError("Service unavailable (proxy server cannot connect to host?)", exception.getLongDescription());
        } else if (httpCode == 403) {
          reportError(MESSAGE_LOGIN_FAILED.create(), null);
        } else if (grandcause instanceof ConnectException) {
          reportError("Cannot connect to remote server.",
            "<html><body>Cannot connect to remote server:<br>" + grandcause);
        } else if (exception instanceof SyncNotAllowedException) {
          reportError(exception.getMediumDescription(), htmlify(exception.getLongDescription()));
        } else {
          reportError(exception.getShortDescription(), exception.getLongDescription());
        }
      }
    } catch (Exception e) {
      Log.error(e);
      // don't throw in finally
    }
  }

  private static String htmlify(String text) {
    if (text == null) return null;
    if (Util.lower(text).startsWith("<html>")) return text;
    return "<html><body>" + text.replaceAll("\n", "<br>") + "</body></html>";
  }

  private void reportError(String error, @Nullable String longDescription) {
    myCallback.addMessage(true, error, longDescription);
  }

  public void cancel() {
    myCancelFlag.setValue(true);
  }

  interface Callback {
    @ThreadSafe
    void addMessage(boolean problem, @NotNull String shortMessage, @Nullable String longHtml);
  }
}
