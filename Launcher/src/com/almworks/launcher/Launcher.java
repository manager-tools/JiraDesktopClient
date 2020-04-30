package com.almworks.launcher;

import com.almworks.api.install.Setup;
import com.almworks.api.install.TrackerProperties;
import com.almworks.appinit.EventQueueReplacement;
import javafx.application.Platform;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.logging.*;

public class Launcher {
  private final String[] myArgs;
  private ClassLoader myClassLoader;
  public static final String LAUNCHED_CLASS = "com.almworks.platform.ComponentLoader";
  public static final String LAUNCHED_METHOD = "launch";
  private static final String IDEA_OUTPUT_PATH_LINK = ".cp";
  private static final String IDEA_OUTPUT_PATH_LINK_2 = ".cprc";

  public static void main(String[] args) {
    Platform.setImplicitExit(false);
    new Launcher(args).run();
  }

  public Launcher(String[] args) {
    myArgs = args;
  }

  protected void run() {
    try {
      setupJava();
      replaceEventQueue();
      setupBootstrapLogging();
      setupHomeDir();
      setupWorkspaceDir();
      setupLauncherFileLogging();
      writeBanner();
      myClassLoader = createClassLoader();
      Method method = getLaunchMethod();
      method.invoke(null, new Object[] {myArgs});
    } catch (NoClassDefFoundError e) {
      logger().log(Level.SEVERE, "java.class.path=" + System.getProperty("java.class.path"));
      abort(e);
    } catch (Exception e) {
      abort(e);
    }
  }

  /**
   * Set Java properties to configure Java subsystems before any is in use
   */
  private void setupJava() {
    // enable cross-origin requests in JavaFX WebView
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
  }

  private void setupWorkspaceDir() {
    File workspace;

    // 1. Try explicit setting passed from the environment
    String explicit = Setup.getStringProperty(TrackerProperties.WORKSPACE);
    workspace = validateWorkspaceDir(explicit, true);
    if (workspace != null) {
      Setup.setWorkspaceDir(workspace, false);
      Setup.setWorkspaceExplicit(true);
      return;
    }

    // 1a. Try command line
    if (myArgs != null && myArgs.length > 0) {
      explicit = myArgs[0];
      workspace = validateWorkspaceDir(explicit, true);
      if (workspace != null) {
        Setup.setWorkspaceDir(workspace, false);
        Setup.setWorkspaceExplicit(true);
        return;
      }
    }

    // 1b. Try environment variable
    try {
      explicit = System.getenv(Setup.ENV_VARIABLE_WORKSPACE);
    } catch (Throwable e) {
      // ignore
      explicit = null;
    }
    workspace = validateWorkspaceDir(explicit, true);
    if (workspace != null) {
      Setup.setWorkspaceDir(workspace, false);
      Setup.setWorkspaceExplicit(true);
      return;
    }

    // 1c. Try another environment variable
    try {
      explicit = System.getenv(Setup.ENV_VARIABLE_WORKSPACE_2);
    } catch (Throwable e) {
      // ignore
      explicit = null;
    }
    workspace = validateWorkspaceDir(explicit, true);
    if (workspace != null) {
      Setup.setWorkspaceDir(workspace, false);
      Setup.setWorkspaceExplicit(true);
      return;
    }

    // 2. Try to find workspace at home
    File athome = new File(Setup.getHomeDir(), Setup.DIR_WORKSPACE_OLD_FASHIONED);
    workspace = validateWorkspaceDir(athome.getAbsolutePath(), false);
    if (workspace != null) {
      Setup.setWorkspaceDir(workspace, true);
      return;
    }

    // 3. Use default location - user's folder
    String userHome = Setup.getStringProperty("user.home");
    if (userHome != null) {
      File candidate = null;
      try {
        candidate = new File(userHome, Setup.getWorkspaceDirectoryInUserHome());
      } catch (Exception e) {
        // ignore
      }
      if (candidate != null) {
        workspace = validateWorkspaceDir(candidate.getAbsolutePath(), true);
        if (workspace != null) {
          Setup.setWorkspaceDir(workspace, false);
          return;
        }
      }
    }

    // 4. If no home (?) - use home dir anyway
    workspace = validateWorkspaceDir(athome.getAbsolutePath(), true);
    if (workspace != null) {
      Setup.setWorkspaceDir(workspace, true);
      return;
    }

    // 5. Cannot do anything
    logger().log(Level.SEVERE,
      "cannot set up workspace directory, consider using " + TrackerProperties.WORKSPACE + " option");
    throw new RuntimeException("cannot set up workspace directory");
  }

