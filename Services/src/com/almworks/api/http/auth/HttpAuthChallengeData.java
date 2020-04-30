package com.almworks.api.http.auth;

public class HttpAuthChallengeData {
  private final String myAuthScheme;
  private final String myRealm;
  private final String myHost;
  private final int myPort;
  private final int myStatusCode;
  private final String myStatusText;
  private final boolean myProxy;

  public HttpAuthChallengeData(String authScheme, String realm, String host, int port, int statusCode,
    String statusText, boolean proxy) {

    myAuthScheme = authScheme;
    myHost = host;
    myPort = port;
    myRealm = realm;
    myStatusCode = statusCode;
    myStatusText = statusText;
    myProxy = proxy;
  }

  public boolean isProxy() {
    return myProxy;
  }

  public String getAuthScheme() {
    return myAuthScheme;
  }

  public String getHost() {
    return myHost;
  }

  public int getPort() {
    return myPort;
  }

  public String getRealm() {
    return myRealm;
  }

  public int getStatusCode() {
    return myStatusCode;
  }

  public String getStatusText() {
    return myStatusText;
  }

  public String toString() {
    return myAuthScheme + ":" + myHost + ":" + myPort + ":" + myRealm + ":" + myStatusCode + ":" + myStatusText + ":" + myProxy;
  }
}
