package com.almworks.jira.connector2;

import com.almworks.api.connector.http.HttpDumper;
import com.almworks.platform.DiagnosticRecorder;
import com.almworks.util.Env;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Log;

import java.io.File;
import java.util.List;

// todo merge with BugzillaEnv


public class JiraEnv {
  public static final LocalizedAccessor I18N_LOCAL = CurrentLocale.createAccessor(JiraEnv.class.getClassLoader(), "com/almworks/jira/JiraExceptions");
  public static final String ENV_JIRA_DUMP = "jira.dump";
  public static final String DUMP_ERRORS = "errors";
  public static final String DUMP_ALL = "all";
  public static final String JIRA_LOG_SUBDIR = "jira";
  public static final String JIRA_UPDATE_TOLERANCE = "jira.update.tolerance";
  public static final String JIRA_DEBUG_UPDATE_TIME = "jira.debug.update.time";

  private static File myGlobalLogs = null;
  private static volatile DiagnosticRecorder diagnosticRecorder;

  public static void setDiagnosticRecorder(DiagnosticRecorder recorder) {
    diagnosticRecorder = recorder;
  }
  
  public static List<HttpDumper.DumpSpec> getHttpDumperSpecs() {
    HttpDumper.DumpLevel envLevel = getDumpLevel();
    File envLogDir = getLogDir();
    DiagnosticRecorder recorder = diagnosticRecorder;
    File diagDir = recorder != null ? recorder.getSessionDir() : null;
    HttpDumper.DumpSpec diagSpec = diagDir != null
      ? new HttpDumper.DumpSpec(HttpDumper.DumpLevel.ALL, diagDir) : null;

    HttpDumper.DumpSpec envSpec = envLevel != HttpDumper.DumpLevel.NONE
      ? new HttpDumper.DumpSpec(envLevel, envLogDir) : null;

    return HttpDumper.DumpSpec.listOfTwo(diagSpec, envSpec);
  }

  private static HttpDumper.DumpLevel getDumpLevel() {
    String env = Env.getString(ENV_JIRA_DUMP);
    if (DUMP_ALL.equalsIgnoreCase(env))
      return HttpDumper.DumpLevel.ALL;
    else if (DUMP_ERRORS.equalsIgnoreCase(env))
      return HttpDumper.DumpLevel.ERRORS;
    else
      return HttpDumper.DumpLevel.NONE;
  }

  private static File getLogDir() {
    if (myGlobalLogs == null)
      return null;
    File dir = new File(myGlobalLogs, JIRA_LOG_SUBDIR);
    return dir;
  }

  public static void installGlobalLogsDir(File globalLogs) {
    assert myGlobalLogs == null;
    myGlobalLogs = globalLogs;
  }

  public static void cleanUpForTestCase() {
    myGlobalLogs = null;
  }

  static {
    String env = Env.getString(ENV_JIRA_DUMP);
    if (env != null)
      Log.debug(ENV_JIRA_DUMP + " = " + env);
  }
}
