package com.almworks.util.http;

import com.almworks.util.LogHelper;
import org.almworks.util.ArrayUtil;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class WebCookieManager {
  private static final String SET_COOKIE = "Set-Cookie";
  private static final String COOKIE = "Cookie";
  private final CookieStore myCookieStore;

  public WebCookieManager() {
    this(new CookieStore());
  }

  public WebCookieManager(CookieStore cookieStore) {
    myCookieStore = cookieStore;
  }

  public CookieStore getCookieStore() {
    return myCookieStore;
  }

  public void processHeaders(URI uri, Map<String, List<String>> responseHeaders) {
    for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
      String headerName = entry.getKey();
      if (headerName == null || !SET_COOKIE.equalsIgnoreCase(headerName)) continue;
      List<String> cookieValues = entry.getValue();
      if (cookieValues == null) return;
      myCookieStore.addCookies(uri, cookieValues);
    }
  }

  public void install() {
    CookieHandler.setDefault(new CookieHandler() {
      @Override
      public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
        LogHelper.debug("WebCookie: collecting cookies for", uri);
        Cookie[] cookies = myCookieStore.selectCookies(uri);
        Map<String, List<String>> result;
        if (cookies.length > 0) {
          List<String> cookieList = new ArrayList<>();
          for (Cookie cookie : cookies) {
            cookieList.add(cookie.getName() + "=" + cookie.getValue());
          }
          result = new HashMap<>();
          result.put(COOKIE, cookieList);
        } else result = Collections.emptyMap();
        return result;
      }

      @Override
      public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
        LogHelper.debug("WebCookie: Response from: ", uri, "headers:", responseHeaders);
        processHeaders(uri, responseHeaders);
      }
    });
  }

  /**
   * Compares cookie list. The arguments must be ordered by {@link CookieIdentityComparator#INSTANCE}
   * @return true if both list has the same cookies
   */
  public static boolean areSame(List<Cookie> l1, List<Cookie> l2) {
    boolean res = ArrayUtil.listsEqualsByComparator(l1, l2, CookieIdentityComparator.INSTANCE);
    if (!res || l1 == null || l2 == null || l1.size() != l2.size()) return res;
    for (int i = 0; i < l1.size(); i++) {
      Cookie c1 = l1.get(i);
      Cookie c2 = l2.get(i);
      if (!Objects.equals(c1.getName(), c2.getName())) return false;
      if (!Objects.equals(c1.getValue(), c2.getValue())) return false;
    }
    return true;
  }

  @NotNull
  public static List<Cookie> findCookie(List<Cookie> cookies, String cookieName) {
    return cookies.stream().filter(cookie -> cookieName.equals(cookie.getName())).collect(Collectors.toList());
  }
}
