package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.http.CookieStore;
import com.almworks.util.http.WebCookieManager;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Objects;

/**
 * This is an interface to deal with WebLogin configuration.<br>
 * To read configuration just deserialize this object and use accessors.<br>
 * To modify configuration, first deserialize this object, then make modifications and serialize it back.
 */
public class WebLoginConfig {
  private static final JSONKey<JSONObject> COOKIES = JSONKey.object("cookies");

  private final CookieStore myCookies;

  public WebLoginConfig() {
    this(new CookieStore());
  }

  private WebLoginConfig(CookieStore cookies) {
    myCookies = cookies;
  }

  public CookieStore getCookies() {
    return myCookies;
  }

  @Nullable
  public static WebLoginConfig fromJson(String json) {
    JSONObject root = JSONKey.ROOT_OBJECT.parseNoExceptions(json);
    if (root == null) return null;
    CookieStore cookieStore = CookieStore.fromJsonObject(COOKIES.getValue(root));
    if (cookieStore == null) return null;
    return new WebLoginConfig(cookieStore);
  }

  @SuppressWarnings("unchecked")
  public String toJson() {
    JSONObject root = new JSONObject();
    root.put(COOKIES.getName(), myCookies.toJsonObject());
    return root.toJSONString();
  }

  public boolean sameConnectionSettings(WebLoginConfig other) {
    return other != null && WebCookieManager.areSame(getCookies().getAllCookies(), other.getCookies().getAllCookies());
  }

  public static WebLoginConfig create(List<Cookie> cookies) {
    return new WebLoginConfig(CookieStore.withCookies(cookies));
  }

  public boolean updateCookies(List<Cookie> newCookies) {
    List<Cookie> cookies = myCookies.getAllCookies();
    MultiMap<String, Cookie> byName = new MultiMap<>();
    for (Cookie c : cookies) byName.add(c.getName(), c);
    boolean updated = false;
    for (Cookie c : newCookies) {
      Cookie known = findEqual(byName.getAll(c.getName()), c);
      if (known == null || known.getSecure() != c.getSecure() || !Objects.equals(known.getValue(), c.getValue())) {
        myCookies.add(c);
        updated = true;
      }
    }
    return updated;
  }

  private Cookie findEqual(List<Cookie> list, Cookie cookie) {
    if (list == null) return null;
    for (Cookie c : list) {
      if (c.equals(cookie)) return c;
    }
    return null;
  }
}
