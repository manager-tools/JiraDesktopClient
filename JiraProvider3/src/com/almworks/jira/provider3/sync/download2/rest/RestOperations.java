package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.ArrayKey;
import com.almworks.restconnector.operations.LoadUserInfo;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.List;
import java.util.TimeZone;

public class RestOperations {
  private static final String PATH_PROJECT = "api/2/project";
  public static List<JSONObject> projectsBrief(RestSession session) throws ConnectorException {
    try {
      return loadList(session, PATH_PROJECT, RequestPolicy.SAFE_TO_RETRY);
    } catch (ParseException e) {
      throw new JiraInternalException("cannot parse server reply");
    }
  }

  @Nullable("When failed to load project")
  public static JSONObject projectFull(RestSession session, String key) {
    if (key == null || key.isEmpty()) {
      LogHelper.error("Wrong project key", key);
      return null;
    }
    try {
      RestResponse response = session.restGet(PATH_PROJECT + "/" + key, RequestPolicy.SAFE_TO_RETRY);
      if (response.getStatusCode() == 404) return null;
      if (!response.isSuccessful()) return null;
      try {
        return response.getJSONObject();
      } catch (ParseException e) {
        LogHelper.warning("Failed to parse project");
        return null;
      }
    } catch (ConnectorException e) {
      LogHelper.warning(e);
      return null;
    }
  }

  private static final String PATH_PRIORITY = "api/2/priority";
  public static List<JSONObject> priorities(RestSession session) throws ConnectorException, ParseException {
    return loadList(session, PATH_PRIORITY, RequestPolicy.SAFE_TO_RETRY);
  }

  private static final String PATH_STATUS = "api/2/status";
  public static List<JSONObject> statuses(RestSession session) throws ConnectorException, ParseException {
    return loadList(session, PATH_STATUS, RequestPolicy.SAFE_TO_RETRY);
  }

  private static final String PATH_RESOLUTION = "api/2/resolution";
  public static List<JSONObject> resolutions(RestSession session) throws ConnectorException, ParseException {
    return loadList(session, PATH_RESOLUTION, RequestPolicy.SAFE_TO_RETRY);
  }

  private static List<JSONObject> loadList(RestSession session, String path, RequestPolicy policy) throws ConnectorException, ParseException {
    RestResponse response = session.restGet(path, policy);
    if (!response.isSuccessful()) return Collections15.emptyList();
    Object json = response.getJSON();
    return ArrayKey.ROOT_ARRAY.list(json);
  }

  @NotNull
  public static TimeZone getUserTimeZone(RestSession session) throws ConnectorException {
    LoadUserInfo userInfo = LoadUserInfo.loadMe(session);
    if (userInfo != null) {
      TimeZone timeZone = userInfo.getTimeZone();
      if (timeZone != null) return timeZone;
    }
    return RestServerInfo.get(session).getServerTimeZone();
  }

}
