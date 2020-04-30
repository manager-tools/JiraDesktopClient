package com.almworks.jira.connector2;

public class JiraInternalException extends JiraException {
  public JiraInternalException(String shortReason, String longReason) {
    super(longReason, "Compatibility Error: " + shortReason,
      "There was a compatibility error when talking to Jira: " + longReason + "." +
      "Please contact support.", JiraCause.COMPATIBILITY);
  }

  public JiraInternalException(String shortReason) {
    super(shortReason, "Compatibility Error: " + shortReason,
      "There was a compatibility error when talking to Jira (" + shortReason + "). Please contact support.", JiraCause.COMPATIBILITY);
  }
}