  private File validateWorkspaceDir(String dirname, boolean mayCreate) {
    if (dirname == null)
      return null;
    File dir;
    try {
      dir = new File(dirname).getAbsoluteFile();
    } catch (Exception e) {
      logger().log(Level.WARNING, "cannot use workspace dir [" + dirname + "]");
      return null;
    }
    logger().info("Trying workspace directory: '" + dir + "' mayCreate:" + mayCreate);


    if (!dir.exists()) {
      if (!mayCreate) {
        logger().log(Level.INFO, dirname + " does not exist, not creating");
        return null;
      }
      File parent = dir.getParentFile();
      if (parent == null) {
        logger().log(Level.WARNING, "no parent for " + dir);
        return null;
      }
      if (!parent.exists()) {
        logger().log(Level.WARNING, parent + " does not exist");
        return null;
      }
      if (!parent.isDirectory()) {
        logger().log(Level.WARNING, parent + " is not a directory");
        return null;
      }
      if (!parent.canWrite()) {
        logger().log(Level.WARNING, parent + " is not writable");
        return null;
      }
      boolean success = false;
      for (int i = 0; i < 3; i++) {
        success = dir.mkdir();
        if (success)
          break;
        try {
          Thread.sleep(300);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      if (!success) {
        logger().log(Level.WARNING, "cannot create directory " + dir);
        return null;
      }
    }

    assert dir.exists();

    if (!dir.isDirectory()) {
      logger().log(Level.WARNING, dir + " is not a directory");
      return null;
    }
    if (!dir.canWrite()) {
      logger().log(Level.WARNING, "cannot write to " + dir);
      return null;
    }
    logger().info("Workspace directory '" + dir + "' approved");
    return dir;
  }

  private void replaceEventQueue() {
    EventQueueReplacement.ensureInstalled();
  }

  private static void abort(Throwable e) {
//    _debug("aborting", e);
    logger().log(Level.SEVERE, "Application cannot be started", e);
    if (Setup.isRunningJUnit()) {
      // this is a special condition check so we have a chance to see LauncherTest failure
      System.err.println("abort");
      throw new Error(e);
    }
    System.exit(2);
  }

  private void setupBootstrapLogging() {
    // Logging when we don't know home dir yet.
    boolean configurationRead = readLoggingConfiguration();
    if (!configurationRead) {
      // default configuration part one - only console.
      // will be more after finding home
      Logger logger = Logger.getLogger("");
      Handler[] handlers = logger.getHandlers();
      for (Handler handler : handlers) logger.removeHandler(handler);
      ConsoleHandler consoleHandler = new ConsoleHandler();
      consoleHandler.setFormatter(new StartupFormatter());
      consoleHandler.setLevel(getLogLevel(Level.SEVERE, Level.INFO));
      logger.setLevel(Level.FINE);
      logger.addHandler(consoleHandler);
    }
  }

  private void setupLauncherFileLogging() {
    // this may be re-set-up later in ComponentLoader
    try {
      FileHandler fileHandler = Setup.getLoggingFileHandler();
      fileHandler.setFormatter(new StartupFormatter());
      Level logLevel = getLogLevel(Level.WARNING, Level.FINE);
      fileHandler.setLevel(logLevel);
      Logger logger = Logger.getLogger("");
      logger.setLevel(logLevel);
      logger.addHandler(fileHandler);
      if (!"true".equalsIgnoreCase(System.getProperty("sqlite4java.debug")))
        Logger.getLogger("com.almworks.sqlite4java").setLevel(Level.WARNING);
    } catch (IOException e) {
      Logger.getLogger("").log(Level.WARNING, "cannot configure logging to files", e);
    }
  }

  private boolean readLoggingConfiguration() {
    String configFile = Setup.getStringProperty(TrackerProperties.LOGCONFIG);
    if (configFile == null)
      return false;
    File file = new File(configFile);
    if (!file.isFile() || !file.canRead()) {
      // todo write something
      return false;
    }
    try {
      System.setProperty("java.util.logging.config.file", file.getCanonicalPath());
      LogManager.getLogManager().readConfiguration();
    } catch (IOException e) {
      // strange, but will do
      // todo write something
      return false;
    }
    return true;
  }

  private void writeBanner() {
    Logger logger = logger();
    logger.info("APPLICATION STARTS");
    logger.fine("HOME = " + Setup.getHomeDir());
  }

  private void setupHomeDir() {
    if (tryHomeDir(Setup.getStringProperty(TrackerProperties.HOME), "passed with property " + TrackerProperties.HOME))
      return;

    String homeDir;
    try {
      // some JDK do not support getenv()
      homeDir = System.getenv(Setup.ENV_VARIABLE_HOME);
    } catch (Throwable e) {
      homeDir = null;
    }
    if (tryHomeDir(homeDir, "passed with environment variable " + Setup.ENV_VARIABLE_HOME))
      return;
    try {
      // some JDK do not support getenv()
      homeDir = System.getenv(Setup.ENV_VARIABLE_HOME_2);
    } catch (Throwable e) {
      homeDir = null;
    }
    if (tryHomeDir(homeDir, "passed with environment variable " + Setup.ENV_VARIABLE_HOME_2))
      return;

    if (tryHomeDir(findHome(), "gathered from the system"))
      return;

    abort(new Error("cannot locate product home directory"));
    throw new Error("must never get here");
  }

  private boolean tryHomeDir(String homeDir, String whereFrom) {
    if (homeDir == null)
      return false;
    boolean valid = isHomeDirValid(homeDir);
    if (valid)
      setHomeDir(homeDir);
    else
      logger().warning("product home " + whereFrom + " is not valid (" + homeDir + ")");
    return valid;
  }

  private boolean isHomeDirValid(String homeDir) {
    if (homeDir == null)
      return false;
    File home = new File(homeDir);
    if (!home.isDirectory())
      return false;

    //noinspection SimplifiableIfStatement
    if (Setup.isDebugging() || Setup.isRunFromIDE()) {
      // In debugging environment, we might not have required folders
      return true;
    }

    return isHomeDir(home);
  }

  private boolean isHomeDir(File home) {
    //noinspection SimplifiableIfStatement
    if (home == null)
      return false;
    return home.isDirectory() && new File(home, Setup.DIR_LIBRARIES).isDirectory() &&
      new File(home, Setup.DIR_COMPONENTS).isDirectory();
  }

  private void setHomeDir(String homeDir) {
    System.setProperty(TrackerProperties.HOME, homeDir);
    Setup.setHomeDir(new File(homeDir));
  }

  private static File getJarByClassURL(URL url) {
    if (url == null)
      return null;
    if (!"jar".equals(url.getProtocol()))
      return null;
    String prefix = "file:";
    String path = decodePath(url);
    if (path == null || !path.startsWith(prefix))
      return null;
    String jarPath = path.substring(prefix.length());
    int k = jarPath.lastIndexOf('!');
    if (k >= 0)
      jarPath = jarPath.substring(0, k);
    File jarFile = new File(jarPath);
    if (!jarFile.isFile())
      return null;
    return jarFile;
  }

  private static String decodePath(URL url) {
    try {
      return URLDecoder.decode(url.getPath(), "UTF-8");
    } catch (Exception e) {
      abort(e);
      return null;
    }
  }

  private URL getMyClassURL() {
    String launcherClassName = getClass().getName();
    return getClass().getClassLoader().getResource(launcherClassName.replace('.', '/').concat(".class"));
  }

  private String findHome() {
    URL url = getMyClassURL();
    if (url == null)
      return null;
    if ("jar".equals(url.getProtocol())) {
// Launcher is deployed in jar
      File jarFile = getJarByClassURL(url);
      if (jarFile != null) {
        File jarDir = jarFile.getParentFile();
// assume our jar is in root directory, or in a subdirectory
        if (jarDir != null) {
          if (isHomeDir(jarDir))
            return jarDir.getPath();
          File jarParentDir = jarDir.getParentFile();
          if (jarParentDir != null) {
            if (isHomeDir(jarParentDir))
              return jarParentDir.getPath();
          }
        }
      }
    } else if ("file".equals(url.getProtocol())) {
// Launcher is run as a separate class (debugger/tests): find parent directory with "lib" subdirectory
      String pathname = decodePath(url);
      if (pathname == null) return null;
      for (File dir = new File(pathname); dir != null; dir = dir.getParentFile()) {
        if (dir.isDirectory() && new File(dir, Setup.DIR_LIBRARIES).isDirectory())
          return dir.getPath();
      }
    }
    return null;
  }

  private ClassLoader createClassLoader() throws MalformedURLException {
    ArrayList<File> urllist = new ArrayList<>();
    gatherLibUrls(urllist);
    gatherComponentUrls(urllist);
    gatherDebugUrls(urllist);

    final URL[] urlarray = new URL[urllist.size()];
    int i = 0;
    for (Object anUrllist : urllist) {
      File jarFile = (File) anUrllist;
      logger().fine("LIBRARY += " + jarFile);
      String path = jarFile.getPath();
      if (jarFile.isDirectory())
        path += "/";
      urlarray[i++] = new URL("file", null, path);
    }

    return new LauncherLoader(urlarray);
  }

  private void gatherDebugUrls(ArrayList<File> urllist) {
    File link = new File(Setup.getHomeDir(), IDEA_OUTPUT_PATH_LINK);
    if (link.isDirectory())
      addSubdirsToURLList(link, urllist);
    link = new File(Setup.getHomeDir(), IDEA_OUTPUT_PATH_LINK_2);
    if (link.isDirectory())
      addSubdirsToURLList(link, urllist);
  }

  private void addSubdirsToURLList(File linkToOutput, ArrayList<File> urllist) {
    File[] moduleDirs = linkToOutput.listFiles((FileFilter) null);
    if (moduleDirs == null)
      return;
    for (File dir : moduleDirs) {
      if (dir.isDirectory()) {
        if (new File(dir, "com").isDirectory()) {
          urllist.add(dir);
        }
      }
    }
  }

  private void gatherComponentUrls(List<File> targetList) {
    // place JARs in order of decreasing mtime, so that if there is a class collision,
    // later version takes precedence.
    TreeSet<File> sortedJars = new TreeSet<>((file1, file2) -> {
      if (file1.equals(file2))
        return 0;
      long m1 = file1.lastModified();
      long m2 = file2.lastModified();
      if (m1 > m2)
        return -1;
      else if (m1 < m2)
        return 1;
      else
        return file1.getPath().compareTo(file2.getPath());
    });

    File componentsDir = new File(Setup.getHomeDir(), Setup.DIR_COMPONENTS);
    File[] jars = componentsDir.listFiles(new ExtensionFilter("jar"));
    if (jars == null)
      return;
    for (File jar : jars)
      if (isJarValid(jar))
        sortedJars.add(jar);

    targetList.addAll(sortedJars);
  }

  private void gatherLibUrls(List<File> targetList) {
    File libDir = new File(Setup.getHomeDir(), Setup.DIR_LIBRARIES);
    File[] jars = libDir.listFiles(new ExtensionFilter("jar"));
    if (jars == null)
      return;
    for (File jarFile : jars) {
      if (isJarObsolete(jarFile)) {
        logger().log(Level.WARNING, "library lib/" + jarFile.getName() + " is obsolete and can be deleted");
        continue;
      }
      if (isJarValid(jarFile)) {
        targetList.add(jarFile);
      }
    }
  }

  private boolean isJarObsolete(File jarFile) {
    String name = jarFile.getName();
    return "deskzilla-external-api.jar".equalsIgnoreCase(name) || "jdom.jar".equalsIgnoreCase(name);
  }

  private boolean isJarValid(File jarFile) {
    if (!jarFile.isFile())
      return badJar(jarFile + " is a directory", null);
    if (!jarFile.canRead())
      return badJar("cannot read " + jarFile, null);
    try {
      JarFile jar = new JarFile(jarFile);
      jar.getManifest();
    } catch (Exception e) {
      return badJar("exception", e);
    }
    return true;
  }

  private boolean badJar(String message, Throwable exception) {
    logger().log(Level.WARNING, message, exception);
    return false;
  }

  private Method getLaunchMethod() throws ClassNotFoundException, NoSuchMethodException {
    String launchClassName = Setup.getStringProperty(LAUNCHED_CLASS, LAUNCHED_CLASS);
    Class<?> launcherClass = myClassLoader.loadClass(launchClassName);
    return launcherClass.getMethod(LAUNCHED_METHOD, String[].class);
  }

  private static Logger logger() {
    return Logger.getLogger("com.almworks.launcher");
  }

  private boolean isDebug() {
    return Setup.getBooleanProperty(TrackerProperties.DEBUG);
  }

  private Level getDebugLevel() {
    String s = Setup.getStringProperty(TrackerProperties.DEBUG_LEVEL);
    if (s != null)
      try {
        return Level.parse(s);
      } catch (IllegalArgumentException e) {
        // bad level
      }
    return null;
  }

  private Level getLogLevel(Level levelNoDebug, Level levelDebugDefault) {
    if (!isDebug()) {
      return levelNoDebug;
    } else {
      Level level = getDebugLevel();
      if (level == null)
        level = levelDebugDefault;
      else if (level.intValue() > Level.INFO.intValue())
        level = Level.INFO;
      return level;
    }
  }

  private class LauncherLoader extends URLClassLoader {

    public LauncherLoader(URL[] urlarray) {
      super(urlarray, Launcher.class.getClassLoader());
    }
  }
}
