package com.almworks.http;

import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.commons.httpclient.cookie.RFC2109Spec;

import java.util.List;
import java.util.Set;

public class RFC2109Loosened extends RFC2109Spec {
  public static final String RFC2109_LOOSENED = "rfc2109_loosened";

  public void validate(String host, int port, String path, boolean secure, Cookie cookie)
    throws MalformedCookieException
  {

    try {
      super.validate(host, port, path, secure, cookie);
    } catch (MalformedCookieException e) {
      String message = Util.upper(Util.NN(e.getMessage()));
      if (message.indexOf("ILLEGAL DOMAIN") >= 0 || message.indexOf("ILLEGAL PATH") >= 0 ||
        message.indexOf("DOMAIN ATTRIBUTE") >= 0)
      {

        // correct cookie...
        // todo in general, this is not correct and even unsafe. in our case this is acceptable
        // maybe ask user?

        Log.debug("correcting " + cookie + " (domain=" +
          (cookie.isDomainAttributeSpecified() ? cookie.getDomain() : "") + ", path=" +
          (cookie.isPathAttributeSpecified() ? cookie.getPath() : "") + ")");
        cookie.setDomainAttributeSpecified(false);
        cookie.setPathAttributeSpecified(true);
        cookie.setPath("/");

        // ignore
        return;
      }
      throw e;
    }
  }

  /**
   * Copied from CookieSpecBase.
   */
  public boolean domainMatch(String host, String domain) {
    if (host.equals(domain)) {
      return true;
    }
    if (!domain.startsWith(".")) {
      domain = "." + domain;
    }
    return host.endsWith(domain) || host.equals(domain.substring(1));
  }


  public Cookie[] match(String host, int port, String path, boolean secure, Cookie cookies[]) {
    Cookie[] result = super.match(host, port, path, secure, cookies);
    if (result == null || result.length == 0) {
      return result;
    }
    // remove redundant cookies with same names
    // NB: cookies must come in "specific-first" order.
    // 1. see if we got any duplicates
    Set<String> names = Collections15.hashSet();
    boolean duplicate = false;
    for (Cookie cookie : result) {
      if (!names.add(cookie.getName())) {
        duplicate = true;
        break;
      }
    }
    if (duplicate) {
      names.clear();
      List<Cookie> list = Collections15.arrayList();                                                                       
      for (Cookie cookie : result) {
        if (names.add(cookie.getName())) {
          list.add(cookie);
        }
      }
      result = list.toArray(new Cookie[list.size()]);
    }
    return result;
  }


  public String formatCookies(Cookie[] cookies) {
    if (cookies == null) {
      throw new IllegalArgumentException("Cookie array may not be null");
    }
    if (cookies.length == 0) {
      throw new IllegalArgumentException("Cookie array may not be empty");
    }

    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < cookies.length; i++) {
      if (i > 0) {
        buffer.append("; ");
      }
      buffer.append(formatCookie(cookies[i]));
    }
    return buffer.toString();
  }

  public String formatCookie(Cookie cookie) {
    if (cookie == null) {
      throw new IllegalArgumentException("Cookie may not be null");
    }
    StringBuffer buf = new StringBuffer();
    buf.append(cookie.getName());
    buf.append("=");
    String s = cookie.getValue();
    if (s != null) {
      buf.append(s);
    }
    return buf.toString();
  }
}
