package com.almworks.restconnector.operations;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.UserDataHolder;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class LoadServerInfo {
  public static final LoadServerInfo DEFAULT = new LoadServerInfo(RequestPolicy.SAFE_TO_RETRY);

  public static final String PATH = "api/2/serverInfo";

  private static final TypedKey<RestServerInfo> INSTANCE_KEY = TypedKey.create("serverInfo");
  private final RequestPolicy myRequestPolicy;

  public LoadServerInfo(RequestPolicy requestPolicy) {
    myRequestPolicy = requestPolicy;
  }

  @NotNull
  public RestServerInfo get(RestSession session) throws ConnectorException {
    RestServerInfo info = getLoaded(session);
    if (info != null) return info;
    info = loadServerInfo(session);
    session.getUserData().putUserData(INSTANCE_KEY, info);
    session.getSessionData().putUserData(INSTANCE_KEY, info);
    return info;
  }

  private RestServerInfo getLoaded(RestSession session) {
    UserDataHolder data = session.getUserData();
    RestServerInfo info = data.getUserData(INSTANCE_KEY);
    if (info == null) info = session.getSessionData().getUserData(INSTANCE_KEY);
    return info;
  }

  @NotNull
  private RestServerInfo loadServerInfo(RestSession session) throws ConnectorException {
    RestResponse response = session.restGet(PATH, myRequestPolicy);
    return fromResponse(response);
  }

  @NotNull
  public static RestServerInfo fromResponse(RestResponse response) throws ConnectorException {
    response.ensureSuccessful();
    JSONObject rawInfo;
    try {
      rawInfo = response.getJSONObject();
      return RestServerInfo.fromJSON(rawInfo);
    } catch (ParseException e) {
      LogHelper.warning("Failed to parse server info");
      LogHelper.debug(e);
      throw new JiraInternalException("Failed to load server info. Cannot understand server reply.");
    }
  }
}
