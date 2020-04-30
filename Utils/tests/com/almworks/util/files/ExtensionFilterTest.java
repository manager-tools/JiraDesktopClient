package com.almworks.util.files;

import com.almworks.util.tests.BaseTestCase;

import java.io.File;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ExtensionFilterTest extends BaseTestCase {
  public void testAccept() {
    ExtensionFileFilter filter = new ExtensionFileFilter("jar", false);
    assertTrue(filter.accept(new TestFile("TestFile.jar", true)));
    assertTrue(filter.accept(new TestFile("another.TestFile.jar", true)));
    assertTrue(filter.accept(new TestFile("/path/to/another.TestFile.jar", true)));
    assertTrue(filter.accept(new TestFile("\\path\\to\\another.TestFile.jar", true)));
    assertTrue(filter.accept(new TestFile("\\path\\to\\another.TestFile.jar.jar", true)));
    assertTrue(filter.accept(new TestFile(".jar", true)));
  }

  public void testReject() {
    ExtensionFileFilter filter = new ExtensionFileFilter("jar", false);
    assertTrue(!filter.accept(new TestFile("jar", true)));
    assertTrue(!filter.accept(new TestFile("fjar", true)));
    assertTrue(!filter.accept(new TestFile("\\path\\to\\another.filejar", true)));
    assertTrue(!filter.accept(new TestFile("\\path\\to\\another.TestFile.jar.", true)));
    assertTrue(!filter.accept(new TestFile("\\path\\to\\another.TestFile.jar.1", true)));
    assertTrue(!filter.accept(new TestFile("", true)));
    assertTrue(!filter.accept(null));
    assertTrue(!filter.accept(new TestFile("filte.jar", false)));
    assertTrue(!filter.accept(new TestFile("filt", false)));
  }

  public static class TestFile extends File {
    private final boolean myExists;

    public TestFile(String pathname, boolean exists) {
      super(pathname);
      myExists = exists;
    }

    // override, so we don't have to create real files
    public boolean isFile() {
      return myExists;
    }
  }
}
