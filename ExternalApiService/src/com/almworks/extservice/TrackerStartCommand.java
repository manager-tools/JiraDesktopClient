package com.almworks.extservice;

import com.almworks.api.install.Setup;
import com.almworks.api.install.TrackerProperties;
import com.almworks.launcher.Launcher;
import com.almworks.tracker.alpha.AlphaProtocol;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.LogHelper;
import com.almworks.util.files.FileUtil;
import org.almworks.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;

class TrackerStartCommand {
  private static final String[] WINDOWS_JAVAS = {"javaw.exe", "javaw", "java.exe", "java", "jre"};
  private static final String[] GENERIC_JAVAS = {"java", "jre", "javaw", "java.exe", "javaw.exe"};

  public static void store(boolean forceDebug) throws CannotStoreTrackerStarterException {
    String command = getLaunchCommand();
    String workingDirectory = getWorkingDirectory();
    boolean debug = forceDebug || Env.isDebugging() || Env.isRunFromIDE();
    if (debug) {
      storeLaunchCommandInDebugFile(command);
    }
    String suffix = debug ? ".debug" : "";
    Preferences node;
    node = Preferences.userRoot().node(AlphaProtocol.PREFERENCES_PATH);
    store(node, suffix, command, workingDirectory);
//  causes exception on unix
//    node = Preferences.systemRoot().node(AlphaProtocol.PREFERENCES_PATH);
//    store(node, suffix, command, workingDirectory);
  }

  private static void storeLaunchCommandInDebugFile(String command) {
    boolean windows = Env.isWindows();
    File dir = windows ? new File("C:\\") : new File(Env.getUserHome());
    if (dir.isDirectory()) {
      File file = windows ? new File(dir, Setup.getProductId() + "-last-run.bat") :
        new File(dir, Setup.getProductId() + "-last-run.sh");
      if (windows)
        command = adjustCommandForStoringInFileOnWindows(command);
      try {
        FileUtil.writeFile(file, command);
      } catch (IOException e) {
        // ignore
      }
    }
  }

  /**
   * When storing deskzilla-last-command.bat, remove redundant \\ from paths.
   */
  static String adjustCommandForStoringInFileOnWindows(String command) {
    int len = command.length();
    StringBuffer buf = new StringBuffer(len);
    boolean inEscape = false;
    boolean inQuote = false;
    for (int i = 0; i < len; i++) {
      char c = command.charAt(i);
      if (inQuote) {
        if (!inEscape && c == '\\' && i < len - 1 && command.charAt(i + 1) == '\\') {
          inEscape = true;
          continue;
        }
        if (!inEscape) {
          if (c == '"') {
            inQuote = false;
          } else if (c == '\\') {
            inEscape = true;
          }
        } else {
          inEscape = false;
        }
      } else {
        if (c == '"') {
          inQuote = true;
        }
      }
      buf.append(c);
    }
    return buf.toString();
  }

  private static void store(Preferences node, String suffix, String command, String workingDirectory) {
    String applicationId = Setup.getProductId();

    try {
      node.put(applicationId + suffix, command);
    } catch (IllegalArgumentException e) {
      if (Env.isDebugging()) LogHelper.debug(e.getMessage());
      else LogHelper.error(e);
    }
    if (workingDirectory != null)
      node.put(applicationId + AlphaProtocol.START_DIRECTORY_SUFFIX + suffix, workingDirectory);
  }


  private static String getWorkingDirectory() {
    try {
      return new File(".").getCanonicalPath();
    } catch (IOException e) {
      return null;
    }
  }

  private static String getLaunchCommand() throws CannotStoreTrackerStarterException {
    StringBuffer command = new StringBuffer();
    boolean isWindows = Util.lower(System.getProperty("os.name", "")).indexOf("windows") >= 0;
    File javaHome = getFile("java.home");
    File javaExe = getJavaExecutable(javaHome, isWindows);
    append(command, javaExe.getAbsolutePath());
    append(command, "-cp");
    append(command, getProperty("java.class.path"));
    append(command, getMxString());
    append(command, getAssertionStatus());
    appendProperties(command);
    appendProperty(command, TrackerProperties.HOME, Setup.getHomeDir().getAbsolutePath());
    append(command, Launcher.class.getName());
    append(command, Setup.getWorkspaceDir().getAbsolutePath());
    return command.toString();
  }

  private static void appendProperties(StringBuffer command) {
    Properties properties = System.getProperties();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String))
        continue;
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      if (TrackerProperties.HOME.equalsIgnoreCase(key)) {
        // added later
        continue;
      }
      boolean repeat = GlobalProperties.hasProperty(key) || TrackerProperties.hasProperty(key);
      if (!repeat) {
        // hacks :(
        String lower = Util.lower(key);
        repeat = key.indexOf("jiraclient") >= 0 || key.indexOf("deskzilla") >= 0 || key.indexOf("bugzilla") >= 0 ||
          key.indexOf("tracker") >= 0 || key.indexOf("almworks") >= 0;
      }
      if (repeat) {
        appendProperty(command, key, value);
      }
    }
  }

  private static void appendProperty(StringBuffer command, String key, String value) {
    append(command, "-D" + key + "=" + value);
  }

  private static void append(StringBuffer command, String element) {
    if (element == null || element.length() == 0)
      return;
    if (command.length() > 0 && command.charAt(command.length() - 1) != ' ')
      command.append(' ');
    if (element.indexOf(' ') >= 0) {
      // needs quotes
      int len = element.length();
      command.append('"');
      for (int i = 0; i < len; i++) {
        char c = element.charAt(i);
        if (c == '"' || c == '\\')
          command.append('\\');
        command.append(c);
      }
      command.append('"');
    } else {
      command.append(element);
    }
  }

  private static String getAssertionStatus() {
    return TrackerStartCommand.class.desiredAssertionStatus() ? "-ea" : "";
  }

  private static String getMxString() {
    long total = Runtime.getRuntime().maxMemory();
    int megs = FileUtil.getMemoryMegs(total);
    return "-Xmx" + megs + "m";
  }

  private static File getJavaExecutable(File javaHome, boolean windows) throws CannotStoreTrackerStarterException {
    File bin = new File(javaHome, "bin");
    if (!bin.isDirectory())
      throw new CannotStoreTrackerStarterException("bad java home " + javaHome);
    String[] candidates = windows ? WINDOWS_JAVAS : GENERIC_JAVAS;
    for (String candidate : candidates) {
      File java = new File(bin, candidate);
      if (java.isFile() && java.canRead())
        return java;
    }
    throw new CannotStoreTrackerStarterException("java not found in " + javaHome);
  }

  private static File getFile(String property) throws CannotStoreTrackerStarterException {
    String value = getProperty(property);
    try {
      return new File(value);
    } catch (Exception e) {
      throw new CannotStoreTrackerStarterException("bad location " + value);
    }
  }

  private static String getProperty(String property) throws CannotStoreTrackerStarterException {
    String value = System.getProperty(property);
    if (value == null)
      throw new CannotStoreTrackerStarterException("no " + property);
    return value;
  }
}
