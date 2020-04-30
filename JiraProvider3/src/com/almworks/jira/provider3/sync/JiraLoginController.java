package com.almworks.jira.provider3.sync;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.GlobalLoginController;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.http.HttpMaterial;
import com.almworks.http.errors.SSLProblemHandler;
import com.almworks.jira.connector2.JiraCredentialsRequiredException;
import com.almworks.jira.provider3.app.connection.ConnectionDescriptor;
import com.almworks.jira.provider3.app.connection.JiraConfigHolder;
import com.almworks.jira.provider3.app.connection.JiraConfiguration;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.restconnector.JiraCredentials;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.login.JiraLoginInfo;
import com.almworks.restconnector.login.LoginController;
import com.almworks.restconnector.login.LoginJiraCredentials;
import com.almworks.restconnector.operations.RestAuth1Session;
import com.almworks.util.LogHelper;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.io.IOUtils;
import com.almworks.util.threads.Computable;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JiraLoginController implements LoginController {
  private static final LocalizedAccessor.Value M_INVALID_LOGIN = ConnectorManager.LOCAL.getFactory("login.invalid");
  private final JiraConnection3 myConnection;

  public JiraLoginController(JiraConnection3 connection) {
    myConnection = connection;
  }

  @Override
  public String getInvalidLoginMessage(String login) {
    return getInvalidLoginMessage(getGlobalLoginController(), getSite());
  }

  private String getSite() {
    return getLoginToken(myConnection);
  }

  private GlobalLoginController getGlobalLoginController() {
    return myConnection.getActor(GlobalLoginController.ROLE);
  }

  public static String getInvalidLoginMessage(GlobalLoginController globalLogin, String site) {
    if (!globalLogin.isLoginFailed(site)) return null;
    return M_INVALID_LOGIN.create();
  }

  @Override
  public JiraCredentials loginInvalid(@NotNull LoginJiraCredentials credentials, final String message) throws InterruptedException {
    return getGlobalLoginController().updateLogin(myConnection, new Computable<JiraCredentials>() {
      @Override
      public JiraCredentials compute() {
        final JiraConfigHolder config = myConnection.getConfigHolder();
        if (!config.shouldAskReLogin() && getGlobalLoginController().isLoginFailed(getSite())) return null; // User has already approved login failure
        JiraCredentials current = config.getJiraCredentials();
        if (current == null || !credentials.sameCredentials(current)) {
          return current;
        }
        final StringBuffer login = new StringBuffer(Util.NN(credentials.getLogin()));
        final StringBuffer password = new StringBuffer(Util.NN(credentials.getPassword()));
        final boolean[] anonymous = {!current.isAnonymous()};
        boolean changed = ThreadGate.AWT_IMMEDIATE.compute(() -> {
          DialogManager dialogManager = myConnection.getActor(DialogManager.ROLE);
          Engine engine = myConnection.getActor(Engine.ROLE);
          String connectionName = engine.getConnectionManager().getConnectionName(myConnection.getConnectionID());
          return AskCredentialsDialog.show(dialogManager, login, password, anonymous, connectionName, config.getBaseUrl(), message);
        });
        if (!changed) {
          getGlobalLoginController().setFailureFlag(getSite());
          return null;
        }
        JiraCredentials update = updateCredentials(login, password, anonymous[0]);
        if (update != null) getGlobalLoginController().clearFailureFlag(getSite());
        return update;
      }

      private JiraCredentials updateCredentials(StringBuffer loginBuf, StringBuffer passwordBuf, boolean anonymous) {
        String login = anonymous ? null : loginBuf.toString();
        String password = anonymous ? null : passwordBuf.toString();
        RestSession session = null;
        ConnectionDescriptor descriptor = myConnection.getConfigHolder().getConnectionDescriptor();
        if (descriptor != null) {
          String baseUrl = descriptor.getBaseUrl();
          if (baseUrl != null) {
            HttpMaterial material = descriptor.createHttpMaterial();
            session = RestSession.create(baseUrl, LoginJiraCredentials.authenticated(login, password, null, null), material, null, myConnection.getActor(SSLProblemHandler.ROLE).getSNIErrorHandler());
          }
        }
        if (session == null) {
          LogHelper.warning("Credentials not checked due to connection is not ready");
          AskCredentialsDialog.showWrongCredentials(new ConnectorException("Connection not ready", "Connection not ready", "Connection not ready"));
          return null;
        }
        JiraLoginInfo userInfo;
        RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.SAFE_TO_RETRY, false);
        if (!auth.hasUsername()) {
          LogHelper.warning("Failed to load username", auth.getResult());
          AskCredentialsDialog.showWrongCredentials(auth.getFailureOr(new JiraCredentialsRequiredException()));
          return null;
        }
        String username = auth.getUsername();
        userInfo = username != null ? new JiraLoginInfo(login, password, anonymous, username) : JiraLoginInfo.ANONYMOUS;
        Engine engine = myConnection.getActor(Engine.ROLE);
        Configuration newConfig = ConfigurationUtil.copy(myConnection.getConfiguration());
        JiraConfiguration.setLoginPassword(newConfig, userInfo);
        engine.getConnectionManager().updateConnection(myConnection, newConfig);
        LogHelper.warning("Credentials updated for", session.getBaseUrl(), login);
        return myConnection.getConfigHolder().getConnectionDescriptor().getCredentials();
      }
    });
  }

  public static String getLoginToken(JiraConnection3 connection) {
    JiraConfigHolder config = connection.getConfigHolder();
    LoginJiraCredentials credentials = Util.castNullable(LoginJiraCredentials.class, config.getJiraCredentials());
    return getLoginToken(config.getBaseUrl(), credentials);
  }

  private static String getLoginToken(String baseUrl, @Nullable LoginJiraCredentials credentials) {
    if (credentials != null && credentials.isAnonymous()) credentials = null;
    String login = credentials != null ? credentials.getLogin() : null;
    String password = credentials != null ? credentials.getPassword() : null;
    return getLoginToken(baseUrl, login, password);
  }

  public static String getLoginToken(String baseUrl, String login, String password) {
    baseUrl = getSite(baseUrl);
    login = Util.NN(login);
    password = Util.NN(password);
    try {
      return baseUrl + IOUtils.md5sum(login + password);
    } catch (Exception e) {
      LogHelper.error(e);
      return baseUrl;
    }
  }

  public static String getSite(String baseUrl) {
    baseUrl = Util.lower(baseUrl);
    return baseUrl;
  }
}
