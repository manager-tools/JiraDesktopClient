package com.almworks.restconnector.login;

import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class JiraLoginInfo {
  public static final JiraLoginInfo ANONYMOUS = new JiraLoginInfo("", "", true, "", null);

  private final String myLogin;
  private final String myPassword;
  private final boolean myAnonymous;
  /** @see com.almworks.restconnector.operations.LoadUserInfo#getAccountId()  */
  @Nullable
  private final String myAccountId;
  /**
   * Display name of the current Jira user. We store this in a connection config in order to be able to initialize
   * the connection from config (when the DB has been dropped).
   */
  @Nullable
  private final String myDisplayName;

  public JiraLoginInfo(String login, String password, boolean anonymous, String accountId, String displayName) {
    myAccountId = accountId;
    if (login != null && login.isEmpty()) login = null;
    myLogin = login;
    myPassword = password;
    myAnonymous = anonymous;
    myDisplayName = displayName;
  }

  /**
   * @return login name to log into JIRA via JIRA login. Returns null for anonymous connections and in case user logs in via another facility (single sing-on)
   */
  @Nullable("When authentication is specified")
  public String getLogin() {
    return myLogin;
  }

  @Nullable("When authentication is specified")
  public String getPassword() {
    return myPassword;
  }

  public boolean isAnonymous() {
    return myAnonymous;
  }

  @Nullable
  public String getDisplayName() {
    return myDisplayName;
  }

  /**
   * @return not null Account ID for non-anonymous connections for new Jiras, those identify users by Account ID
   */
  @Nullable
  public String getAccountId() {
    return myAccountId;
  }
  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object obj) {
    JiraLoginInfo other = Util.castNullable(JiraLoginInfo.class, obj);
    if (other == null) return false;
    if (myAnonymous != other.myAnonymous) return false;
    if (myAnonymous) return true;
    return Util.equals(myLogin, other.myLogin) && Util.equals(myPassword, other.myPassword)
      && Util.equals(myAccountId, other.myAccountId) && Objects.equals(myDisplayName, other.myDisplayName);
  }

  @Override
  public int hashCode() {
    if (myAnonymous) return Util.hashCode(myAccountId);
    return Util.hashCode(myLogin, myPassword, myAccountId, myDisplayName);
  }

  @Override
  public String toString() {
    if (myAnonymous) return "<Anonymous>";
    return myLogin + (myPassword != null && !myPassword.isEmpty() ? "***" : "<noPassword>");
  }
}
