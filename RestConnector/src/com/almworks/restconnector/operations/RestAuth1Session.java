package com.almworks.restconnector.operations;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.ConnectionException;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LocalLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class RestAuth1Session {
  private static final LocalLog log = LocalLog.topLevel("rest.1.auth");
  public static final String AUTH_SESSION = "auth/1/session";
  private static final JSONKey<String> SESSION_NAME = JSONKey.text("name");

  /** No response was received */
  public static final int R_NO_RESPONSE = 0;
  /** Wrong format. Server responded something, but the response cannot be parsed. */
  public static final int R_WRONG = 1;
  /** Response was obtained and successfully parsed */
  public static final int R_OK = 2;

  /** Contains connection problem if request was failed to perform. */
  @Nullable
  private final ConnectorException myFailure;
  /** Result status, see R_* constants for explanation */
  private final int myResult;
  /** Logged in username or null if anonymous session. Applicable if only result status is {@link #R_OK}. */
  @Nullable
  private final String myUsername;

  private RestAuth1Session(@Nullable ConnectorException failure, int result, @Nullable String username) {
    myFailure = failure;
    myResult = result;
    myUsername = username;
  }

  public int getResult() {
    return myResult;
  }

  /**
   * @return true if proper response obtained. The session may be authenticated or anonymous
   */
  public boolean hasUsername() {
    return myResult == R_OK;
  }

  /**
   * Network failure. If this method returns null this means just some response is obtained, but it still can be invalid
   * and contain no session info.
   * @return network problem or null if no network problem occurred
   */
  @Nullable
  public ConnectorException getFailure() {
    return myFailure;
  }

  public ConnectorException getFailureOr(@NotNull ConnectorException notAuthenticated) {
    if (hasUsername()) log.error("Not failed");
    return myFailure != null ? myFailure : notAuthenticated;
  }

  /**
   * Applicable if {@link #hasUsername()} returns true (which is equal to {@link #getResult()} == {@link #R_OK})
   * @return null for anonymous session, not null session's account
   */
  @Nullable
  public String getUsername() {
    return myUsername;
  }

  public static RestAuth1Session get(RestSession session, RequestPolicy policy, boolean auxiliary) {
    RestResponse response;
    try {
      response = doGet(session, policy, auxiliary);
    } catch (ConnectorException e) {
      return new RestAuth1Session(e, R_NO_RESPONSE, null);
    }
    String contentType = response.getHttpResponse().getContentType();
    int statusCode = response.getStatusCode();
    if (!"application/json".equals(contentType)) {
      log.warning("Wrong response format from", contentType, statusCode, session.getBaseUrl());
      return new RestAuth1Session(null, R_WRONG, null);
    }
    JSONObject json;
    try {
      json = response.getJSONObject();
    } catch (ConnectionException e) {
      return new RestAuth1Session(e, R_NO_RESPONSE, null);
    } catch (ParseException e) {
      log.warning("Failed to parse response", statusCode, session.getBaseUrl());
      return new RestAuth1Session(null, R_WRONG, null);
    }
    String username = SESSION_NAME.getValue(json);
    if (statusCode / 100 == 2) {
      if (username == null) {
        log.warning("Successful reply missing username", statusCode, session.getBaseUrl());
        return new RestAuth1Session(null, R_WRONG, null);
      }
      return new RestAuth1Session(null, R_OK, username);
    }
    if (username != null) return new RestAuth1Session(null, R_OK, username);
    if (!json.containsKey("errorMessages") && !json.containsKey("errors")) {
      log.warning("Unexpected JSON", statusCode, session.getBaseUrl());
      return new RestAuth1Session(null, R_WRONG, null);
    }
    if (statusCode != 401) log.warning("Strange status code", statusCode, session.getBaseUrl());
    return new RestAuth1Session(null, R_OK, null);
  }

  private static RestResponse doGet(RestSession session, RequestPolicy policy, boolean auxiliary) throws ConnectorException {
    ConnectorException ex = null;
    for (int i = 0; i < 2; i++) {
      try {
        return doHttp(session, policy, auxiliary);
      } catch (ConnectorException e) {
        if (ex == null) ex = e;
        else ex.addSuppressed(e);
      }
    }
    throw ex;
  }

  @NotNull
  private static RestResponse doHttp(RestSession session, RequestPolicy policy, boolean auxiliary) throws ConnectorException {
    String url = session.getRestResourcePath(AUTH_SESSION);
    return session
            .perform(new RestSession.Job(RestSession.GetDelete.get(url, RestSession.getDebugName(AUTH_SESSION)), policy, auxiliary))
            .ensureHasResponse();
  }
}
