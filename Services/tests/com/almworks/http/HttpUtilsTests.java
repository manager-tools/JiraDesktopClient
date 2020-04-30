package com.almworks.http;

import com.almworks.api.http.HttpUtils;
import com.almworks.util.tests.BaseTestCase;

public class HttpUtilsTests extends BaseTestCase {
  public static void testAddGetParameterIfMissing() {
    check("http://haba/whatever", "x", "y", "http://haba/whatever?x=y");
    check("http://haba/whatever?x=z", "x", "y", "http://haba/whatever?x=z");
    check("http://haba/whatever", "whatever", "y", "http://haba/whatever?whatever=y");
    check("http://haba/whatever?wwhatever=z", "whatever", "y", "http://haba/whatever?wwhatever=z&whatever=y");
    check("http://haba/whatever?whateverr=z", "whatever", "y", "http://haba/whatever?whateverr=z&whatever=y");
  }

  private static void check(String base, String name, String value, String result) {
    String url = HttpUtils.addGetParameterIfMissing(base, name, value);
    assertEquals(result, url);
  }
}
