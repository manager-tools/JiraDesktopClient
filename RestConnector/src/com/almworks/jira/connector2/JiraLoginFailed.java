package com.almworks.jira.connector2;

import com.almworks.util.i18n.text.LocalizedAccessor;

public class JiraLoginFailed extends JiraException {
  public static final LocalizedAccessor.Value SHORT = JiraEnv.I18N_LOCAL.getFactory("login.denied.short");
  public static final LocalizedAccessor.Value NETWORK_PROBLEM = JiraEnv.I18N_LOCAL.getFactory("login.problem.network.short");
  public static final LocalizedAccessor.Value PARSE_PROBLEM = JiraEnv.I18N_LOCAL.getFactory("login.problem.parse.short");
  private static final LocalizedAccessor.MessageStr LONG = JiraEnv.I18N_LOCAL.messageStr("login.denied.long");
  public static final LocalizedAccessor.Value SERVER_ERROR_SHORT = JiraEnv.I18N_LOCAL.getFactory("login.serverFailure.short");
  public static final LocalizedAccessor.MessageInt SERVER_ERROR_FULL = JiraEnv.I18N_LOCAL.messageInt("login.serverFailure.full");

  private final String myReason;

  public JiraLoginFailed(String reason) {
    super("jira login failed: " + reason, SHORT.create(), LONG.formatMessage(reason), JiraCause.ACCESS_DENIED);
    myReason = reason;
  }

  public String getReason() {
    return myReason;
  }
}
