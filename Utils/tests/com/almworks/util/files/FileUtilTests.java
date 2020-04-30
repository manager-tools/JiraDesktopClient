package com.almworks.util.files;

import com.almworks.util.tests.BaseTestCase;

import java.util.Locale;

public class FileUtilTests extends BaseTestCase {

  private Locale myDefaultLocale;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myDefaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
  }

  @Override
  protected void tearDown() throws Exception {
    if (myDefaultLocale != null) Locale.setDefault(myDefaultLocale);
    super.tearDown();
  }

  public void testDisplayableSize() {
    assertEquals("1 B", FileUtil.displayableSize(1));
    assertEquals("999 B", FileUtil.displayableSize(999));
    assertEquals("1023 B", FileUtil.displayableSize(1023));
    assertEquals("1 KB", FileUtil.displayableSize(1024));
    assertEquals("1 KB", FileUtil.displayableSize(1026));
    assertEquals("1.01 KB", FileUtil.displayableSize(1036));
    assertEquals("10 KB", FileUtil.displayableSize(10*1024));
    assertEquals("10 KB", FileUtil.displayableSize(10*1024 + 10));
    assertEquals("10 KB", FileUtil.displayableSize(10*1024 + 100));
    assertEquals("10.1 KB", FileUtil.displayableSize(10*1024 + 103));
    assertEquals("100 KB", FileUtil.displayableSize(100*1024));
    assertEquals("100 KB", FileUtil.displayableSize(100*1024 + 1000));
    assertEquals("100 KB", FileUtil.displayableSize(100*1024 + 1023));

    assertEquals("100 GB", FileUtil.displayableSize(100l*1024*1024*1024));
    assertEquals("1000 GB", FileUtil.displayableSize(1000l*1024*1024*1024));
    assertEquals("1000 GB", FileUtil.displayableSize(1001l*1024*1024*1024 - 1));
    assertEquals("1001 GB", FileUtil.displayableSize(1001l*1024*1024*1024));
  }
}
