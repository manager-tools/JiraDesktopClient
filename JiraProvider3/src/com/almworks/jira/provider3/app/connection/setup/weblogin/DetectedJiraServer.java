package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.jira.provider3.app.connection.setup.JiraBaseUri;
import com.almworks.jira.provider3.app.connection.setup.ServerConfig;
import com.almworks.restconnector.CookieJiraCredentials;
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
  private final String myUsername;
  private final String myDisplayableUsername;

  public DetectedJiraServer(@NotNull JiraBaseUri baseUri, List<Cookie> cookies, @NotNull RestServerInfo serverInfo, String username, String displayableUsername) {
    myBaseUri = baseUri;
    myCookies = cookies;
    myServerInfo = serverInfo;
    myUsername = username;
    myDisplayableUsername = displayableUsername;
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
  public String getUsername() {
    return myUsername;
  }

  @Nullable("When anonymous")
  public String getDisplayableUsername() {
    if (myDisplayableUsername != null) return myDisplayableUsername;
    return myUsername;
  }

  @NotNull
  public ServerConfig toServerConfig(boolean ignoreProxy) {
    CookieJiraCredentials credentials = CookieJiraCredentials.connected(myUsername, myCookies, null, null, null);
    return new ServerConfig(myBaseUri.getBaseUri().toString(), credentials, ignoreProxy, true);
  }

  @Override
  public String toString() {
    return String.format("ServerInfo[URL=%s, Name=%s, user=%s, account=%s]", myBaseUri, myServerInfo.getServerTitle(), getDisplayableUsername(), myUsername);
  }
}
