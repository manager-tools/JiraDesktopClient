package com.almworks.launcher;

import com.almworks.api.install.Setup;
import com.almworks.api.install.TrackerProperties;
import junit.framework.TestCase;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public abstract class LauncherFixture extends TestCase {
  protected static boolean myLaunched;
  protected static String[] myLaunchedArgs;

  protected void launch(Class launcher, String[] args) throws NoSuchMethodException, IllegalAccessException,
    InvocationTargetException {
    Method mainMethod = launcher.getDeclaredMethod("main", new Class[]{String[].class});
    mainMethod.invoke(null, new Object[]{args});
  }

  protected void createDir(File dir) throws IOException {
    if (!dir.mkdir())
      throw new IOException("cannot create dir " + dir);
    dir.deleteOnExit();
  }

  protected File createTempDir() throws IOException {
    File dir = File.createTempFile("launch", null);
    if (!dir.delete())
      throw new IOException("cannot delete file " + dir);
    createDir(dir);
    return dir;
  }

  protected Class createWorkareaAndGetLauncher(File parentDir) throws IOException, ClassNotFoundException {
    if (parentDir == null) {
      parentDir = createTempDir();
    }
    if (!new File(parentDir, Setup.DIR_COMPONENTS).mkdir())
      throw new IOException();
    if (!new File(parentDir, Setup.DIR_LIBRARIES).mkdir())
      throw new IOException();

    File jar = new File(parentDir, "launcher.jar");
    return createLauncherExtClass(jar);
  }

  private Class createLauncherExtClass(File jarFileName) throws IOException, ClassNotFoundException {
    final String CLASS_NAME = "com.almworks.launcher.LauncherTestExt";
    String path = CLASS_NAME.replace('.', '/').concat(".class");
    File jarName = createJarWithClass(path, jarFileName);
    Class launcher = loadClassFromJar(jarName, CLASS_NAME);
    return launcher;
  }

  private Class loadClassFromJar(File jarName, final String className) throws ClassNotFoundException,
    MalformedURLException {

    final String classPath = className.replace('.', '/').concat(".class");
    URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file", null, jarName.getPath())},
      LauncherTest.class.getClassLoader()) {
      protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c != null)
          return c;
        if (name.equals(className)) {
          c = findClass(name);
          if (resolve) {
            resolveClass(c);
          }
          return c;
        } else {
          return super.loadClass(name, resolve);
        }
      }

      public URL getResource(String name) {
        if (name.equals(classPath)) {
          URL resource = findResource(name);
          // special hack to repeat "spaces in home dir" bug
          try {
            resource = new URL(resource.getProtocol(), resource.getHost(), resource.getPort(),
              URLEncoder.encode(resource.getFile(), "UTF-8"));
          } catch (MalformedURLException e) {
            throw new Error("what?");
          } catch (UnsupportedEncodingException e) {
            throw new Error("what-what?");
          }
          return resource;
        } else
          return super.getResource(name);
      }
    };

    Class launcher = classLoader.loadClass(className);
    return launcher;
  }

  private File createJarWithClass(String path, File jarFileName) throws IOException {
    InputStream classIn = LauncherTest.class.getClassLoader().getResourceAsStream(path);
    FileOutputStream output = new FileOutputStream(jarFileName);
    JarOutputStream jarOut = new JarOutputStream(output);
    ZipEntry entry = new ZipEntry(path);
    jarOut.putNextEntry(entry);
    byte[] buffer = new byte[4096];
    while (true) {
      int n = classIn.read(buffer);
      if (n <= 0)
        break;
      jarOut.write(buffer, 0, n);
    }
    jarOut.closeEntry();
    jarOut.close();
    classIn.close();
    return jarFileName;
  }

  protected void setUp() throws Exception {
    super.setUp();
    Setup.cleanupForTestCase();
    System.getProperties().remove(TrackerProperties.HOME);
    System.getProperties().setProperty(Launcher.LAUNCHED_CLASS, TestPlatformLauncher.class.getName());
    myLaunched = false;
    myLaunchedArgs = null;
  }

  protected void tearDown() throws Exception {
    System.getProperties().remove(Launcher.LAUNCHED_CLASS);
    super.tearDown();
  }

  public static final class TestPlatformLauncher {
    public static void launch(String[] args) {
      myLaunched = true;
      myLaunchedArgs = args;
    }
  }
}
