package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.WindowController;
import com.almworks.api.gui.WindowManager;
import com.almworks.api.http.HttpProxyInfo;
import com.almworks.api.misc.WorkArea;
import com.almworks.http.HttpMaterialFactory;
import com.almworks.http.errors.SNIErrorHandler;
import com.almworks.http.errors.SSLProblemHandler;
import com.almworks.restconnector.login.AuthenticationRegister;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.components.Link;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This is {@link WebLoginWindow} parameters builder.<br>
 * Also this class blocks opening of several WebLoginWindows at a moment. This is not possible due to browser uses static
 * handlers to collect cookies and implement web browser features.
 */
public class WebLoginParams {
  private static Pair<WindowController, String> ourCurrentBrowser = null;

  private final Dependencies myDependencies;
  private final boolean myIgnoreProxy;
  private final String myPurpose;
  @Nullable
  private String myWindowTitle;
  @Nullable
  private String myInitialUrl;
  @Nullable
  private Function<DetectedJiraServer, String> myServerFilter;
  @Nullable
  private Predicate<DetectedJiraServer> myConnectPopup;
  @Nullable
  private String myConnectHint;
  @Nullable
  private WebLoginConfig myConfig;

  public WebLoginParams(Dependencies dependencies, boolean ignoreProxy, String purpose) {
    myDependencies = dependencies;
    myIgnoreProxy = ignoreProxy;
    myPurpose = purpose;
  }

  /**
   * @param onOpen called on AWT thread. not null argument means the window is open. Null argument means something went wrong.
   * @param consumer called after onOpen receives not-null.
   *                 See {@link WebLoginWindow#show(String, Dependencies, boolean, Consumer, String, Function, String, Predicate, WebLoginConfig, Consumer)}
   */
  @ThreadSafe
  public void showBrowser(Consumer<WindowController> onOpen, Consumer<WebLoginWindow> consumer) {
    if (ThreadGate.AWT_IMMEDIATE.compute(() -> {
      Pair<WindowController, String> pair = getCurrentBrowser();
      if (pair != null) {
        LogHelper.debug("Another browser is open", pair);
        showCantOpen(pair.getFirst(), pair.getSecond());
        onOpen.accept(null);
        return true;
      }
      ourCurrentBrowser = Pair.create(null, myPurpose);
      return false;
    })) return;
    try {
      WebLoginWindow.show(myInitialUrl, myDependencies, myIgnoreProxy, consumer, myWindowTitle, myServerFilter,
        myConnectHint, myConnectPopup, myConfig, window -> {
          replaceCurrentBrowser(null, window);
          if (window != null)
            window.getShowLife().add(new Detach() {
              @Override
              protected void doDetach() throws Exception {
                replaceCurrentBrowser(window, null);
              }
            });
          onOpen.accept(window);
        });
    } catch (Throwable e) {
      replaceCurrentBrowser(null, null);
      throw e;
    }
  }

  /**
   * Replaces {@link #ourCurrentBrowser} if expected window matches actual window.
   * @param expectedWindow expected current browser window
   * @param newWindow new window controller or null to unlock {@link #ourCurrentBrowser}
   */
  private void replaceCurrentBrowser(WindowController expectedWindow, WindowController newWindow) {
    ThreadGate.AWT_IMMEDIATE.execute(() -> {
      Pair<WindowController, String> pair = ourCurrentBrowser;
      if (pair != null && pair.getFirst() == expectedWindow) {
        if (newWindow == null) ourCurrentBrowser = null;
        else ourCurrentBrowser = Pair.create(newWindow, pair.getSecond());
      }
    });
  }

  /**
   * Informs user that WebBrowser cannot be open due to another web browser is open at the moment
   * @param window another browsers window. May be null in rare cases when several windows attempts to open concurrently
   * @param purpose user displayable purpose of another web browser
   */
  private void showCantOpen(@Nullable WindowController window, String purpose) {
    DialogBuilder builder = myDependencies.getDialogManager().createBuilder("WebBrowserLogin.cantOpen");
    builder.setTitle("Web Browser");
    JTextArea message = new JTextArea();
    message.setColumns(30);
    message.setEditable(false);
    message.setFocusable(false);
    message.setLineWrap(true);
    message.setWrapStyleWord(true);
    message.setOpaque(false);
    message.setText("You already has another web browser open.\n" +
            "Please, close it first.\n\n" +
            "Open browser: " + purpose);
    JPanel panel = new JPanel(new BorderLayout(0, 15));
    panel.add(message, BorderLayout.CENTER);
    Link link = new Link();
    link.setText("Activate Browser");
    link.setHorizontalAlignment(SwingConstants.RIGHT);
    link.addActionListener(e -> {
      try {
        builder.closeWindow();
        if (window != null) window.activate();
      } catch (CantPerformException e1) {
        LogHelper.debug(e1);
      }
    });
    panel.add(link, BorderLayout.SOUTH);
    builder.setContent(panel);
    builder.setEmptyOkAction();
    builder.setIgnoreStoredSize(true);
    builder.setResizable(false);
    builder.setModal(true);
    builder.showWindow();
  }

