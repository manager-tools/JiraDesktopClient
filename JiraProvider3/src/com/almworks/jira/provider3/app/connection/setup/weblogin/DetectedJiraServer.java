package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.jira.provider3.app.connection.setup.JiraBaseUri;
import com.almworks.jira.provider3.app.connection.setup.ServerConfig;
import com.almworks.restconnector.CookieJiraCredentials;
import com.almworks.restconnector.operations.LoadUserInfo;
import com.almworks.restconnector.operations.RestServerInfo;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DetectedJiraServer {
  @NotNull
  private final JiraBaseUri myBaseUri;
  private final List<Cookie> myCookies;
  @NotNull
  private final RestServerInfo myServerInfo;
  private final LoadUserInfo myMyself;

  public DetectedJiraServer(@NotNull JiraBaseUri baseUri, List<Cookie> cookies, @NotNull RestServerInfo serverInfo, LoadUserInfo myself) {
    myBaseUri = baseUri;
    myCookies = cookies;
    myServerInfo = serverInfo;
    myMyself = myself;
  }

  public List<Cookie> getCookies() {
    return myCookies;
  }

  @NotNull
  public JiraBaseUri getBaseUri() {
    return myBaseUri;
  }

  @NotNull
  public RestServerInfo getServerInfo() {
    return myServerInfo;
  }

  @Nullable("When anonymous")
  public LoadUserInfo getMyself() {
    return myMyself;
  }

  @Nullable("When anonymous")
  public String getAccountId() {
    return myMyself != null ? myMyself.getAccountId() : null;
  }

  @NotNull
  public ServerConfig toServerConfig(boolean ignoreProxy) {
    CookieJiraCredentials credentials;
    if (myMyself != null)
      credentials = CookieJiraCredentials.connected(myMyself.getAccountId(), myCookies, null, null, null, myMyself.getDisplayName());
    else credentials = CookieJiraCredentials.connected(null,myCookies, null, null, null, null);
    return new ServerConfig(myBaseUri.getBaseUri().toString(), credentials, ignoreProxy, true);
  }

  @Override
  public String toString() {
    return String.format("ServerInfo[URL=%s, Name=%s, user=%s]", myBaseUri, myServerInfo.getServerTitle(), myMyself);
  }
}
