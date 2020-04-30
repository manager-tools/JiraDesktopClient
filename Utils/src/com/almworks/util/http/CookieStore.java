package com.almworks.util.http;

import com.almworks.restconnector.json.ArrayKey;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookieSpecBase;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.URI;
import java.util.*;

public class CookieStore {
  private static final ArrayKey<JSONObject> COOKIE_LIST = ArrayKey.objectArray("cookies");
  private static final JSONKey<String> DOMAIN = JSONKey.text("domain");
  private static final JSONKey<String> NAME = JSONKey.text("name");
  private static final JSONKey<String> VALUE = JSONKey.text("value");
  private static final JSONKey<String> PATH = JSONKey.text("path");
  private static final JSONKey<Boolean> SECURE = JSONKey.bool("secure");

  private final HttpState myCookies = new HttpState();

  public static CookieStore withCookies(List<Cookie> cookies) {
    CookieStore cookieStore = new CookieStore();
    cookieStore.addAll(cookies);
    return cookieStore;
  }

  public Cookie[] selectCookies(URI uri) {
    CookieSpecBase cookieSpec = new CookieSpecBase();
    Cookie[] cookies = CookieOrigin.fromUri(uri).match(cookieSpec, myCookies.getCookies());
    Arrays.sort(cookies, CookieIdentityComparator.INSTANCE);
    return cookies;
  }

  public static CookieStore fromJsonObject(JSONObject root) {
    if (root == null) {
      LogHelper.error("Null JSON object");
      return null;
    }
    List<JSONObject> cookies = COOKIE_LIST.list(root);
    CookieStore store = new CookieStore();
    for (JSONObject obj : cookies) {
      if (obj == null) {
        LogHelper.error("Null cookie", root.toJSONString());
        continue;
      }
      String domain = DOMAIN.getValue(obj);
      String name = NAME.getValue(obj);
      String value = VALUE.getValue(obj);
      String path = PATH.getValue(obj);
      Boolean secure = SECURE.getValue(obj);
      if (domain == null || name == null || value == null || secure == null) {
        LogHelper.error("Expected not null", domain, name, value, path, secure);
        return null;
      }
      store.myCookies.addCookie(new Cookie(domain, name, value, path, null, secure));
    }
    return store;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public JSONObject toJsonObject() {
    JSONArray array = new JSONArray();
    for (Cookie cookie : myCookies.getCookies()) {
      JSONObject obj = new JSONObject();
      obj.put(DOMAIN.getName(), cookie.getDomain());
      obj.put(NAME.getName(), cookie.getName());
      obj.put(VALUE.getName(), cookie.getValue());
      obj.put(PATH.getName(), cookie.getPath());
      obj.put(SECURE.getName(), cookie.getSecure());
      array.add(obj);
    }
    JSONObject root = new JSONObject();
    root.put(COOKIE_LIST.getName(), array);
    return root;
  }

  public void addCookies(URI uri, List<String> cookieValues) {
    HashMap<String, Cookie> result = new HashMap<>();
    CookieOrigin origin = CookieOrigin.fromUri(uri);
    CookieSpecBase spec = new CookieSpecBase();
    for (String cookie : cookieValues) {
      try {
        Cookie[] parsed = origin.parce(spec, cookie);
        for (Cookie next : parsed) {
          String name = next.getName();
          Cookie prev = result.putIfAbsent(name, next);
          if (prev != null) {
            Cookie[] current = myCookies.getCookies();
            Cookie candidate = null;
            for (Cookie c : current) {
              if (c.equals(prev) && Objects.equals(c.getValue(), prev.getValue())) candidate = next;
              else if (c.equals(next) && Objects.equals(c.getValue(), next.getValue())) candidate = prev;
            }
            LogHelper.warning("Duplicated cookie", name, prev, next, candidate);
            if (candidate != null) result.put(name, candidate);
          }
        }
      } catch (MalformedCookieException e) {
        LogHelper.warning(e);
      }
    }
    result.values().forEach(myCookies::addCookie);
  }

  public List<Cookie> getAllCookies() {
    Cookie[] cookies = myCookies.getCookies();
    Arrays.sort(cookies, CookieIdentityComparator.INSTANCE);
    return Collections.unmodifiableList(Arrays.asList(cookies));
  }

  public void addAll(List<Cookie> cookies) {
    if (cookies == null) return;
    cookies.forEach(myCookies::addCookie);
  }

  public void add(Cookie cookie) {
    if (cookie == null) return;
    myCookies.addCookie(cookie);
  }
}
