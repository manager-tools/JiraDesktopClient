package com.almworks.restconnector.login;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.connector2.JiraCredentialsRequiredException;
import com.almworks.jira.connector2.JiraException;
import com.almworks.restconnector.*;
import com.almworks.restconnector.operations.RestAuth1Session;
import com.almworks.util.LogHelper;
import org.almworks.util.Const;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * This implementation logs directly into JIRA with login and password.
 */
public class LoginJiraCredentials implements JiraCredentials {
  private static final TypedKey<LoginJiraCredentials> KEY = TypedKey.create(LoginJiraCredentials.class);

  private final CheckRecentLogin myCheckRecentLogin = new CheckRecentLogin();
  private final String myLogin;
  private final String myPassword;
  @Nullable
  private final LoginController myLoginController;
  /**
   * null - anonymous<br>
   * "" (empty string) - not anonymous but not known yet
   * not-empty string - known expected username
   */
  private final String myUsername;

  private LoginJiraCredentials(String login, String password, @Nullable LoginController loginController, String username) {
    if (login != null && login.isEmpty()) login = null;
    if (password != null && password.isEmpty()) password = null;
    if (login == null || password == null) {
      login = null;
      password = null;
    }
    myLogin = login;
    myPassword = password;
    myLoginController = loginController;
    myUsername = username;
  }

  public static LoginJiraCredentials authenticated(String login, String password, String username, @Nullable LoginController loginController) {
    username = Util.NN(username);
    return new LoginJiraCredentials(login, password, loginController, username);
  }

  public static LoginJiraCredentials anonymous(@Nullable LoginController loginController) {
    return new LoginJiraCredentials(null, null, loginController, null);
  }

  @Override
  public String toString() {
    return String.format("Login(login='%s', username='%s')", myLogin, myUsername);
  }

  @Override
  @NotNull
  public String getUsername() {
    return Util.NN(myUsername);
  }

  @Override
  public boolean isAnonymous() {
    return myUsername == null;
  }

  public String getLogin() {
    return myLogin;
  }

  public String getPassword() {
    return myPassword;
  }

  @Override
  public void initNewSession(RestSession session) throws ConnectorException {
    session.getUserData().putUserData(KEY, this);
    ensureLoggedIn(session, false);
  }

