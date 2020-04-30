package com.almworks.util.http;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.CookieSpecBase;
import org.apache.commons.httpclient.cookie.MalformedCookieException;

import java.net.URI;

public class CookieOrigin {
  private final String myHost;
  private final int myPort;
  private final String myPath;
  private final boolean mySecure;

  public CookieOrigin(String host, int port, String path, boolean secure) {
    myHost = host;
    myPort = port;
    myPath = path;
    mySecure = secure;
  }

  public static CookieOrigin fromUri(URI uri) {
    boolean secure = uri.getScheme().equalsIgnoreCase("https");
    int port = uri.getPort();
    if (port < 0) port = secure ? 443 : 80;
    return new CookieOrigin(uri.getHost(), port, uri.getPath(), secure);
  }

  public Cookie[] parce(CookieSpecBase spec, String cookie) throws MalformedCookieException {
    return spec.parse(myHost, myPort, myPath, mySecure, cookie);
  }

  public Cookie[] match(CookieSpecBase spec, Cookie[] cookies) {
    return spec.match(myHost, myPort, myPath, mySecure, cookies);
  }
}
