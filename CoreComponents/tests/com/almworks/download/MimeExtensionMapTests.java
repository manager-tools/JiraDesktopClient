package com.almworks.download;

import com.almworks.util.tests.BaseTestCase;

public class MimeExtensionMapTests extends BaseTestCase {
  public void test() {
    check("text/plain", "txt");
    check("text/html", "html");
    check("application/xml", "xml");
    check("image/jpeg", "jpg");
    check("image/gif", "gif");
    check("image/png", "png");
  }

  private void check(String mime, String extension) {
    assertEquals(extension, MimeExtensionMap.guessExtension(mime, ""));
  }
}
