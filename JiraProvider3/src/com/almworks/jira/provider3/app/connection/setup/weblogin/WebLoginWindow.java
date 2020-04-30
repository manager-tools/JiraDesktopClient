package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.api.gui.FrameBuilder;
import com.almworks.api.gui.WindowController;
import com.almworks.api.http.HttpProxyInfo;
import com.almworks.api.misc.WorkArea;
import com.almworks.http.HttpMaterialFactory;
import com.almworks.http.errors.SNIErrorHandler;
import com.almworks.http.errors.SSLProblemHandler;
import com.almworks.http.errors.URLLoaderExceptionInterceptor;
import com.almworks.jira.provider3.app.connection.setup.ServerConfig;
import com.almworks.restconnector.login.AuthenticationRegister;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.LogHelper;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.fx.AwtFxWindow;
import com.almworks.util.fx.FXUtil;
import com.almworks.util.fx.PopOverHelper;
import com.almworks.util.http.WebCookieManager;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.ThreadFX;
import com.almworks.util.threads.ThreadSafe;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class WebLoginWindow {
  private final boolean myIgnoreProxy;
  private volatile boolean myOkPressed = false;
  @ThreadAWT
  private WindowController myWindowController;
  private final DetectJiraController myDetectController;

  WebLoginWindow(HttpMaterialFactory factory, boolean ignoreProxy, Function<DetectedJiraServer, String> serverFilter,
                 WebLoginConfig config, SNIErrorHandler sniErrorHandler, AuthenticationRegister authenticationRegister) {
    myIgnoreProxy = ignoreProxy;
    WebCookieManager cookieManager = config != null ? new WebCookieManager(config.getCookies()) : null;
    myDetectController = new DetectJiraController(JiraServerDetector.create(factory, ignoreProxy, sniErrorHandler, authenticationRegister), serverFilter, cookieManager);
  }

  /**
   * @param url initial URL to open in browser
   * @param dependencies platform services required by WebLogin
   * @param ignoreProxy is connection configured to ignore proxy
   * @param consumer callback - called with connected server, or null if user closed browser
   * @param windowTitle the title of the window
   * @param serverFilter filters server. If the server cannot be connected returns not-null reason.
   * @param connectHint hint for Connect button
   * @param showConnectPopOver decides when to show Connect button hint in popOver.
   * @param config previous configuration such as session cookies
   * @param onOpen called with not-null argument when window is open, with null if something went wrong
   */
  @ThreadSafe
  static void show(String url, WebLoginParams.Dependencies dependencies, boolean ignoreProxy,
                               Consumer<WebLoginWindow> consumer,
                               String windowTitle,
                               @Nullable Function<DetectedJiraServer, String> serverFilter,
                               @Nullable String connectHint,
                               @Nullable Predicate<DetectedJiraServer> showConnectPopOver,
                               @Nullable WebLoginConfig config,
                               Consumer<WindowController> onOpen) {
    JFXPanel root = new JFXPanel();
    URLLoaderExceptionInterceptor interceptor= new URLLoaderExceptionInterceptor();
    AwtFxWindow.start(
      () -> {
        WebLoginWindow wl = new WebLoginWindow(dependencies.getHttpMaterialFactory(), ignoreProxy, serverFilter, config,
          dependencies.getSNIErrorHandler(), dependencies.getAuthenticationRegister());
        wl.setupPanel(root, url, dependencies.getWorkArea(), interceptor, dependencies.getSSLProblemHandler(), connectHint, showConnectPopOver);
        return wl;
      },
      webLogin -> {
        ProxySelector saveProxySelector = ProxySelector.getDefault();
        installProxySupport(dependencies.getProxyInfo(), ignoreProxy);
        FrameBuilder builder = dependencies.getWindowManager().createFrame("WebBrowserLogin");
        builder.setContent(root);
        String title;
        if (windowTitle == null) {
          LogHelper.error("Missing window title");
          title = "Web Login";
        } else title = windowTitle;
        builder.setTitle(title);
        WindowController window = builder.showWindow(new Detach() {
          @Override
          protected void doDetach() throws Exception {
            ProxySelector.setDefault(saveProxySelector);
            consumer.accept(webLogin.myOkPressed ? webLogin : null);
          }
        });
        interceptor.install(window.getShowLife());
        webLogin.myWindowController = window;
        return window;
      },
      onOpen);
  }

  private static void installProxySupport(final HttpProxyInfo proxyInfo, final boolean ignoreProxy) {
    String host = Util.NN(proxyInfo.getProxyHost()).trim();
    int port = proxyInfo.getProxyPort();
    Proxy proxy;
    if (ignoreProxy || !proxyInfo.isUsingProxy() || host.isEmpty() || port <= 0) proxy = Proxy.NO_PROXY;
    else proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    ProxySelector.setDefault(new ProxySelector() {
      @Override
      public List<Proxy> select(URI uri) {
        return Collections.singletonList(proxy);
      }

      @Override
      public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        LogHelper.warning("WebLogin:ProxySelector#connectFailed", proxy, uri, sa, ioe);
      }
    });
  }

  @ThreadFX
  private void setupPanel(JFXPanel panel, String url, WorkArea workArea, URLLoaderExceptionInterceptor interceptor,
                          SSLProblemHandler sslProblemHandler,
                          @Nullable String connectHint,
                          @Nullable Predicate<DetectedJiraServer> showConnectPopOver) {
    WebLoginBrowser browserPanel = new WebLoginBrowser(interceptor, sslProblemHandler);
    WebView webView = browserPanel.getWebView();
    // User agent is significant when accessing some sites. Well known are:
    // 1. Any site with Google ReCaptcha. These sites needs "modern" user agent, otherwise ReCaptcha does not work and reports "unsupported browser"
    // For this reason by default JC sends customized UserAgent.
    // Example site: http://demo.cart-power.com/addons-default/preview/?demo_storefront_url=8
    // 2. Google accounts needs "unsupported old" UserAgent, otherwise it does not work - falls into permanent page reload
    // See: https://support.almworks.com/browse/ALM-1193
    // To make JC access both ReCaptcha-enabled sites and Google account, JC switches UserAgent when navigating to a new site.
    String defaultUserAgent = webView.getEngine().getUserAgent();
    String customUserAgent = Env.getString(GlobalProperties.WEB_VIEW_USER_AGENT, defaultUserAgent + " Chrome/51.0.2704.103");
    LogHelper.debug("WebLogin:userAgent. Custom:", customUserAgent, " Default:", defaultUserAgent);
    WebEngine engine = webView.getEngine();
    engine.userAgentProperty().bind(Bindings.createStringBinding(
      () -> {
        String location = engine.getLocation();
        if (location != null && Util.lower(location).startsWith("https://accounts.google.com/")) return defaultUserAgent;
        else return customUserAgent;
      },
      engine.locationProperty()));
    engine.userAgentProperty().addListener((observable, oldValue, newValue) -> LogHelper.debug("WebLogin:userAgent: switching to:", newValue));
    webView.getEngine().setUserDataDirectory(workArea.getAuxDir("webLogin/browser"));
    webView.getEngine().setOnStatusChanged(event -> LogHelper.debug("WebLogin:StatusChange:", event.toString()));
    webView.getEngine().setOnError(event -> LogHelper.warning("WWebLogin:WebView.Error:", event.getMessage(), event.getException()));

    browserPanel.getConnectButton().setOnAction(event -> ThreadGate.AWT_OPTIMAL.execute(() -> {
      myOkPressed = true;
      //noinspection ThrowableResultOfMethodCallIgnored
      myWindowController.close();
    }));

    browserPanel.attachDetector(myDetectController);
    if (connectHint != null) {
      browserPanel.getConnectButton().setTooltip(new Tooltip(connectHint));
      ConnectPopupController.install(browserPanel.getConnectButton(), connectHint, showConnectPopOver, myDetectController.serverProperty());
    }
    Scene scene  =  new Scene(browserPanel.getWholePane(), Color.ALICEBLUE);
    if (url != null && !url.isEmpty()) browserPanel.getBrowserController().navigate(url);
    panel.setScene(scene);
    panel.setPreferredSize(FXSize.PREF.calcAwtDimension(scene.getRoot()));
  }

  @ThreadSafe
  @Nullable
  public ServerConfig getServerConfig() {
    if (!myOkPressed) return null;
    DetectedJiraServer server = getDetectedServer();
    if (server == null) return null;
    return server.toServerConfig(myIgnoreProxy);
  }

  @ThreadSafe
  public DetectedJiraServer getDetectedServer() {
    if (!myOkPressed) return null;
    return myDetectController.getDetectedServer();
  }

  private static class ConnectPopupController {
    private final PopOverHelper.WithClose myPopOver;
    private final String myConnectHint;
    private final Predicate<DetectedJiraServer> myShowConnectPopOver;
    private boolean myAlreadyShown = false;

    private ConnectPopupController(Control target, String connectHint, Predicate<DetectedJiraServer> showConnectPopOver) {
      PopOverHelper popOver = new PopOverHelper(target)
              .setFill(Color.valueOf("#FFF6D7"))
              .setAutoHide(false)
              .setPopOverLocation(PopOverHelper.LOCATION_TOP_CENTER)
              .addStylesheet(FXUtil.loadCssRef(this, "/com/almworks/jira/provider3/app/connection/setup/weblogin/connectPopOver.css"));
      myPopOver = new PopOverHelper.WithClose(popOver)
              .setContentClass("message");
      myConnectHint = connectHint;
      myShowConnectPopOver = showConnectPopOver;
    }

    public static void install(Control target, String connectHint, @Nullable Predicate<DetectedJiraServer> showConnectPopOver,
                               ObservableValue<DetectedJiraServer> serverValue) {
      if (showConnectPopOver == null || target == null || connectHint == null) return;
      ConnectPopupController controller = new ConnectPopupController(target, connectHint, showConnectPopOver);
      serverValue.addListener((observable, oldValue, newValue) -> controller.onServerChanged(newValue));
    }

    private void onServerChanged(DetectedJiraServer server) {
      if (server == null) {
        myPopOver.hide();
        return;
      }
      if (myAlreadyShown) return;
      if (!myShowConnectPopOver.test(server)) return;
      myPopOver.show(new Label(myConnectHint));
      myAlreadyShown = true;
    }
  }
}
