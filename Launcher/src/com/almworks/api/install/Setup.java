package com.almworks.api.install;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class Setup {
  private static final String PROPERTY_SETUP_PROPERTIES = "setup.properties";
  private static final String SETUP_PROPERTIES = "com/almworks/rc/setup.properties";
  private static final String SETUP_PROPERTY_PRODUCT_ID = "product.id";
  private static final String SETUP_PROPERTY_PRODUCT_NAME = "product.name";
  private static final String SETUP_JIRACLIENT_PRODUCT_ID = "jiraclient";
  private static final String SETUP_DESKZILLA_PRODUCT_ID = "deskzilla";
  private static final String SETUP_DEFAULT_PRODUCT_ID = SETUP_DESKZILLA_PRODUCT_ID;
  public static final String SETUP_DEFAULT_PRODUCT_NAME = "Deskzilla";

  // 90 days to try beta
  public static final long MAXIMUM_TIME_ALLOWED_TO_USE_BETA_AFTER_BETA_RELEASE = 90L * 86400L * 1000L;

  private static Properties mySetupProperties;
  private static final String myProductId = loadProductId();
  private static final String myProductName = loadProductName();

  private static String loadProductId() {
    loadSetupProperties();
    assert mySetupProperties != null;
    String productId = mySetupProperties.getProperty(SETUP_PROPERTY_PRODUCT_ID);
    if (!SETUP_JIRACLIENT_PRODUCT_ID.equalsIgnoreCase(productId)) {
      productId = SETUP_DEFAULT_PRODUCT_ID;
    }
    return productId;
  }

  private static String loadProductName() {
    loadSetupProperties();
    assert mySetupProperties != null;
    return mySetupProperties.getProperty(SETUP_PROPERTY_PRODUCT_NAME, SETUP_DEFAULT_PRODUCT_NAME);
  }

  private synchronized static void loadSetupProperties() {
    if (mySetupProperties == null) {
      final String resource = System.getProperty(PROPERTY_SETUP_PROPERTIES, SETUP_PROPERTIES);
      mySetupProperties = PropertyUtils.loadProperties(Setup.class, resource);
    }
  }

  public static final String DIR_COMPONENTS = "components";
  public static final String DIR_LIBRARIES = "lib";
  public static final String DIR_LOGS = "log";
  public static final String DIR_ETC = "etc";
  public static final String DIR_WORKSPACE_OLD_FASHIONED = "workspace";
  public static final String DIR_WORKSPACE_DATABASE = "db";
  public static final String DIR_WORKSPACE_DOWNLOAD = "download";
  public static final String DIR_WORKSPACE_UPLOAD = "upload";
  public static final String DIR_WORKSPACE_STATE = "system2";
  public static final String DIR_CONFIG_BACKUP_DIR = "backup";
  public static final String DIR_TEMP = "temp";

  public static final String FILE_WORKSPACE_LOCK = ".lock";
  public static final String FILE_WORKSPACE_CONFIG = "config.xml";

  public static final String PREFERENCES_INSTANCES = "instances";

  public static final String ENV_VARIABLE_HOME = "DESKZILLA_HOME";
  public static final String ENV_VARIABLE_HOME_2 = "JIRACLIENT_HOME";

  public static final String ENV_VARIABLE_WORKSPACE = "DESKZILLA_WORKSPACE";
  public static final String ENV_VARIABLE_WORKSPACE_2 = "JIRACLIENT_WORKSPACE";

  public static final String LOG_FILE_PATTERN = "tracker%g.log";

  public static final String URL_ALMWORKS = "http://almworks.com";

  public static final int LOG_FILE_COUNT = 4;
  public static final int LOG_FILE_LIMIT = 10000000;

  private static final String BUNDLED_PROPERTIES = "com/almworks/almworks.properties";
  private static final long myLoadedTime = System.currentTimeMillis();

  private static final String[] SYSTEM_PROPERTY_PREFIXES = {
    "java.", "user.", "os.", "awt.", "file.", "line.", "path.",
    "sun.", "apple.", "com.apple.", "mrj.", "install4j.", "exe4j.",
    "ftp.nonProxyHosts", "http.nonProxyHosts", "socksNonProxyHosts", "gopherProxySet",
  };

  public static String getPropertyFileName() {
    return isJiraClient() ? "jiraclient.properties" : "deskzilla.properties";
  }

  private static abstract class Overrider {
    private volatile boolean myApplied = false;

    protected abstract File getBaseDir();

    public void apply() {
      if(!myApplied) {
        final File base = getBaseDir();
        if(base != null) {
          myApplied = true;
          final File file = new File(base, getPropertyFileName());
          final Properties props = PropertyUtils.loadProperties(file);
          for (Map.Entry<Object, Object> e : props.entrySet()) {
            if (e.getKey() instanceof String && e.getValue() instanceof String) {
              System.setProperty((String) e.getKey(), (String) e.getValue());
            }
          }
          myProperties.putAll(props);
        }
      }
    }
  }

  private static Overrider[] myOverriders = {
    new Overrider() { @Override protected File getBaseDir() { return myHomeDir; }},
    new Overrider() { @Override protected File getBaseDir() { return myWorkspaceDir; }}
  };

  private static File myHomeDir = null;
  private static File myWorkspaceDir = null;
  private static Boolean myWorkspaceOldFashioned = null;
  private static boolean myWorkspaceExplicit = false;
  private static Properties myProperties = null;
  private static Boolean myRunningUnitTest = null;

  private Setup() {
  }

  public static void cleanupForTestCase() {
    //noinspection AssertWithSideEffects
    assert isRunningJUnit();
    if (!isRunningJUnit())
      return;
    myHomeDir = null;
    myProperties = null;
    myWorkspaceDir = null;
    myWorkspaceOldFashioned = null;
  }

  public static File createDir(File file) {
    if (!file.exists())
      //noinspection ResultOfMethodCallIgnored
      file.mkdirs();
    return file.isDirectory() ? file : null;
  }

  public static long getApplicationStartTime() {
    return myLoadedTime;
  }

  public static boolean getBooleanProperty(String propertyName) {
    String v = getStringProperty(propertyName);
    return "true".equalsIgnoreCase(v);
  }

  public static File getHomeDir() {
    File homeDir = myHomeDir;
    if (homeDir == null) {
      // kludge or robustness?
      assert false;
      return new File(".");
    }
    return homeDir;
  }

  @NotNull
  public static File getWorkspaceDir() {
    File workspaceDir = myWorkspaceDir;
    if (workspaceDir == null) {
      assert false;
      return new File(getHomeDir(), Setup.DIR_WORKSPACE_OLD_FASHIONED);
    }
    return workspaceDir;
  }

  public static boolean isWorkspaceOldfashioned() {
    Boolean oldFashioned = myWorkspaceOldFashioned;
    if (oldFashioned == null) {
      assert false;
      return false;
    }
    return oldFashioned;
  }

  private synchronized static Properties getProperties() {
    if(myProperties != null) {
      applyOverriding();
      return myProperties;
    }

    myProperties = PropertyUtils.loadProperties(Setup.class, BUNDLED_PROPERTIES);

    // Merge from system properties
    myProperties.putAll(System.getProperties());
    applyOverriding();
    return myProperties;
  }

  private static void applyOverriding() {
    for(final Overrider o : myOverriders) {
      o.apply();
    }
  }

  public static String getStringProperty(String propertyName, String defaultValue) {
    String value = getStringProperty(propertyName);
    return value == null ? defaultValue : value;
  }

  public static String getStringProperty(String propertyName) {
    Properties properties = getProperties();
    String result = properties.getProperty(propertyName);
    if (result == null) {
      // initially there were deskzilla.* properties, but would be strange on jira client
      // so allow to specify jiraclient.* or almworks.* and translate to deskzilla.* here
      String prefix = "deskzilla.";
      if (propertyName.startsWith(prefix) && prefix.length() < propertyName.length()) {
        String suffix = propertyName.substring(prefix.length());
        result = properties.getProperty("almworks." + suffix);
        if (result == null) {
          result = properties.getProperty("jiraclient." + suffix);
        }
      }
    }
    return result;
  }

  public static Collection<String> getPropertyKeys() {
    return getProperties().stringPropertyNames().stream().filter(Setup::isApplicationProperty).collect(Collectors.toList());
  }

  private static boolean isApplicationProperty(String key) {
    for(final String prefix : SYSTEM_PROPERTY_PREFIXES) {
      if(key.startsWith(prefix)) {
        return false;
      }
    }
    return true;
  }

  public static void changeProperties(final Map<String, String> diff) {
    PropertyUtils.changeProperties(getProperties(), diff);

    if(myWorkspaceDir == null) {
      logger().warning("Too early to save: " + diff);
    }

    final File file = new File(myWorkspaceDir, getPropertyFileName());
    new Thread() {
      @Override
      public void run() {
        PropertyUtils.saveProperties(diff, file, null);
      }
    }.start();
  }

  private static Logger logger() {
    return Logger.getLogger(Setup.class.getName());
  }

  public static Preferences getUserPreferences() {
    Preferences p = Preferences.userRoot();
    p = p.node("com/almworks/" + myProductId);
    return p;
  }

  public static boolean isDebugging() {
    return getBooleanProperty("is.debugging");
  }

  public static boolean isRunFromIDE() {
    return getBooleanProperty("from.ide");
  }

  public static boolean isRunningJUnit() {
    if (myRunningUnitTest == null) {
      StackTraceElement[] stackTrace = new Throwable().getStackTrace();
      for (StackTraceElement aStackTrace : stackTrace) {
        String s = aStackTrace.getClassName();
        if (s != null && s.toUpperCase(Locale.US).contains("JUNIT")) {
          myRunningUnitTest = Boolean.TRUE;
          break;
        }
      }
      if (myRunningUnitTest == null)
        myRunningUnitTest = Boolean.FALSE;
    }
    return myRunningUnitTest;
  }

  public static void setHomeDir(File homeDir) {
    if (myHomeDir != null) {
      assert false : myHomeDir;
      return;
    }
    try {
      myHomeDir = homeDir.getCanonicalFile();
      assert myHomeDir.isDirectory();
    } catch (IOException e) {
      assert false : e;
    }
  }

  public static void setWorkspaceDir(File workspace, boolean oldFashioned) {
    logger().info("Workspace directory: '" + workspace + "' oldFashioned:" + oldFashioned);
    if (myWorkspaceDir != null) {
      assert false : myWorkspaceDir;
      return;
    }
    if (myWorkspaceOldFashioned != null) {
      assert false : myWorkspaceOldFashioned;
      return;
    }
    try {
      myWorkspaceDir = workspace.getCanonicalFile();
      myWorkspaceOldFashioned = oldFashioned;
    } catch (IOException e) {
      assert false : e;
    }
  }

  public static void setWorkspaceExplicit(boolean explicit) {
    myWorkspaceExplicit = explicit;
  }

  public static boolean isWorkspaceExplicit() {
    return myWorkspaceExplicit;
  }

  public static FileHandler getLoggingFileHandler() throws IOException {
    return getProductionHandler(getLogDir());
  }

  public static File getLogDir() {
    if (isWorkspaceOldfashioned()) {
      return new File(getHomeDir(), DIR_LOGS);
    } else {
      return new File(getWorkspaceDir(), DIR_LOGS);
    }
  }

  public static FileHandler getDiagnosticsHandler(File diagDir) throws IOException {
    if(createDir(diagDir) == null) {
      throw new IOException("cannot create log directory " + diagDir);
    }
    return new FileHandler(new File(diagDir, LOG_FILE_PATTERN).getPath(), true);
  }

  public static FileHandler getProductionHandler(File diagDir) throws IOException {
    if(createDir(diagDir) == null) {
      throw new IOException("cannot create log directory " + diagDir);
    }
    return new FileHandler(new File(diagDir, LOG_FILE_PATTERN).getPath(), LOG_FILE_LIMIT, LOG_FILE_COUNT, true);
  }

  public static void workspaceMigrated(File workspace) {
    if (myWorkspaceDir == null) {
      assert false : workspace;
      return;
    }
    if (myWorkspaceOldFashioned != Boolean.TRUE) {
      assert false : myWorkspaceOldFashioned;
      return;
    }
    try {
      myWorkspaceDir = workspace.getCanonicalFile();
      myWorkspaceOldFashioned = false;
    } catch (IOException e) {
      assert false : e;
    }
  }

  public static String getWorkspaceDirectoryInUserHome() {
    return isJiraClient() ? ".JIRAClient" : ".Deskzilla";
  }

  public static boolean isJiraClient() {
    return SETUP_JIRACLIENT_PRODUCT_ID.equalsIgnoreCase(myProductId);
  }

  public static String getProductName() {
    return myProductName;
  }

  public static String getProductId() {
    return myProductId;
  }

  public static String getUrlProduct() {
    return "http://almworks.com/" + getProductId();
  }

  public static String getUrlUserManual() {
    return "http://almworks.com/" + getProductId() + "/manual";
  }

  public static String getUrlPurchase() {
    return "http://almworks.com/" + getProductId() + "/purchase";
  }

  public static String getUrlVersionInfo() {
    return "http://almworks.com/" + getProductId() + "/versioninfo.xml";
  }

  public static String getUrlDownload() {
    return "http://almworks.com/" + getProductId() + "/download";
  }

  public static String getUrlEapDownload() {
    return "http://almworks.com/" + getProductId() + "/eapdownload.html";
  }

  public static String getUrlRfq() {
    return "http://almworks.com/rfq.html?product=" + getProductId();
  }
}
