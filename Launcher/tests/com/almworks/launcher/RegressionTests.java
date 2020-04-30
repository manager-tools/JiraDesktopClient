package com.almworks.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class RegressionTests extends LauncherFixture {
  // http://bugzilla/main/show_bug.cgi?id=758
  public void testLaunchFromDirectoryWithExclamationMarks() throws IOException, ClassNotFoundException,
    IllegalAccessException, NoSuchMethodException, InvocationTargetException {

    File dir = createTempDir();
    dir = new File(dir, "waba!");
    if (!dir.mkdir())
      throw new IOException();
    dir = new File(dir, "deskzilla");
    if (!dir.mkdir())
      throw new IOException();

    String[] args = {};
    Class launcher = createWorkareaAndGetLauncher(dir);
    launch(launcher, args);

    assertTrue(myLaunched);
    assertTrue(myLaunchedArgs == args);
  }
}
