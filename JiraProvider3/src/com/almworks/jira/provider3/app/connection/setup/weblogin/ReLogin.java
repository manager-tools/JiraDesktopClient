package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.api.engine.GlobalLoginController;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.DialogResult;
import com.almworks.api.gui.WindowController;
import com.almworks.jira.provider3.app.connection.JiraConfigHolder;
import com.almworks.jira.provider3.app.connection.JiraConfiguration;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.restconnector.login.JiraAccount;
import com.almworks.util.LocalLog;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.components.URLLink;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.concurrent.Synchronized;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ReLogin {
  private static final LocalizedAccessor I18N = WebLoginBrowser.I18N;
  public static final LocalizedAccessor.MessageStr WRONG_JIRA = I18N.messageStr("connection.filter.wrongJira");
  private final LocalLog log;

  private static final List<Cookie> CANCELLED = Collections15.unmodifiableListCopy((Cookie) null);
  private final JiraConfigHolder myMaster;
  private final ReadonlyConfiguration myConfig;

  public ReLogin(JiraConfigHolder master, ReadonlyConfiguration config) {
    myMaster = master;
    myConfig = config;
    log = LocalLog.topLevel("ReLogin").child(master.getConnection().getName() + ":" + master.getBaseUrl());
  }

  public List<Cookie> doReLogin() {
    log.debug("Relogin started");
    {
      List<Cookie> reuseCookies = findAliveSession();
      if (reuseCookies != null) {
        log.debug("Reusing alive session");
        return reuseCookies;
      }
    }
    if (WebLoginParams.isAnyBrowserOpen()) {
      log.debug("Has open browser - no relogin UI is shown.");
      return null; // Do not propose user to review session - it is not possible at the moment see JCO-1981
    }
    return myMaster.getConnection().getActor(GlobalLoginController.ROLE).updateLogin(myMaster.getConnection(), () -> {
      log.debug("Maybe ask for relogin");
      String username = Util.NN(myMaster.getJiraUsername());
      List<Cookie> reuseCookies = findAliveSession();
      if (reuseCookies != null) {
        log.debug("Reusing alive session on second attempt");
        return reuseCookies;
      }
      if (!myMaster.shouldAskReLogin()) {
        log.debug("Do not ask: has been deferred recently");
        return null;
      }
      ReadonlyConfiguration actual = myMaster.getActualConfiguration();
      if (!JiraConfiguration.isSyncSettingEqual(actual, myConfig)) {
        log.debug("Do not ask: sync settings are different");
        return null;
      }
      WebLoginConfig prevWebLogin = JiraConfiguration.getWebLogin(myConfig);
      WebLoginConfig actualWebLogin = JiraConfiguration.getWebLogin(actual);
      if (prevWebLogin == null || actualWebLogin == null || !prevWebLogin.sameConnectionSettings(actualWebLogin)) {
        log.debug("Do not ask: webLogin settings are changed");
        return null;
      }

      Synchronized<List<Cookie>> update = new Synchronized<>(null);
      List<Cookie> cookies;
      long windowClosed = -1;
      AtomicReference<Pair<Boolean, WindowController>> windowRef = new AtomicReference<>(null);
      openWindow(actualWebLogin, username, update, windowRef::set);
      int waitOpenWindowCountDown = 10;
      while (true) {
        cookies = update.get();
        if (cookies != null) break;
        Pair<Boolean, WindowController> pair = windowRef.get();
        if (pair != null) {
          if (!pair.getFirst()) {
            log.debug("Relogin not needed");
            return null;
          }
          WindowController window = pair.getSecond();
          if (window == null) {
            LogHelper.warning("Failed to open browser");
            cookies = CANCELLED;
            break;
          }
          if (!ThreadGate.AWT_IMMEDIATE.compute(window::isVisible)) {
            if (windowClosed == -1) windowClosed = System.currentTimeMillis();
            else if (System.currentTimeMillis() - windowClosed > Const.SECOND) break;
          } else windowClosed = -1;
        } else {
          waitOpenWindowCountDown--;
          LogHelper.debug("Waiting for open browser: ", waitOpenWindowCountDown);
          if (waitOpenWindowCountDown == 0) {
            LogHelper.warning("Browser still not open");
            return null;
          }
        }
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
      }
      if (cookies == CANCELLED) {
        log.debug("Browser cancelled");
        return null;
      } else {
        log.debug("New cookies are provided", cookies);
        return cookies;
      }
    });
  }

  /**
   * Searches other connections for alive session to the same server and account
   * @return list of cookies from another connection (if found).
   * null means no suitable connection, or suitable connection has no alive session.
   */
  @Nullable
  private List<Cookie> findAliveSession() {
    String baseUrl = myMaster.getBaseUrl();
    String username = myMaster.isAuthenticated() ? Util.NN(myMaster.getJiraUsername()) : null;
    return myMaster.getAuthenticationRegister().suggestCookies(JiraAccount.create(baseUrl, username));
  }

  /**
   * @param username expected JIRA account. Not-null.
   *                 Empty string for anonymous, not-empty for authenticated account
   * @param windowConsumer called on AWT thread.
   *                       First: false if user has rejected to login (second is null in the case). true means the login is required.
   *                       Second: Contains open window controller or null if something went wrong
   */
  private void openWindow(WebLoginConfig actualWebLogin, @NotNull String username, Synchronized<List<Cookie>> update,
                                      Consumer<Pair<Boolean, WindowController>> windowConsumer) {
    JiraConnection3 connection = myMaster.getConnection();
    WebLoginParams.Dependencies dependencies = WebLoginParams.Dependencies.fromContainer(connection.getContainer());
    boolean needsLogin = ThreadGate.AWT_IMMEDIATE.compute(() -> {
      if (!CantLoginForm.askUser(dependencies.getDialogManager(), myConfig)) {
        log.debug("User rejected relogin");
        windowConsumer.accept(Pair.create(false, null));
        return false;
      }
      return true;
    });
    if (!needsLogin) return;
    ServerFilter serverFilter = new ServerFilter()
      .checkUrl(JiraConfiguration.getBaseUrl(myConfig), WRONG_JIRA);
    if (username.isEmpty()) serverFilter.checkAccount(null, I18N.getString("relogin.filter.expectedAnonymous"));
    else serverFilter.checkAccount(username, I18N.messageStr("relogin.filter.wrongAccount").formatMessage(username));
    new WebLoginParams(dependencies, JiraConfiguration.isIgnoreProxy(myConfig),
      "Restore connection " + connection.getName())
      .setWindowTitle(I18N.getString("relogin.window.title"))
      .initialUrl(JiraConfiguration.getBaseUrl(myConfig))
      .serverFilter(serverFilter)
      .setConnectHint(I18N.getString("relogin.connectPopup"))
      .setConnectPopup(server -> serverFilter.apply(server) == null)
      .config(actualWebLogin)
      .showBrowser(window -> windowConsumer.accept(Pair.create(true, window)), login -> onConnect(update, login, username));
  }

  private void onConnect(Synchronized<List<Cookie>> update, WebLoginWindow login, String expectedUsername) {
    List<Cookie> p = CANCELLED;
    try {
      DetectedJiraServer server = login != null ? login.getDetectedServer() : null;
      if (server != null) {
        if (!Objects.equals(expectedUsername, server.getUsername())) log.error("Username has been changed. Ignoring cookies.");
        else p = server.getCookies();
      }
    } finally {
      update.set(p);
    }
  }

  private static class CantLoginForm {
    private JPanel myWholePanel;
    private JTextField myAccount;
    private URLLink myConnectionLink;
    private JTextArea myDescription;

    public CantLoginForm(ReadonlyConfiguration config) {
      setConnection(JiraConfiguration.getConnectionName(config), JiraConfiguration.getBaseUrl(config));
      myDescription.setText(I18N.getString("relogin.description.text"));
      String username = JiraConfiguration.getJiraUsername(config);
      myAccount.setText(username != null ? username : "<Anonymous>");
      Aqua.disableMnemonics(myWholePanel);
      UIUtil.setDefaultLabelAlignment(myWholePanel);
    }

    public static boolean askUser(DialogManager dialogManager, ReadonlyConfiguration config) {
      DialogBuilder builder = dialogManager.createBuilder("weblogin.relogin");
      DialogResult<Boolean> dr = new DialogResult<>(builder);
      DialogResult.configureYesNo(dr, "Restore", "Later");
      dr.pack();
      CantLoginForm form = new CantLoginForm(config);
      Boolean res = dr.showModal("Connection Expired", form.myWholePanel);
      return Boolean.TRUE.equals(res);
    }

    private void setConnection(String connectionName, String baseUrl) {
      myConnectionLink.setUrlText(connectionName);
      myConnectionLink.setUrl(baseUrl);
      myConnectionLink.setShowTooltip(true);
    }
  }

}
