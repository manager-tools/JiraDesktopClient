package com.almworks.jira.connector2;


public class JiraCredentialsRequiredException extends JiraException {
  public JiraCredentialsRequiredException(Throwable cause, String requestMessage) {
    super("jira credentials required" + (requestMessage == null ? "" : " (" + requestMessage + ")"),
            cause,
            requestMessage == null ? "Jira Login Required" : "Jira Error or Login Required",
            (requestMessage == null ? "Cannot log into Jira" : "Jira reported the following error: \n" + requestMessage)
                    + " \nPlease, edit the connection and check your credentials",
      JiraCause.ACCESS_DENIED);
  }

  public JiraCredentialsRequiredException() {
    this(null, null);
  }
}
