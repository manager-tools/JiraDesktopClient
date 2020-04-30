package com.almworks.platform;

import com.almworks.api.install.Setup;
import com.almworks.util.DecentFormatter;
import com.almworks.util.commons.Procedure;
import com.almworks.util.properties.Role;
import org.almworks.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiagnosticRecorder {
  public static final Role<DiagnosticRecorder> ROLE = Role.role(DiagnosticRecorder.class);
  private static final SimpleDateFormat DIAG_DIR = new SimpleDateFormat("yyyyMMdd-HHmm");

  private final Logger myInternalLogger;
  private final List<Procedure<? super Logger>> myAdditionalInfo = new CopyOnWriteArrayList<Procedure<? super Logger>>();

  private File mySessionDir;
  private FileHandler mySessionHandler;

  public DiagnosticRecorder() {
    myInternalLogger = Logger.getLogger("com.almworks.diagnostics");
    myInternalLogger.setLevel(Level.ALL);
    myInternalLogger.setUseParentHandlers(false);
  }

  private static synchronized File getNextDiagnosticsDir() throws IOException {
    final String prefix = "diag-" + DIAG_DIR.format(new Date()) + "-";
    final Pattern pattern = Pattern.compile(prefix + "(\\d+)");

    final File logDir = Setup.getLogDir();

    File[] existingLogFiles = logDir.listFiles();
    if (existingLogFiles == null) {
      throw new IOException("cannot list log directory contents " + logDir);
    }

    int lastIndex = -1;
    for(final File f : existingLogFiles) {
      final Matcher m = pattern.matcher(f.getName());
      if(m.matches()) {
        lastIndex = Math.max(lastIndex, Integer.parseInt(m.group(1)));
      }
    }

    final File diagDir = new File(logDir, prefix + (lastIndex + 1));
    if(Setup.createDir(diagDir) == null) {
      throw new IOException("cannot create log directory " + diagDir);
    }

    return diagDir;
  }

  public synchronized void startSession() {
    if(mySessionDir != null) {
      return;
    }

    try {
      mySessionDir = getNextDiagnosticsDir();
      mySessionHandler = Setup.getDiagnosticsHandler(mySessionDir);
    } catch(IOException e) {
      Log.error(e);
      mySessionDir = null;
      mySessionHandler = null;
      return;
    }

    mySessionHandler.setLevel(Level.FINE);
    mySessionHandler.setFormatter(new DecentFormatter());
    Log.getRootLogger().addHandler(mySessionHandler);
    myInternalLogger.addHandler(mySessionHandler);

    logSessionStart();
  }

  public void addSessionInfo(Procedure<? super Logger> logProcedure) {
    if (logProcedure != null) {
      logProcedure.invoke(Log.getApplicationLogger());
      myAdditionalInfo.add(logProcedure);
    }
  }

  private void logSessionStart() {
    myInternalLogger.info("Started diagnostic session " + mySessionDir.getName());
    for (Procedure<? super Logger> procedure : myAdditionalInfo) procedure.invoke(myInternalLogger);
  }

  public synchronized File stopSession() {
    if(mySessionDir == null) {
      return null;
    }

    final File sessionDir = mySessionDir;
    mySessionDir = null;

    myInternalLogger.info("Stopped diagnostic session " + sessionDir.getName());
    Log.getRootLogger().removeHandler(mySessionHandler);
    myInternalLogger.removeHandler(mySessionHandler);
    mySessionHandler.close();
    mySessionHandler = null;

    return sessionDir;
  }

  public boolean isRecording() {
    return mySessionDir != null;
  }

  public File getSessionDir() {
    return mySessionDir;
  }
}
