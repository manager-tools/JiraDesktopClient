package com.almworks.util.http;

import com.almworks.util.tests.BaseTestCase;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CookieStoreTest extends BaseTestCase {
  private static final URI DOMAIN = URI.create("https://b.com/");
  public void testPrefersNewCookie() {
    // Test new cookie last
    CookieStore store = new CookieStore();
    store.addCookies(DOMAIN, Collections.singletonList(createCookie("V1")));
    store.addCookies(DOMAIN, Arrays.asList(createCookie("V1"), createCookie("V2")));
    List<Cookie> all = store.getAllCookies();
    assertEquals(1, all.size());
    assertEquals("V2", all.get(0).getValue());
    // Test new cookie first
    store = new CookieStore();
    store.addCookies(DOMAIN, Collections.singletonList(createCookie("V1")));
    store.addCookies(DOMAIN, Arrays.asList(createCookie("V2"), createCookie("V1")));
    all = store.getAllCookies();
    assertEquals(1, all.size());
    assertEquals("V2", all.get(0).getValue());
  }

  @NotNull
  private String createCookie(String value) {
    return new Cookie("a.b.c", "A", value).toExternalForm();
  }
}
