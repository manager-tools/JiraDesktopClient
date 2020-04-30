package com.almworks.restconnector.operations;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.connector2.JiraServerVersionInfo;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.TimeZone;

public class RestServerInfo {
  private static final JSONKey<String> VERSION_TEXT = JSONKey.textNNTrim("version");
  private static final JSONKey<String> TITLE = JSONKey.textNNTrim("serverTitle");
  private static final JSONKey<TimeZone> USER_TIMEZONE = JSONKey.dateTimeZone("serverTime");
  private static final JSONKey<TimeZone> BUILD_DATE_TIMEZONE = JSONKey.dateTimeZone("buildDate");

  private final String myServerTitle;
  private final JiraServerVersionInfo myVersion;
  private final TimeZone myServerTimeZone;

  private RestServerInfo(String serverTitle, JiraServerVersionInfo version, TimeZone serverTimeZone) {
    myServerTitle = serverTitle;
    myVersion = version;
    myServerTimeZone = serverTimeZone;
  }

  public String getServerTitle() {
    return myServerTitle;
  }

  public String getVersionText() {
    return myVersion.getVersion();
  }

  @NotNull
  public JiraServerVersionInfo getVersion() {
    return myVersion;
  }

  @NotNull
  public static RestServerInfo get(RestSession session) throws ConnectorException {
    return LoadServerInfo.DEFAULT.get(session);
  }

  public static boolean isJiraVersionOrLater(RestSession session, String version) throws ConnectorException {
    RestServerInfo info;
    info = get(session);
    Boolean later = info.getVersion().isVersionOrLater(version);
    if (later != null) return later;
    else {
      LogHelper.error("Wrong version", version, info.getVersion().getVersion());
      return false;
    }
  }

  public TimeZone getServerTimeZone() {
    return myServerTimeZone;
  }

  @NotNull
  public static RestServerInfo fromJSON(@NotNull JSONObject rawInfo) throws ParseException {
    String serverTitle = Util.NN(TITLE.getValue(rawInfo));
    String versionText = VERSION_TEXT.getValue(rawInfo);
    if (versionText == null) {
      LogHelper.error("Failed to get server version text", rawInfo);
      throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION);
    }
    JiraServerVersionInfo version = JiraServerVersionInfo.create(versionText);
    TimeZone timeZone = USER_TIMEZONE.getValue(rawInfo);
    if (timeZone == null) {
      LogHelper.debug("No timeZone in server info. Assuming anonymous connection.");
      timeZone = BUILD_DATE_TIMEZONE.getValue(rawInfo); // Extract timeZone from build date (with server-default time shift)
    }
    return new RestServerInfo(serverTitle, version, timeZone);
  }
}
