package com.almworks.platform;

import com.almworks.api.install.Setup;
import com.almworks.api.install.TrackerProperties;
import com.almworks.util.Env;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Failure;
import org.almworks.util.Log;
import org.apache.xmlrpc.XmlRpcClient;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WorkAreaLocker {
  private static final int SAFE_BYTES = 900;
  private static final long LOCK_PLACE = SAFE_BYTES;
  private static final long LOCK_SIZE = 21;
  private static final String ALMWORKS_TO_FRONT = "almworks.toFront";
  private static final String ALMWORKS_OPEN_ARTIFACTS = "almworks.openArtifacts";

  private final File myLockFile;
  private final boolean myNoLocking;
  private final boolean myIgnoreFailedLocking;
  private final List<String> myUrlsToOpen;

  @Nullable private RandomAccessFile myFile;
  @Nullable private FileChannel myChannel;
  @Nullable private FileLock myLock;

  private int myApiPort = 0;

  public WorkAreaLocker(File lockFile, List<String> urlsToOpen) {
    myLockFile = lockFile;
    myUrlsToOpen = urlsToOpen;
    myNoLocking = Env.getBoolean(TrackerProperties.NO_LOCKING, false);
    myIgnoreFailedLocking = Env.getBoolean(TrackerProperties.IGNORE_LOCKING, false);
  }

  public void lock() throws ControlPassedException {
    if (myNoLocking)
      return;
    try {
      myFile = new RandomAccessFile(myLockFile, "rw");
    } catch (IOException e) {
      lockFailed();
      return;
    } catch (SecurityException e) {
      // let it fall through and exit
      throw e;
    }
    myChannel = myFile.getChannel();
    try {
      myLock = myChannel.tryLock(LOCK_PLACE, LOCK_SIZE, false);
    } catch (IOException e) {
      Log.warn("lock failed", e);
      myLock = null;
    }
    if (myLock == null)
      lockFailed();
    else
      lockSucceeded();
  }

  private void lockSucceeded() {
    writeLockFile();
  }

  private void writeLockFile() {
    FileLock lock = myLock;
    if (lock == null) {
      // no lock
      return;
    }
    FileChannel channel = myChannel;
    assert channel != null;
    assert channel.isOpen();
    assert lock.isValid();
    assert !lock.isShared();
    StringBuffer contents = new StringBuffer(createLockFileContents());
    while (contents.length() < SAFE_BYTES) {
      contents.append(' ');
    }
    while (contents.length() < SAFE_BYTES + LOCK_SIZE) {
      contents.append("LOCKED-");
    }
    byte[] bytes = contents.toString().getBytes();
    try {
      channel.position(0);
      int written = channel.write(ByteBuffer.wrap(bytes));
      if (written != bytes.length) {
        Log.warn("lock write imcomplete, " + written);
      }
      channel.force(false);
    } catch (IOException e) {
      Log.warn("cannot write lock file", e);
    }
  }

  private String createLockFileContents() {
    return
      "This is " + Setup.getProductName() + " lock file. You can delete it.\n" +
      "File written " + new Date() + ".\n" +
      (myApiPort == 0 ? "" : "api::" + myApiPort + "\n") +
      "pid::" + getMyPid();
  }

  private int getMyPid() {
    final Pattern p = Pattern.compile("^\\d+");
    final Matcher m = p.matcher(ManagementFactory.getRuntimeMXBean().getName());
    return m.find() ? Integer.parseInt(m.group()) : 0;
  }

  private void lockFailed() throws ControlPassedException {
    if (passControl()) {
      throw new ControlPassedException();
    } else {
      if (!myIgnoreFailedLocking)
        throw new Failure("cannot lock " + myLockFile);
    }
  }

  private boolean passControl() {
    String lockContent = readLockFile();
    if (lockContent == null) {
      Log.debug("cannot read lock file content");
      return false;
    }
    int port = getPort(lockContent);
    if (port == 0) {
      return false;
    }
    int pid = getPid(lockContent);
    return callTracker(port, pid);
  }

  private String readLockFile() {
    FileInputStream stream = null;
    DataInputStream in = null;
    try {
      stream = new FileInputStream(myLockFile);
      in = new DataInputStream(stream);
      byte[] bytes = new byte[SAFE_BYTES];
      in.readFully(bytes);
      return new String(bytes).trim();
    } catch (IOException e) {
      Log.debug(e);
      return null;
    } finally {
      IOUtils.closeStreamIgnoreExceptions(in);
      IOUtils.closeStreamIgnoreExceptions(stream);
    }
  }

  private int getPort(String lockContent) {
    Matcher matcher = Pattern.compile("api::(\\d+)").matcher(lockContent);
    if (!matcher.find()) {
      Log.debug("port is not announced");
      return 0;
    }
    String portString = matcher.group(1);
    int port;
    try {
      port = Integer.parseInt(portString);
    } catch (NumberFormatException e) {
      Log.debug("bad port " + portString);
      return 0;
    }
    if (port < 1024 || port > 65535) {
      Log.debug("bad port " + port);
      return 0;
    }
    return port;
  }

  private int getPid(String lockContent) {
    Matcher matcher = Pattern.compile("pid::(\\d+)").matcher(lockContent);
    if (!matcher.find()) {
      Log.debug("pid is not announced");
      return 0;
    }
    String pidString = matcher.group(1);
    int pid;
    try {
      pid = Integer.parseInt(pidString);
    } catch (NumberFormatException e) {
      Log.debug("bad pid " + pidString);
      return 0;
    }
    return pid;
  }

  public void announceApiPort(int port) {
    myApiPort = port;
    writeLockFile();
  }

  public void unlock() {
    FileLock lock = myLock;
    if (lock != null) {
      try {
        lock.release();
      } catch (IOException e) {
        // ignore
      }
      myLock = null;
    }
    FileChannel channel = myChannel;
    if (channel != null) {
      try {
        channel.close();
      } catch (IOException e) {
        // ignore
      }
      myChannel = null;
    }
    RandomAccessFile file = myFile;
    if (file != null) {
      try {
        file.close();
      } catch (IOException e) {
        // ignore
      }
      myFile = null;
    }
    if (!myNoLocking && myLockFile.isFile()) {
      try {
        myLockFile.delete();
      } catch (Exception e) {
        // ignore
      }
      try {
        myLockFile.deleteOnExit();
      } catch (Exception e) {
        // ignore
      }
    }
  }

  private boolean callTracker(final int port, int pid) {
    try {
      WinFocusStealing.allowFocusStealing(pid);
      XmlRpcClient client = new XmlRpcClient("127.0.0.1", port);
      if(myUrlsToOpen == null || myUrlsToOpen.isEmpty()) {
        client.execute(ALMWORKS_TO_FRONT, new Vector());
      } else {
        final Vector<Object> params = new Vector<Object>() {{
          add(port);
          addAll(myUrlsToOpen);
        }};
        client.execute(ALMWORKS_OPEN_ARTIFACTS, params);
      }
      return true;
    } catch (Exception e) {
      Log.warn(e);
      return false;
    }
  }
}
