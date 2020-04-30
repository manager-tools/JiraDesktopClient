package com.almworks.restconnector.login;

import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JiraLoginInfo {
  public static final JiraLoginInfo ANONYMOUS = new JiraLoginInfo("", "", true, "");

  private final String myLogin;
  private final String myPassword;
  private final boolean myAnonymous;
  private final String myJiraUsername;

  public JiraLoginInfo(String login, String password, boolean anonymous, @NotNull String jiraUsername) {
    if (login != null && login.isEmpty()) login = null;
    myLogin = login;
    myPassword = password;
    myAnonymous = anonymous;
    myJiraUsername = jiraUsername;
  }

  /**
   * @return login name to log into JIRA via JIRA login. Returns null for anonymous connections and in case user logs in via another facility (single sing-on)
   * @see #getJiraUsername()
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

  /**
   * Logged in JIRA user name. It may be not null while {@link #getLogin() login} is null. This happens if user is authenticated not via JIRA
   * login but some kind of sing sigh-on facility.
   * @return not empty logged in username or empty if connection is anonymous (user does not log in in any way)
   */
  @NotNull
  public String getJiraUsername() {
    return myJiraUsername;
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object obj) {
    JiraLoginInfo other = Util.castNullable(JiraLoginInfo.class, obj);
    if (other == null) return false;
    if (myAnonymous != other.myAnonymous) return false;
    if (!Util.equals(myJiraUsername, other.myJiraUsername)) return false;
    if (myAnonymous) return true;
    return Util.equals(myLogin, other.myLogin) && Util.equals(myPassword, other.myPassword);
  }

  @Override
  public int hashCode() {
    if (myAnonymous) return Util.hashCode(myJiraUsername);
    return Util.hashCode(myLogin, myPassword, myJiraUsername);
  }

  @Override
  public String toString() {
    if (myAnonymous) return "<Anonymous>";
    return myLogin + " (" + myJiraUsername + ") " + (myPassword != null && !myPassword.isEmpty() ? "***" : "<noPassword>");
  }
}
