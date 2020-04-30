package com.almworks.util;

import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

public class Env {
  private static final int OS_WINDOWS = 1;
  private static final int OS_MAC = 2;
  private static final int OS_LINUX = 3;
  private static final int OS_OTHER = 4;

  private static final Env ourInstance = new Env();

  private volatile EnvImpl myImpl = null;
  private final EnvImpl myDefaultEnv = new EnvDefaultImpl();

  private int myOS;

  // Mac version number
  private int myMacMajor;
  private int myMacMinor;

  private Env() {}

  public static boolean isRunFromIDE() {
    return getBoolean(GlobalProperties.RUN_FROM_IDE, false);
  }

  public static boolean isDebugging() {
    return getBoolean(GlobalProperties.IS_DEBUGGING, false);
  }

  public static boolean getBoolean(@NotNull String property) {
    return getBoolean(property, false);
  }

  public static boolean getBoolean(@NotNull String property, boolean defaultValue) {
    String value = getString(property, null);
    if (value == null)
      return defaultValue;
    else
      return "true".equalsIgnoreCase(value);
  }

  @Nullable
  public static String getString(@NotNull String property) {
    return getString(property, null);
  }

  @Nullable
  public static String getString(@NotNull String property, @Nullable String defaultValue) {
    EnvImpl impl = getImpl();
    String value = impl.getProperty(property);
    if (value == null && impl != ourInstance.myDefaultEnv) {
      value = ourInstance.myDefaultEnv.getProperty(property);
    }
    return value == null ? defaultValue : value;
  }

  @NotNull
  private static EnvImpl getImpl() {
    EnvImpl impl = ourInstance.myImpl;
    return impl != null ? impl : ourInstance.myDefaultEnv;
  }

  private int getOS() {
    if (myOS == 0) {
      String osname = Util.NN(getString("os.name"));
      String upper = Util.upper(osname);
      if (upper.indexOf("WINDOWS") >= 0) {
        myOS = OS_WINDOWS;
      } else if (upper.indexOf("MAC OS X") >= 0) {
        myOS = OS_MAC;
      } else if (upper.indexOf("MAC") >= 0) {
        Log.warn("is it Mac OS, not Mac OS X?");
        myOS = OS_MAC;
      } else if (upper.indexOf("LINUX") >= 0) {
        myOS = OS_LINUX;
      } else {
        Log.warn("probably unsupported operating system: " + osname);
        myOS = OS_OTHER;
      }
    }
    return myOS;
  }

  private int getMacMajor() {
    loadMacVersions();
    return myMacMajor;
  }

  private int getMacMinor() {
    loadMacVersions();
    return myMacMinor;
  }

  private void loadMacVersions() {
    if (myMacMajor == 0) {
      myMacMajor = -1;
      myMacMinor = -1;
      if (getOS() == OS_MAC) {
        int[] versions = getMajorMinor("os.version");
        if (versions != null && versions[0] != 0) {
          myMacMajor = versions[0];
          myMacMinor = versions[1];
        }
      }
    }
  }

  public static boolean isWindows() {
    return ourInstance.getOS() == OS_WINDOWS;
  }

  public static boolean isMac() {
    return ourInstance.getOS() == OS_MAC;
  }

  public static boolean isMacLeopardOrNewer() {
    return isMacVersionOrNewer(10, 5);
  }

  public static boolean isMacSnowLeopardOrNewer() {
    return isMacVersionOrNewer(10, 6);
  }

  public static boolean isMacLionOrNewer() {
    return isMacVersionOrNewer(10, 7);
  }

  private static boolean isMacVersionOrNewer(int minMajor, int minMinor) {
    final int major = ourInstance.getMacMajor();
    if(major < 0) {
      return false;
    }
    final int minor = ourInstance.getMacMinor();
    return (major == minMajor && minor >= minMinor) || (major > minMajor);
  }

  public static boolean isLinux() {
    int os = ourInstance.getOS();
    return os == OS_LINUX || os == OS_OTHER;
  }

  /**
   * Is needed to work around some bugs, i.e. bad behaviour of JFileChooser with A:
   * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4089199
   */
  public static boolean isWindows2000orEarlier() {
    if (!isWindows())
      return false;
    int version[] = getMajorMinor("os.version");
    if (version == null || version.length != 2)
      return true;
    int majorNumber = version[0];
    int minorNumber = version[1];
    // Windows 2000 is version 5.0. Windows XP is version 5.1.
    return (majorNumber < 5) || (majorNumber == 5 && minorNumber == 0);
  }

  @Nullable
  private static int[] getMajorMinor(String prop) {
    String osVersion = getString(prop);
    if (osVersion == null) {
      // return safer value if not known
      return null;
    }
    int k1 = osVersion.indexOf('.');
    int k2 = k1 >= 0 ? osVersion.indexOf('.', k1 + 1) : -1;
    String major = k1 > 0 ? osVersion.substring(0, k1) : osVersion;
    String minor = k1 > 0 ? (k2 > 0 ? osVersion.substring(k1 + 1, k2) : osVersion.substring(k1 + 1)) : null;
    int majorNumber;
    int minorNumber;
    try {
      majorNumber = Integer.parseInt(major.trim());
      minorNumber = minor == null ? 0 : Integer.parseInt(minor.trim());
    } catch (NumberFormatException e) {
      // strange
      return null;
    }
    return new int[] {majorNumber, minorNumber};
  }

  public static boolean isWindowsVistaOrLater() {
    int[] version = getMajorMinor("os.version");
    if (version == null || version.length != 2)
      return true;
    return version[0] >= 6;
  }

  public static void setImpl(EnvImpl impl) {
    ourInstance.myImpl = impl;
    ourInstance.myOS = ourInstance.myMacMajor = ourInstance.myMacMinor = 0;
  }

  public static int getInteger(@NotNull String property, int defaultValue) {
    return getInteger(property, Integer.MIN_VALUE, Integer.MAX_VALUE, defaultValue);
  }

  public static int getInteger(@NotNull String property, int min, int max, int defaultValue) {
    String string = getString(property);
    if (string == null)
      return defaultValue;
    try {
      int value = Integer.parseInt(string);
      if (value < min) {
        Log.warn("value " + property + "=" + value + " is too low, setting to " + min);
        return min;
      }
      if (value > max) {
        Log.warn("value " + property + "=" + value + " is too high, setting to " + max);
        return max;
      }
      return value;
    } catch (NumberFormatException e) {
      Log.warn("cannot parse " + property + "=" + string);
      return defaultValue;
    }
  }

  public static String getUserHome() {
    return getString("user.home");
  }

  public static int getJavaSpecificationVersion() {
    String version = getString("java.specification.version");
    if (version == null) {
      Log.warn("No Java version");
      return -1;
    }
    if (version.length() < 3 || version.charAt(1) != '.' || version.length() > 3 || version.charAt(0) != '1' ||
      !isDigit(version.charAt(2)))
    {
      Log.warn("Wrong Java version format: " + version);
      return -1;
    }
    return getDigit(version.charAt(2));
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static int getDigit(char c) {
    assert isDigit(c) : c;
    return c - '0';
  }

  public static Map<String, String> getProperties() {
    final Map<String, String> props = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

    final EnvImpl impl = getImpl();
    for(final String key : impl.getPropertyKeys()) {
      props.put(key, impl.getProperty(key));
    }

    return props;
  }

  public static void changeProperties(Map<String, String> diff) {
    EnvImpl impl = ourInstance.myImpl;
    if (impl == null) {
      LogHelper.error("Changed properties not persisted");
      impl = ourInstance.myDefaultEnv;
    }
    impl.changeProperties(diff);
  }
}