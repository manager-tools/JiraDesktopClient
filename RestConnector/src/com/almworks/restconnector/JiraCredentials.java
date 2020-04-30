package com.almworks.restconnector;

import com.almworks.api.connector.ConnectorException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JiraCredentials {
  JiraCredentials ANONYMOUS = new JiraCredentials() {

    @Nullable("When anonymous")
    @Override
    public String getDisplayName() {
      return null;
    }

    @Override
    public String getAccountId() {
      return null;
    }

    @Override
    public boolean isAnonymous() {
      return true;
    }

    @Override
    public void initNewSession(RestSession session) {
    }

    @Override
    public void ensureLoggedIn(RestSession restSession, boolean fastCheck) {
    }

    @NotNull
    @Override
    public ResponseCheck checkResponse(RestSession session, RestSession.Job job, @NotNull RestResponse response) {
      return ResponseCheck.success("Anonymous");
    }

    @Override
    public JiraCredentials createUpdated(RestSession session) {
      return this;
    }
  };

  @Nullable("When anonymous")
  String getDisplayName();

  /**
   * @return AccountID of the user which is associated with this credentials
   * Returns null for anonymous credentials, when the AccountID is not known or not supported by the Jira server
   * @see com.almworks.restconnector.operations.LoadUserInfo#getAccountId()
   */
  String getAccountId();
  /**
   * @return true if expected anonymous JIRA connection.
   * @see #getAccountId()
   */
  boolean isAnonymous();

  /**
   * This method is called on every new session when it is just instantiated.
   * Implementation must ensure that session is properly authenticated
   * @param session new session
   */
  void initNewSession(RestSession session) throws ConnectorException;

  /**
   * Checks that the session is now logged in and if it seems to be not logged in do log in.<br>
   * This method can be used even in case authentication data is not known (single sing-on is used) and session creator does not know the actual username.<br>
   * This method is intended to be used by code that does work via {@link RestSession}
   * @param fastCheck if true the implementation MAY bypass server access, if it is sure about logged in state, to save network traffic.
   *                  if false implementation MUST accesses server to ensure.
   * @throws ConnectorException if network problem or failed to login
   */
  void ensureLoggedIn(RestSession restSession, boolean fastCheck) throws ConnectorException;

  /**
   * Check if response comes from JIRA server.<br>
   * SSO credentials may assume redirection to some other site or other signs that the session has expired.
   * @param session current session
   * @param job current job
   * @param response job's response
   * @return true if response seems comes from JIRA (even the session may expired)
   *         false if response is surely not from JIRA server
   */
  @NotNull
  ResponseCheck checkResponse(RestSession session, RestSession.Job job, @NotNull RestResponse response);

  /**
   * Request server for username
   * @param session to access the server
   * @return updated credentials of the same type
   */
  JiraCredentials createUpdated(RestSession session) throws ConnectorException;

  class ResponseCheck {
    private final String mySureFailed;
    private final String mySureSuccess;
    private final String myComment;

    public ResponseCheck(String sureFailed, String sureSuccess, String comment) {
      mySureFailed = sureFailed;
      mySureSuccess = sureSuccess;
      myComment = comment;
    }

    public static ResponseCheck fail(String message) {
      return new ResponseCheck(message, null, null);
    }

    public static ResponseCheck success(String message) {
      return new ResponseCheck(null, message, null);
    }

    public static ResponseCheck unsure(String comment) {
      return new ResponseCheck(null, null, comment);
    }

    public ResponseCheck assumeSuccess(String message) {
      return new ResponseCheck(null, message, myComment);
    }

    public boolean isFailed() {
      return mySureFailed != null;
    }

    public boolean isSuccess() {
      return mySureSuccess != null;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      if (mySureSuccess != null) builder.append("SUCCESS: ").append(mySureSuccess);
      else if (mySureFailed != null) builder.append("FAILED: ").append(mySureFailed);
      else builder.append("UNSURE.");
      if (myComment != null) builder.append(" ").append("Comment: ").append(myComment);
      return builder.toString();
    }
  }
}
