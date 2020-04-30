package com.almworks.restconnector;

import com.almworks.api.http.HttpUtils;
import com.almworks.util.collections.MultiMap;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.apache.commons.httpclient.Cookie;

import java.util.ArrayList;
import java.util.List;

public class JiraCookies {
  public static final String JSESSIONID = "JSESSIONID";
  /** May be identifiers session, when {@link #JSESSIONID} is missing */
  public static final String XSRF_TOKEN = "atlassian.xsrf.token";
  private static final String ASESSIONID = "ASESSIONID";
  private static final String SERAPH_OS_COOKIE = "seraph.os.cookie";
  public static final String STUDIO_CROWD_COOKIE = "studio.crowd.tokenkey";
  private static final String SERAPH_AUTOLOGIN_COOKIE = "seraph.autologin";
  private static final String[] AUTH_COOKIES = {JSESSIONID, XSRF_TOKEN, ASESSIONID, SERAPH_OS_COOKIE, STUDIO_CROWD_COOKIE, SERAPH_AUTOLOGIN_COOKIE};

  public static MultiMap<String, Cookie> fixCookiesWithWrongSecurePath(MultiMap<String, Cookie> cookies) {
    if (cookies == null)
      return cookies;
    String REMOVE = "/secure";
    MultiMap<String, Cookie> newCookies = new MultiMap<String, Cookie>();
    newCookies.addAll(cookies);
    cookies = newCookies;
    for (String name : AUTH_COOKIES) {
      List<Cookie> list = cookies.getAll(name);
      if (list == null || list.isEmpty())
        continue;
      List<Cookie> fix = null;
      for (Cookie c : list) {
        if (c.isExpired())
          continue;
        String path = c.getPath();
        if (path == null)
          continue;
        if (path.endsWith(REMOVE)) {
          if (fix == null)
            fix = Collections15.arrayList();
          fix.add(c);
        }
      }
      if (fix == null)
        continue;
      // fix cookies
      for (Cookie c : fix) {
        String path = c.getPath();
        if (path == null)
          continue;
        if (path.endsWith(REMOVE)) {

          // using cookie's equals()
          while (cookies.remove(name, c))
            ;

          path = path.substring(0, path.length() - REMOVE.length());
          if (path.length() == 0)
            path = "/";
          c.setPath(path);

          // using cookie's equals()
          while (cookies.remove(name, c))
            ;

          cookies.add(name, c);
        }
      }
    }
    return cookies;
  }

  public static List<Cookie> collectSessionCookies(Cookie[] cookies) {
    MultiMap<String, Cookie> result;
    try {
      result = HttpUtils.copyCookies(HttpUtils.normalizeCookies(cookies));
    } catch (RuntimeException e) {
      // todo patch-release guardian, remove
      Log.error(e);
      result = HttpUtils.copyCookies(cookies);
    }
    result = fixCookiesWithWrongSecurePath(result);
//    ArrayList<Cookie> list = Collections15.arrayList();
//    for (String authCookie : AUTH_COOKIES) {
//      List<Cookie> all = result.getAll(authCookie);
//      if (all != null) list.addAll(all);
//    }
//    return list;
    return new ArrayList<>(result.values());
  }
}
