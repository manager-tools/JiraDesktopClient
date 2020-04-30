package com.almworks.util;

import com.almworks.util.tests.BaseTestCase;

public class EnvTests extends BaseTestCase {
  TestEnvImpl myImpl;

  protected void setUp() throws Exception {
    super.setUp();
    myImpl = new TestEnvImpl();
    Env.setImpl(myImpl);
  }

  protected void tearDown() throws Exception {
    Env.setImpl(null);
    super.tearDown();
  }

  public void testVersions() {
    setOs("Windows Vista", "6.0000");
    assertTrue(Env.isWindows());
    assertFalse(Env.isWindows2000orEarlier());
    assertTrue(Env.isWindowsVistaOrLater());

    setOs("Microsoft Windows", "5.0000");
    assertTrue(Env.isWindows());
    assertTrue(Env.isWindows2000orEarlier());
    assertFalse(Env.isWindowsVistaOrLater());

    setOs("Mac OS X", "10.4.1");
    assertTrue(Env.isMac());
    assertFalse(Env.isMacLeopardOrNewer());

    setOs("Mac OS X", "10.5.1");
    assertTrue(Env.isMac());
    assertTrue(Env.isMacLeopardOrNewer());

    setOs("Mac OS X", "10.6");
    assertTrue(Env.isMac());
    assertTrue(Env.isMacLeopardOrNewer());

    setOs("Mac OS X", "11");
    assertTrue(Env.isMac());
    assertTrue(Env.isMacLeopardOrNewer());

    setOs("Mac OS X", "10");
    assertTrue(Env.isMac());
    assertFalse(Env.isMacLeopardOrNewer());
  }

  private void setOs(String name, String version) {
    // clean cache
    Env.setImpl(myImpl);
    
    myImpl.setProperty("os.name", name);
    myImpl.setProperty("os.version", version);
  }

  public void testMinMax() {
    myImpl.setProperty("x", "100");
    assertEquals(100, Env.getInteger("x", 0, 10000, -1));
    assertEquals(100, Env.getInteger("x", 100, 10000, -1));
    assertEquals(101, Env.getInteger("x", 101, 10000, -1));
    assertEquals(100, Env.getInteger("x", 0, 100, -1));
    assertEquals(99, Env.getInteger("x", 0, 99, -1));
    assertEquals(100, Env.getInteger("x", 100, 100, -1));
  }
}
