package com.almworks.restconnector.operations;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.http.HttpUtils;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.TimeZone;

public class LoadUserInfo {
  private static final String PATH_USER = "api/2/user";
  private static final JSONKey<TimeZone> USER_TIME_ZONE = JSONKey.timeZoneID("timeZone");
  private static final JSONKey<String> DISPLAY_NAME = JSONKey.text("displayName");

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

  public static LoadUserInfo loadMe(RestSession session) throws ConnectorException {
    String username;
    if (session.getCredentials().isAnonymous()) return null;
    else {
      username = session.getCredentials().getUsername();
      if (username.isEmpty()) {
        RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.NEEDS_LOGIN, false);
        if (auth.hasUsername()) username = auth.getUsername();
        else {
          LogHelper.warning("Failed to load current username");
          username = null;
        }
      }
    }
    return loadUser(session, username);
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
}
