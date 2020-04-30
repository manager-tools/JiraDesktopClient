package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.jira.provider3.app.connection.setup.JiraBaseUri;
import com.almworks.util.LocalLog;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.fx.FXUtil;
import com.almworks.util.fx.ThreadSafeProperty;
import com.almworks.util.http.WebCookieManager;
import com.almworks.util.threads.ThreadSafe;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

class DetectJiraController {
  private static final LocalLog log = LocalLog.topLevel("JIRA-detect-controller");

  private final JiraServerDetector myDetector;
  private final Function<DetectedJiraServer, String> myServerFilter;
  private final WebCookieManager myCookieManager;
  private final SimpleStringProperty myCannotConnect = new SimpleStringProperty();
  private final ThreadSafeProperty<DetectedJiraServer> myServer = new ThreadSafeProperty<>();

  public DetectJiraController(JiraServerDetector detector, Function<DetectedJiraServer, String> serverFilter, @Nullable WebCookieManager cookieManager) {
    myDetector = detector;
    myServerFilter = serverFilter;
    myCookieManager = cookieManager != null ? cookieManager : new WebCookieManager();
  }

  public ObservableStringValue cannotConnectProperty() {
    return myCannotConnect;
  }

  public ObservableObjectValue<DetectedJiraServer> serverProperty() {
    return myServer.getObservable();
  }

  @ThreadSafe
  public DetectedJiraServer getDetectedServer() {
    return myServer.getValue();
  }

  public void attachWebView(WebView webView) {
    myCookieManager.install();
    WebEngine engine = webView.getEngine();
    engine.documentProperty().addListener((observable, oldValue, newDoc) -> {
      log.debug("New document",newDoc != null ? newDoc.getDocumentURI() : "NULL");
      myDetector.cancel();
      setServerInfo(null);
      URI uri = FXUtil.getDocumentUri(newDoc);
      if (uri == null) {
        log.debug("Null document URI");
        return;
      }
      List<Cookie> cookies = myCookieManager.getCookieStore().getAllCookies();
      myDetector.detect(uri, cookies, process -> ThreadGate.FX_OPTIMAL.execute(() -> {
        if (process.isCancelled() || !process.isComplete()) {
          log.debug("Cancelled at", uri);
          return;
        }
        DetectedJiraServer server = process.getDetectedServer();
        if (server == null) {
          log.debug("No JIRA at", uri);
          setServerInfo(null);
        } else {
          JiraBaseUri baseUri = server.getBaseUri();
          URI currentUri = FXUtil.getDocumentUri(engine.getDocument());
          if (!baseUri.isSubUri(currentUri)) {
            log.debug("Stale detection result", baseUri, currentUri);
            return;
          }
          if (!WebCookieManager.areSame(cookies, server.getCookies())) {
            log.debug("Ignoring detection result - cookies changed:", baseUri);
            return;
          }
          log.debug("Detection complete at", baseUri, server);
          setServerInfo(server);
        }
      }));
    });
  }

  @ThreadSafe
  private void setServerInfo(@Nullable DetectedJiraServer info) {
    String cannotConnect = myServerFilter != null ? myServerFilter.apply(info) : null;
    log.debug("Filtering server info", info, cannotConnect);
    ThreadGate.FX_OPTIMAL.execute(() -> {
      myServer.setValue(info);
      myCannotConnect.setValue(cannotConnect);
    });
  }
}
