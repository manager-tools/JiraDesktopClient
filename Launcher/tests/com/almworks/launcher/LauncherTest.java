package com.almworks.launcher;

import com.almworks.api.install.Setup;
import com.almworks.api.install.TrackerProperties;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;


/**
 * :todoc:
 *
 * @author sereda
 */
public class LauncherTest extends LauncherFixture {
  public void testLaunch() throws IOException {
    String[] args = {};
    File homeDir = File.createTempFile("launcher", "test");
    if (homeDir.exists() && !homeDir.delete())
      throw new IOException("cannot delete temp file");
    createDir(homeDir);
    createDir(new File(homeDir, Setup.DIR_LIBRARIES));
    createDir(new File(homeDir, Setup.DIR_COMPONENTS));
    System.setProperty(TrackerProperties.HOME, homeDir.getCanonicalPath());
    System.setProperty(TrackerProperties.DEBUG, "true");
    Launcher.main(args);
    assertTrue(myLaunched);
    assertTrue(myLaunchedArgs == args);
  }

  public void testLaunchFromJar() throws IOException, ClassNotFoundException, NoSuchMethodException,
    IllegalAccessException, InvocationTargetException {

    String[] args = {};
    Class launcher = createWorkareaAndGetLauncher(null);
    launch(launcher, args);
    assertTrue(myLaunched);
    assertTrue(myLaunchedArgs == args);
  }

  public void testLaunchFromDirectoryWithSpaces() throws IOException, ClassNotFoundException, NoSuchMethodException,
    IllegalAccessException, InvocationTargetException {

    File dir = createTempDir();
    dir = new File(dir, "Program Files");
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
