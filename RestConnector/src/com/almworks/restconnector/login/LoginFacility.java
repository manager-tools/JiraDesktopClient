package com.almworks.restconnector.login;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.ConnectionException;
import com.almworks.api.connector.http.HttpFailureConnectionException;
import com.almworks.jira.connector2.*;
import com.almworks.jira.connector2.html.RestLoginPrivacyPolizei;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.ArrayKey;
import com.almworks.restconnector.operations.RestAuth1Session;
import com.almworks.util.LogHelper;
import com.almworks.util.text.TextUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.List;

class LoginFacility {
  @NotNull
  static JiraException loginRequired(String message) {
    return message != null ? new JiraLoginFailed(message) : new JiraCredentialsRequiredException();
  }

  private final ArrayKey<String> LOGIN_ERRORS = ArrayKey.textArray("errorMessages");

  public Result login(RestSession session, String login, String password) throws ConnectorException {
    final JSONObject request = new JSONObject();
    request.put("username", login);
    request.put("password", password);
    final String url = session.getRestResourcePath(RestAuth1Session.AUTH_SESSION);
    RestSession.Request postLogin = RestSession.PutPost.postJSON(url, "login", RestLoginPrivacyPolizei.INSTANCE, request.toJSONString());
    RestResponse response = session.perform(RestSession.Job.auxiliary(postLogin)).ensureHasResponse();
    int code = response.getStatusCode();
    if (code / 100 == 2) {
      response.dumpResponse();
      RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.FAILURE_ONLY, true);
      if (auth.getFailure() != null) throw auth.getFailure();
      if (!auth.hasUsername())
        throw new JiraException("Failed to confirm login", "Failed to log into Jira",
                "Client for Jira cannot log into your Jira.\n" +
                        "Please, edit connection and try to connect with web browser.", JiraException.JiraCause.ACCESS_DENIED);
      String username = auth.getUsername();
      if (username == null) throw new JiraInternalException("Failed to confirm login");
      LogHelper.assertError(code == 200, code);
      return Result.success(username);
    }
    String failure;
    ConnectorException exception;
    boolean invalid;
    if (code / 100 == 5) {
      invalid = false;
      LogHelper.warning("Login failed die to server error", code);
      failure = JiraLoginFailed.SERVER_ERROR_SHORT.create();
      exception = new JiraException(failure, failure, JiraLoginFailed.SERVER_ERROR_FULL.formatMessage(code), JiraException.JiraCause.COMPATIBILITY);
    } else if (code == 403) { // CAPTCHA
      failure = JiraCaptchaRequired.SHORT.create();
      exception = new JiraCaptchaRequired();
      invalid = true; // Credentials may be right
    } else if (code == 401) { // Wrong credentials
      invalid = true;
      failure = extractLoginFailure(response, session, login, password);
      exception = loginRequired(failure);
    } else {
      LogHelper.assertError(code == 404, code); // server may return 404 if the server is not JIRA
      invalid = false;
      exception = new HttpFailureConnectionException(url, code, response.getStatusText());
      failure = JiraLoginFailed.SERVER_ERROR_SHORT.create();
    }
    return Result.failure(exception, failure, invalid);
  }

  @NotNull
  private String extractLoginFailure(RestResponse response, RestSession session, String login, String password) {
    Object loginMessage;
    try {
      loginMessage = response.getJSON();
    } catch (ConnectionException e) {
      LogHelper.warning("Failed to login", e.getMessage());
      LogHelper.debug(e);
      return JiraLoginFailed.NETWORK_PROBLEM.create();
    } catch (ParseException e) {
      LogHelper.warning("Cannot understand login", e.getMessage());
      LogHelper.debug(e);
      return JiraLoginFailed.PARSE_PROBLEM.create();
    }
    String message = TextUtil.separate(LOGIN_ERRORS.list(loginMessage), "\n").trim();
    if (message.length() == 0) {
      String username = session.getCredentials().getUsername();
      String maskedPassword = password != null && !password.trim().isEmpty() ? "***" : "<nullOrEmpty>";
      LogHelper.error("Missing login error", response.getStatusCode(), response.getLoadedString(), response.getLastUrl(), login, username, maskedPassword);
      return JiraLoginFailed.SHORT.create();
    }
    return message;
  }

  public static final LoginFacility GENERIC = new LoginFacility();


  @Nullable("Null mean empty collection")
  public List<String> getAdditionalCookies() {
    return null;
  }

  /**
   * Result of login attempt.
   * It either is successful and has not-null {@link #myUsername username} or failed and has not-null
   * {@link #getLoginException() loginException} and {@link #getFailure() failure message}.
   */
  public static class Result {
    private final ConnectorException myException;
    private final String myFailure;
    private final boolean myInvalid;
    /** Username of successfully logged in user */
    private final String myUsername;

    public Result(String username, ConnectorException exception, String failure, boolean invalid) {
      myUsername = username;
      myException = exception;
      myFailure = failure;
      myInvalid = invalid;
    }

    /**
     * @return not-null username of logged-in user.
     *         null means login failed
     */
    @Nullable
    public String getUsername() {
      return myUsername;
    }

    @Nullable("When login is successful")
    public ConnectorException getLoginException() {
      return myException;
    }

    /**
     * @return true if login facility surely detected that login is invalid. Client code should block subsequent attempts to log in with these credentials.
     */
    public boolean isLoginInvalid() {
      return myInvalid;
    }

    @NotNull
    public String getFailure() {
      return myFailure;
    }

    public static Result success(String username) throws JiraInternalException {
      if (username == null || username.isEmpty()) throw new JiraInternalException("failed to check login");
      return new Result(username, null, "", false);
    }

    public static Result failure(@NotNull ConnectorException exception, String failure, boolean invalid) throws JiraInternalException {
      //noinspection ConstantConditions
      if (exception == null) throw new JiraInternalException("failed to check login");
      if (failure == null) {
        LogHelper.error("Missing login failure", exception, invalid);
        failure = exception.getShortDescription();
      }
      return new Result(null, exception, failure, invalid);
    }
  }
}
