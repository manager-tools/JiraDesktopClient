package com.almworks.restconnector.operations;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.CannotParseException;
import com.almworks.api.http.HttpUtils;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.TimeZone;

public class LoadUserInfo {
  private static final String PATH_USER = "api/2/user";
  private static final String MYSELF = "api/2/myself";
  private static final JSONKey<TimeZone> USER_TIME_ZONE = JSONKey.timeZoneID("timeZone");
  private static final JSONKey<String> DISPLAY_NAME = JSONKey.text("displayName");
  private static final JSONKey<String> ACCOUNT_ID = JSONKey.text("accountId");

  private final JSONObject myObject;

  public LoadUserInfo(JSONObject object) {
    myObject = object;
  }

  public TimeZone getTimeZone() {
    return USER_TIME_ZONE.getValue(myObject);
  }

  public String getDisplayName() {
    return DISPLAY_NAME.getValue(myObject);
  }

  public String getAccountId() {
    return ACCOUNT_ID.getValue(myObject);
  }

  public static LoadUserInfo loadMe(RestSession session) throws ConnectorException {
    if (session.getCredentials().isAnonymous()) return null;
    else {
      RequestPolicy policy = RequestPolicy.NEEDS_LOGIN;
      boolean auxiliary = false;
      return loadMe(session, policy, auxiliary);
    }
  }

  @NotNull
  public static LoadUserInfo loadMe(RestSession session, RequestPolicy policy, boolean auxiliary) throws ConnectorException {
    ConnectorException ex = null;
    RestResponse response = null;
    for (int i = 0; i < 2; i++) {
      try {
        String url = session.getRestResourcePath(MYSELF);
        response = session
          .perform(new RestSession.Job(RestSession.GetDelete.get(url, RestSession.getDebugName(MYSELF)), policy, auxiliary))
          .ensureHasResponse();
      } catch (ConnectorException e) {
        if (ex == null) ex = e;
        else ex.addSuppressed(e);
      }
    }
    if (ex != null) throw ex;
    assert response != null;
    String contentType = response.getHttpResponse().getContentType();
    int statusCode = response.getStatusCode();
    if (!"application/json".equals(contentType)) {
      LogHelper.warning("Wrong response format from", contentType, statusCode, session.getBaseUrl());
      String description = String.format("'application/json' content type was expected, but actual is '%s' status code: %s", contentType, statusCode);
      throw new ConnectorException("Wrong response content type", "Wrong response content type", description);
    }
    try {
      JSONObject json = response.getJSONObject();
      return new LoadUserInfo(json);
    } catch (ParseException e) {
      LogHelper.warning("Failed to parse response", statusCode, session.getBaseUrl());
      throw new CannotParseException(MYSELF, e.getMessage());
    }
  }

  public static LoadUserInfo loadUser(RestSession session, String username) throws ConnectorException {
    if (username == null) return null;
    StringBuilder url = new StringBuilder(PATH_USER);
    HttpUtils.addGetParameter(url, "username", username);
    RestResponse response = session.restGet(url.toString(), RequestPolicy.SAFE_TO_RETRY);
    if (!response.isSuccessful()) {
      LogHelper.warning("Failed to get user info");
      return null;
    }
    try {
      JSONObject userInfo = JSONKey.ROOT_OBJECT.getValue(response.getJSON());
      return new LoadUserInfo(userInfo);
    } catch (ParseException e) {
      LogHelper.warning("Failed to load user timeZone");
      LogHelper.debug(e);
      return null;
    }
  }

  @Override
  public String toString() {
    return "LoadUserInfo{" +
      "myObject=" + myObject +
      '}';
  }
}
