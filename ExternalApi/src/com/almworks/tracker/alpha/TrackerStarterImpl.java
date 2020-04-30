package com.almworks.tracker.alpha;

import com.almworks.dup.util.ApiLog;
import com.almworks.dup.util.ValueModel;
import com.almworks.tracker.eapi.alpha.TrackerApplication;
import com.almworks.tracker.eapi.alpha.TrackerConnectionStatus;
import com.almworks.tracker.eapi.alpha.TrackerDiscoveryHandler;
import com.almworks.tracker.eapi.alpha.TrackerStarter;
import com.almworks.util.xmlrpc.EndPoint;
import org.almworks.util.Util;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TrackerStarterImpl implements TrackerStarter {
  // constants copied from WorkAreaImpl and WorkAreaLocker
  private static final String TRACKER_LOCK_FILE = ".lock";
  private static final int SAFE_BYTES = 900;
  private static final long LOCK_PLACE = SAFE_BYTES;
  private static final long LOCK_SIZE = 21;

  private static final boolean IS_MAC_OS_X = Util.upper(System.getProperty("os.name", "")).contains("MAC OS X");

  private final ValueModel<TrackerConnectionStatus> myConnectionStatus;
  private final EndPoint myEndPoint;
  private final TrackerPinger myPinger;
  private final Timer myTimer;
  public static final int CONNECT_WATCHER_PERIOD = 3000;

  private int myExplicitPort = 0;
  private File myExplicitWorkspace = null;
  private TrackerApplication myDiscoveryApplication = null;
  private TrackerDiscoveryHandler myDiscoveryHandler = null;

  private boolean myStarted = false;
  public static final Pattern PORT_PATTERN = Pattern.compile(".*api\\:\\:(\\d+).*", Pattern.DOTALL);




  public TrackerStarterImpl(EndPoint endPoint, TrackerPinger pinger,
    ValueModel<TrackerConnectionStatus> connectionStatus)
  {
    myEndPoint = endPoint;
    myPinger = pinger;
    myConnectionStatus = connectionStatus;
    myTimer = new Timer(CONNECT_WATCHER_PERIOD, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        verifyConnection(false);
      }
    });
  }

  public ValueModel<TrackerConnectionStatus> getConnectionStatus() {
    return myConnectionStatus;
  }

  public Process startTracker(TrackerApplication application) throws IOException {
    return startTracker(application, null);
  }

  public Process startTracker(TrackerApplication application, File workspaceDirectory) throws IOException {
    final String applicationId = application.getApplicationId();
    final File workingDirectory;
    String[] exec = build(System.getProperty(applicationId + AlphaProtocol.PROPERTY_EXEC_SUFFIX), workspaceDirectory);

    if(exec != null) {
      workingDirectory = getWorkingDirectory(System.getProperty(applicationId + AlphaProtocol.PROPERTY_EXEC_PATH_SUFFIX));
    } else {
      String suffix = System.getProperty(AlphaProtocol.PROPERTY_USE_PREFIX + applicationId, "");
      suffix = "debug".equalsIgnoreCase(suffix) ? ".debug" : "";
      Preferences node = Preferences.userRoot().node(AlphaProtocol.PREFERENCES_PATH);
      exec = build(node.get(applicationId + suffix, null), workspaceDirectory);
      if (exec != null) {
        workingDirectory =
          getWorkingDirectory(node.get(applicationId + AlphaProtocol.START_DIRECTORY_SUFFIX + suffix, null));
      } else {
        try {
          node = Preferences.systemRoot().node(AlphaProtocol.PREFERENCES_PATH);
        } catch (Exception e) {
          node = null;
        }
        if (node != null) {
          exec = build(node.get(applicationId + suffix, null), workspaceDirectory);
          workingDirectory =
            getWorkingDirectory(node.get(applicationId + AlphaProtocol.START_DIRECTORY_SUFFIX + suffix, null));
        } else {
          workingDirectory = null;
        }
      }
    }
    if (exec == null) {
      throw new IOException(applicationId + " is not found");
    }
    if (workingDirectory == null) {
      ApiLog.warn(applicationId + " working directory is not set");
    }
    Process process;
    try {
      process = Runtime.getRuntime().exec(exec, null, workingDirectory);
    } catch (SecurityException e) {
      throw new IOException(e.toString());
    } catch (IllegalArgumentException e) {
      throw new IOException(e.toString());
    }
    return process;
  }

  private File getWorkingDirectory(String value) {
    if (value == null || value.length() == 0)
      return null;
    File file = null;
    try {
      file = new File(value);
    } catch (Exception e) {
      return null;
    }
    if (file.isDirectory() && file.canRead())
      return file;
    else
      return null;
  }

  private String[] build(String execString, File workspace) {
    if(execString == null) {
      return null;
    }
    execString = execString.trim();
    if(execString.isEmpty()) {
      return null;
    }

    final List<String> result = AlphaImplUtils.tokenizeExec(execString);
    if(result.isEmpty()) {
      return null;
    }

    return IS_MAC_OS_X ? buildMacOSX(result, workspace) : buildDefault(result, workspace);
  }

  private String[] buildDefault(List<String> result, File workspace) {
    if(workspace != null && workspace.getParentFile() != null && workspace.getParentFile().isDirectory()) {
      final String workspacePath = workspace.getAbsolutePath();
      // see if there's already a workspace argument
      int candidate = -1;
      boolean replaced = false;
      for(int k = result.size() - 1; k >= 0; k--) {
        final String s = result.get(k);
        if(s.startsWith("com.almworks.")) {
          // this should be the main class
          if(candidate >= 0) {
            result.set(candidate, workspacePath);
            replaced = true;
          }
          break;
        } else if(!s.startsWith("-")) {
          candidate = k;
        }
      }
      if(!replaced) {
        result.add(workspacePath);
      }
    }
    return result.toArray(new String[result.size()]);
  }

  private String[] buildMacOSX(List<String> result, File workspace) {
    final Pattern appRegexp = Pattern.compile("/.+?\\.app/");

    String appPath = null;
    for(final String s : result) {
      if(s.startsWith("/")) {
        final Matcher m = appRegexp.matcher(s);
        if(m.find()) {
          appPath = s.substring(0, m.end() - 1);
          break;
        }
      }
    }

    if(appPath == null) {
      return null;
    }

    final File appFile = new File(appPath);
    if(!appFile.isDirectory()) {
      return null;
    }

    if(workspace != null && workspace.isDirectory()) {
      return new String[] { "open", appFile.getAbsolutePath(), "--args", workspace.getAbsolutePath() };
    }
    return new String[] { "open" , appFile.getAbsolutePath() };
  }

  public void tryConnect() {
    myPinger.pingNow();
  }

  public void useTrackerDiscovery(TrackerApplication application, TrackerDiscoveryHandler handler) {
    myDiscoveryHandler = handler;
    myDiscoveryApplication = application;
    myExplicitWorkspace = null;
    myExplicitPort = 0;
    if (myConnectionStatus.getValue().isConnected()) {
      reconnect();
    } else {
      verifyConnection(true);
    }
  }

  public void useTrackerPort(int port) {
    if (port < 0 || port > 65535)
      throw new IllegalArgumentException(String.valueOf(port));
    myExplicitPort = port;
    myExplicitWorkspace = null;
    myDiscoveryHandler = null;
    verifyConnection(true);
  }

  public void useTrackerWorkspace(File workspaceDirectory) {
    if (workspaceDirectory == null)
      throw new NullPointerException();
    myExplicitWorkspace = workspaceDirectory;
    myExplicitPort = 0;
    myDiscoveryHandler = null;
    verifyConnection(true);
  }

  // awt
  public void start() {
    if (myStarted)
      return;
    myStarted = true;
    myTimer.start();
  }

  // awt
  public void stop() {
    if (!myStarted)
      return;
    myStarted = false;
    myTimer.stop();
  }

  private void verifyConnection(boolean checkConnected) {
    if (!myStarted) {
      return;
    }

    TrackerConnectionStatus connectionStatus = myConnectionStatus.getValue();
    if (connectionStatus.isConnected()) {
      if (checkConnected) {
        checkConnected(connectionStatus);
      }
      return;
    }

    if (myExplicitPort > 0) {
      myEndPoint.setOutboxPort(myExplicitPort);
    } else if (myExplicitWorkspace != null) {
      int port = getPortFromWorkspace(myExplicitWorkspace);
      if (port > 0)
        myEndPoint.setOutboxPort(port);
    } else {
      runDiscovery();
    }
  }

  private void checkConnected(TrackerConnectionStatus connectionStatus) {
    if (myExplicitPort > 0) {
      int usedPort = myEndPoint.getOutbox().getPort();
      if (usedPort != myExplicitPort) {
        reconnect();
      }
    } else if (myExplicitWorkspace != null) {
      String usedDirectory = connectionStatus.getWorkspaceDirectory();
      try {
        File f1 = new File(usedDirectory).getCanonicalFile();
        File f2 = myExplicitWorkspace.getCanonicalFile();
        if (f1 != null && !f1.equals(f2)) {
          reconnect();
        }
      } catch (IOException e) {
        // what to do ? ignore error
      }
    }
  }

  public void reconnect() {
    disconnect();
    // connection will be re-tried by timer
  }

  public void disconnect() {
    myEndPoint.setOutboxPort(0);
    myEndPoint.changeInboxPort();
    myPinger.forceDisconnect();
  }

  private int getPortFromWorkspace(File workspace) {
    File lockFile = new File(workspace, TRACKER_LOCK_FILE);
    if (!lockFile.isFile())
      return 0;
    String content = getLockFileContent(lockFile);
    if (content == null)
      return 0;
    int port = getPort(content);
    if (port == 0)
      return 0;
    // try to lock to see if Deskzilla is running
    if (!isLocked(lockFile))
      return 0;
    return port;
  }

  private boolean isLocked(File lockFile) {
    RandomAccessFile rfile = null;
    try {
      try {
        rfile = new RandomAccessFile(lockFile, "rw");
      } catch (IOException e) {
        return true;
      } catch (SecurityException e) {
        // let it fall through and exit
        // ignore
        return true;
      }
      FileChannel channel = rfile.getChannel();
      FileLock lock = null;
      try {
        lock = channel.tryLock();
        return lock == null;
      } catch (IOException e) {
        return true;
      } finally {
        if (lock != null) {
          try {
            lock.release();
          } catch (IOException e) {
            // ignore
          }
        }
      }
    } finally {
      if (rfile != null) {
        try {
          rfile.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }

  private int getPort(String lockContent) {
    Matcher matcher = PORT_PATTERN.matcher(lockContent);
    if (!matcher.matches()) {
      return 0;
    }
    String portString = matcher.group(1);
    int port;
    try {
      port = Integer.parseInt(portString);
    } catch (NumberFormatException e) {
      return 0;
    }
    if (port < 1024 || port > 65535) {
      return 0;
    }
    return port;
  }

  /**
   * Copied from WorkAreaLocker
   */
  private String getLockFileContent(File lockFile) {
    FileInputStream stream = null;
    DataInputStream in = null;
    try {
      stream = new FileInputStream(lockFile);
      in = new DataInputStream(stream);
      byte[] bytes = new byte[SAFE_BYTES];
      in.readFully(bytes);
      return new String(bytes).trim();
    } catch (IOException e) {
      return null;
    } finally {
      try {
        if (in != null)
          in.close();
      } catch (IOException e) {
        //ignore
      }
      try {
        if (stream != null)
          stream.close();
      } catch (IOException e) {
        //ignore
      }
    }
  }

  /**
   * partly copied from WorkAreaImpl.checkStaleAnnouncedWorkAreas
   */
  private void runDiscovery() {
    TrackerApplication discoveryApplication = myDiscoveryApplication;
    if (discoveryApplication == null) {
      return;
    }
    String nodePath = "com/almworks/" + discoveryApplication.getApplicationId();
    Preferences root = Preferences.userRoot();
    try {
      if (!root.nodeExists(nodePath)) {
        return;
      }
    } catch (BackingStoreException e) {
      return;
    }
    Preferences p = root.node(nodePath);
    Preferences instances = p.node("instances");
    Set<File> workspaceCandidates = new HashSet<File>();
    try {
      String[] keys = instances.keys();
      if (keys != null) {
        for (String key : keys) {
          String w = instances.get(key, null);
          if (w != null) {
            try {
              File file = new File(w).getCanonicalFile();
              workspaceCandidates.add(file);
            } catch (IOException e) {
              // ignore file
            }
          }
        }
      }
    } catch (BackingStoreException e) {
      // ?
      return;
    }
    for (Iterator<File> ii = workspaceCandidates.iterator(); ii.hasNext();) {
      File file = ii.next();
      int port = getPortFromWorkspace(file);
      if (port == 0)
        ii.remove();
    }
    int count = workspaceCandidates.size();
    if (count > 0) {
      TrackerDiscoveryHandler handler = myDiscoveryHandler;
      if (count == 1 || handler == null) {
        useTrackerWorkspace(workspaceCandidates.iterator().next());
      } else {
        handler.onTrackerSelectionRequired(new ArrayList<File>(workspaceCandidates), this);
      }
    }
  }
}
