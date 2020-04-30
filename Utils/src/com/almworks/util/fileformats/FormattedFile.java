package com.almworks.util.fileformats;

import org.almworks.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public abstract class FormattedFile {
  protected final File myPath;
  protected final boolean myDoLocking;

  protected RandomAccessFile myFile;
  protected FileChannel myChannel;
  protected FileLock myFileLock;
  protected boolean myOpen;
  protected boolean myForceDiskWriteEachTime = false;

  public FormattedFile(File path, boolean doLocking) {
    if (path == null)
      throw new NullPointerException("path");
    myPath = path;
    myDoLocking = doLocking;
    myOpen = false;
  }

  protected void doOpen() throws IOException, FileFormatException {
  }

  protected void doClose() {
  }

  protected void doCreate() throws IOException {
  }

  protected void doDrop() {
  }

  protected synchronized void open() throws IOException, FileFormatException {
    if (isOpen())
      return;
    if (!myPath.exists())
      throw new IOException(this + " does not exist");
    if (!myPath.isFile())
      throw new IOException(this + " is not a file");
    if (!myPath.canRead())
      throw new IOException("cannot read " + this);
    boolean success = false;
    try {
      prepareFile(myDoLocking);
      myChannel.position(0);
      doOpen();
      myOpen = true;
      success = true;
    } finally {
      if (!success)
        cleanUp();
    }
  }

  protected synchronized void close() {
    if (isOpen()) {
      doClose();
    }
    cleanUp();
  }

  protected synchronized void create() throws IOException {
    checkNotOpen();
    if (myPath.exists())
      throw new IOException(this + " exists");
    boolean success = false;
    try {
      prepareFile(myDoLocking);
      myChannel.truncate(0);
      doCreate();
      if (myForceDiskWriteEachTime)
        myChannel.force(true);
      myOpen = true;
      success = true;
    } finally {
      if (!success) {
        myPath.delete();  // if it exists - we created it
        cleanUp();
      }
    }
  }

  protected synchronized void drop() throws InterruptedException, IOException {
    close();
    if (!myPath.exists())
      return;
    doDrop();
    // try several times, because deletion may fail to uncertain reasons
    boolean success = false;
    for (int attempt = 0; attempt < 5; attempt++) {
      success = myPath.delete();
      if (success)
        break;
      Thread.sleep(100);
    }
    if (!success)
      throw new IOException("cannot drop " + this);
  }

  public synchronized boolean isOpen() {
    return myOpen;
  }

  public synchronized void forceSync() throws IOException {
    checkOpen();
    if (myChannel != null)
      myChannel.force(true);
  }

  protected void checkNotOpen() {
    if (isOpen())
      throw new IllegalStateException(this + " is open");
  }

  protected void checkOpen() {
    if (!isOpen())
      throw new IllegalStateException(this + " is not open");
  }

  protected void cleanUp() {
    if (myFileLock != null) {
      try {
        myFileLock.release();
      } catch (IOException e) {
        Log.debug("error releasing lock for " + this, e);
      }
      myFileLock = null;
    }
    if (myChannel != null) {
      try {
        if (myForceDiskWriteEachTime)
          myChannel.force(true);
      } catch (IOException e) {
        Log.debug(e);
      }
      try {
        myChannel.close();
      } catch (IOException e) {
        Log.debug(e);
      }
      myChannel = null;
    }
    if (myFile != null) {
      try {
        myFile.close();
      } catch (IOException e) {
        Log.debug(e);
      }
      myFile = null;
    }
    myOpen = false;
  }

  private void prepareFile(boolean lock) throws IOException {
    myFile = new RandomAccessFile(myPath, "rw");
    myChannel = myFile.getChannel();
    if (lock) {
      myFileLock = myChannel.tryLock();
      if (myFileLock == null)
        throw new IOException("cannot lock data file");
    }
  }

  public File getPath() {
    return myPath;
  }

  public String toString() {
    return String.valueOf(myPath);
  }

  public void flush() {
    FileChannel channel = myChannel;
    if (channel != null) {
      try {
        channel.force(true);
      } catch (IOException e) {
        Log.debug(e);
      }
    }
  }
}