  @ThreadAWT
  private static Pair<WindowController, String> getCurrentBrowser() {
    if (ourCurrentBrowser != null && !ourCurrentBrowser.getFirst().isVisible()) ourCurrentBrowser = null;
    return ourCurrentBrowser;
  }

  public WebLoginParams initialUrl(@Nullable String initialUrl) {
    myInitialUrl = initialUrl;
    return this;
  }

  public WebLoginParams setWindowTitle(@Nullable String windowTitle) {
    myWindowTitle = windowTitle;
    return this;
  }

  /**
   * The server filter examines the connected JIRA server and checks if connection to the server is allowed.<br>
   * If the connection is allowed - returns true. Otherwise returns not-null user-displayable explanation.
   * @param serverFilter server filter
   * @return this instance
   */
  public WebLoginParams serverFilter(@Nullable Function<DetectedJiraServer, String> serverFilter) {
    myServerFilter = serverFilter;
    return this;
  }

  /**
   * The predicate determines when to show {@link #setConnectHint(String) Connect button hint} in {@link com.almworks.util.fx.PopOverHelper popOver}.<br>
   * If the predicate is not provided (or null) popOver will not ever be shown.<br>
   * Otherwise it will be shown only once, when the predicate reports true for the first time.
   * @param connectPopup checks currently connected server and decides if popOver has to be shown now
   * @return this instance
   */
  public WebLoginParams setConnectPopup(@Nullable Predicate<DetectedJiraServer> connectPopup) {
    myConnectPopup = connectPopup;
    return this;
  }

  /**
   * Set the hint for Connect button.<br>
   * The hint is always shown in the tooltip. And can be shown once with popOver.
   * @param connectHint text for the hint
   * @return this instance
   * @see #setConnectPopup(Predicate)
   */
  public WebLoginParams setConnectHint(@Nullable String connectHint) {
    myConnectHint = connectHint;
    return this;
  }

  public WebLoginParams config(@Nullable WebLoginConfig config) {
    myConfig = config;
    return this;
  }

  /**
   * Checks if any wen browser is open at the moment
   * @return true if there is an active browser
   */
  @ThreadSafe
  public static boolean isAnyBrowserOpen() {
    return ThreadGate.AWT_IMMEDIATE.compute(() -> getCurrentBrowser() != null);
  }

  public static class Dependencies {
    private final HttpMaterialFactory myHttpMaterialFactory;
    private final DialogManager myDialogManager;
    private final WindowManager myWindowManager;
    private final WorkArea myWorkArea;
    private final HttpProxyInfo myProxyInfo;
    private final SSLProblemHandler mySSLProblemHandler;
    private final AuthenticationRegister myAuthenticationRegister;

    public Dependencies(HttpMaterialFactory httpMaterialFactory, DialogManager dialogManager, WindowManager windowManager, WorkArea workArea, HttpProxyInfo proxyInfo, SSLProblemHandler sslProblemHandler, AuthenticationRegister authenticationRegister) {
      myHttpMaterialFactory = httpMaterialFactory;
      myDialogManager = dialogManager;
      myWindowManager = windowManager;
      myWorkArea = workArea;
      myProxyInfo = proxyInfo;
      mySSLProblemHandler = sslProblemHandler;
      myAuthenticationRegister = authenticationRegister;
    }

    public HttpMaterialFactory getHttpMaterialFactory() {
      return myHttpMaterialFactory;
    }

    public DialogManager getDialogManager() {
      return myDialogManager;
    }

    public WindowManager getWindowManager() {
      return myWindowManager;
    }

    public WorkArea getWorkArea() {
      return myWorkArea;
    }

    public HttpProxyInfo getProxyInfo() {
      return myProxyInfo;
    }

    public static Dependencies fromContainer(ComponentContainer container) {
      return new Dependencies(container.getActor(HttpMaterialFactory.ROLE), container.getActor(DialogManager.class),
              container.getActor(WindowManager.ROLE), container.getActor(WorkArea.APPLICATION_WORK_AREA),
              container.getActor(HttpProxyInfo.ROLE), container.getActor(SSLProblemHandler.ROLE),
              container.getActor(AuthenticationRegister.ROLE));
    }

    public SNIErrorHandler getSNIErrorHandler() {
      return mySSLProblemHandler.getSNIErrorHandler();
    }

    public SSLProblemHandler getSSLProblemHandler() {
      return mySSLProblemHandler;
    }

    public AuthenticationRegister getAuthenticationRegister() {
      return myAuthenticationRegister;
    }
  }
}
