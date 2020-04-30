package com.almworks.jira.provider3.app.connection.setup;

import com.almworks.restconnector.JiraCredentials;

public class ServerConfig {
  private final String myRawBaseUrl;
  private final JiraCredentials myCredentials;
  private final boolean myIgnoreProxy;
  /**
   * The true value of this flag means that the {@link #myRawBaseUrl base URL} has been checked and is correct.
   * No further validation is required.
   * The false value means that the URL comes from a user, and may need correction.
   */
  private final boolean mySureBaseUrl;

  public ServerConfig(String rawBaseUrl, JiraCredentials credentials, boolean ignoreProxy, boolean sureBaseUrl) {
    myRawBaseUrl = rawBaseUrl;
    myCredentials = credentials;
    myIgnoreProxy = ignoreProxy;
    mySureBaseUrl = sureBaseUrl;
  }

  public boolean isSureBaseUrl() {
    return mySureBaseUrl;
  }

  public JiraCredentials getCredentials() {
    return myCredentials;
  }

  public String getRawBaseUrl() {
    return myRawBaseUrl;
  }

  public boolean isIgnoreProxy() {
    return myIgnoreProxy;
  }
}