  @Override
  public void ensureLoggedIn(RestSession session, boolean fastCheck) throws ConnectorException {
    if (fastCheck && checkLoggedIn(session)) return;
    RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.FAILURE_ONLY, true);
    if (auth.getFailure() != null) throw auth.getFailure();
    if (isAnonymous()) {
      if (!auth.hasUsername())
        throw new JiraException("Cannot access Jira", "Cannot access Jira", "Cannot access Jira server.\n" +
                "May be you need to re-configure your connection with web browser.\n" +
                "Please, edit connection and try connect with web browser.", JiraException.JiraCause.ACCESS_DENIED);
      myCheckRecentLogin.afterLogin(session);
      return;
    }
    String username = auth.hasUsername() ? auth.getUsername() : null;
    if (username == null) forceLogin(session);
    else myCheckRecentLogin.afterLogin(session);
  }

  /**
   * Performs login. May request user to fix password and replace session credentials.
   */
  public void forceLogin(RestSession session) throws ConnectorException {
    if (myLogin == null || myPassword == null) throw new JiraCredentialsRequiredException();
    String invalidLogin = myLoginController != null ? myLoginController.getInvalidLoginMessage(myLogin) : null;
    ConnectorException loginException;
    if (invalidLogin == null) {
      LoginFacility.Result failure = doLogin(session);
      if (failure == null) return; // Successful login
      invalidLogin = failure.getFailure();
      LogHelper.debug("Login state is invalid. Login not performed.", invalidLogin);
      loginException = failure.getLoginException();
    } else loginException = LoginFacility.loginRequired(invalidLogin);
    if (myLoginController == null) throw loginException;
    JiraCredentials newCredentials = null;
    try {
      newCredentials = myLoginController.loginInvalid(this, invalidLogin);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    if (newCredentials == null || newCredentials.isAnonymous()) throw loginException; // no new login provided
    if (sameCredentials(newCredentials)) throw loginException; // Same credentials
    LogHelper.warning("Updating credentials", newCredentials);
    session.updateCredentials(newCredentials);
  }

  private LoginFacility.Result doLogin(RestSession session) throws ConnectorException {
    myCheckRecentLogin.resetLogin(session);
    long loginTime = System.currentTimeMillis();
    LoginFacility.Result loginResult = LoginFacility.GENERIC.login(session, myLogin, myPassword);
    String username = loginResult.getUsername();
    if (username != null && !username.isEmpty()) {
      List<Cookie> cookies = JiraCookies.collectSessionCookies(session.getCookies());
      if (!cookies.isEmpty()) session.setSessionCookies(cookies);
      else LogHelper.error("Missing session cookie after successful login", session.getBaseUrl(), LoginFacility.GENERIC);
      if (username.equals(myUsername)) myCheckRecentLogin.afterLogin(session);
      else {
        LoginJiraCredentials newCredentials = authenticated(myLogin, myPassword, username, myLoginController);
        session.updateCredentials(newCredentials);
        newCredentials.myCheckRecentLogin.afterLogin(session);
      }
      return null;
    } else return loginResult;
  }

  /**
   * @return true if session state is know, false if it is unknown or uncertain
   */
  private boolean checkLoggedIn(RestSession session) {
    if (myUsername == null) return myCheckRecentLogin.isJustLoggedIn(session);
    return !myUsername.isEmpty() && myCheckRecentLogin.isJustLoggedIn(session);
  }

  @NotNull
  @Override
  public ResponseCheck checkResponse(RestSession session, RestSession.Job job, @NotNull RestResponse response) {
    return ResponseCheck.success("Login-based"); // Direct JIRA server communication is assumed, no proxies in the middle is expected.
  }

  @Override
  public LoginJiraCredentials createUpdated(RestSession session) throws ConnectorException {
    if (isAnonymous()) return this;
    RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.FAILURE_ONLY, true);
    String username = auth.getUsername();
    if (!auth.hasUsername() || username == null) throw auth.getFailureOr(new JiraCredentialsRequiredException());
    return new LoginJiraCredentials(myLogin, myPassword, myLoginController, username);
  }

  public boolean sameCredentials(JiraCredentials other) {
    if (this == other) return true;
    LoginJiraCredentials login = Util.castNullable(LoginJiraCredentials.class, other);
    if (login == null) return false;
    if (isAnonymous()) return login.isAnonymous();
    //noinspection SimplifiableIfStatement
    if (login.isAnonymous()) return false;
    return Objects.equals(myLogin, login.getLogin()) && Objects.equals(myPassword, login.getPassword());
  }

  public JiraLoginInfo toLoginInfo() {
    return isAnonymous() ? JiraLoginInfo.ANONYMOUS : new JiraLoginInfo(myLogin, myPassword, false, myUsername);
  }

  public static LoginJiraCredentials fromLoginInfo(JiraLoginInfo loginInfo, LoginController loginController) {
    if (loginInfo.isAnonymous()) return anonymous(loginController);
    return authenticated(loginInfo.getLogin(), loginInfo.getPassword(), loginInfo.getJiraUsername(), loginController);
  }

  /**
   * This convenient class tracks login state checks to reduce number of expensive requests.
   */
  public static class CheckRecentLogin {
    private static final long CHECK_TIME = 4* Const.MINUTE;
    private final TypedKey<Long> myLastCheckKey = TypedKey.create("lastCheckTime");

    /**
     * A client should notify when login state has been check and confirmed.
     */
    public void afterLogin(RestSession session) {
      session.getUserData().putUserData(myLastCheckKey, System.currentTimeMillis());
    }

    /**
     * A client notifies that current login state is unknown
     */
    public void resetLogin(RestSession session) {
      session.getUserData().putUserData(myLastCheckKey, null);
    }

    /**
     * @return true if authentication resources were recently successfully accessed (session auth state logged in or anonymous) was confirmed.
     */
    public boolean isJustLoggedIn(RestSession session) {
      Long time = session.getUserData().getUserData(myLastCheckKey);
      long now = System.currentTimeMillis();
      return time != null && now - time < CHECK_TIME;
    }
  }
}
