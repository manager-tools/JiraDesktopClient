package com.almworks.platform;

import com.almworks.api.install.Setup;
import com.almworks.api.misc.WorkArea;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class WorkAreaImpl implements WorkArea {
  private static final String DATABASE_BACKUP_DIR_NAME = "db.backup";
  private static final String LOG_DIR_NAME = Setup.DIR_LOGS;
  private static final String ETC_DIR_NAME = Setup.DIR_ETC;
  private static final String COMPONENTS_DIR_NAME = Setup.DIR_COMPONENTS;

  private final List<String> myUrlsToOpen;
  private WorkAreaLocker myLocker;
  private String myWorkareaAnnouncementKey;

  public WorkAreaImpl(List<String> urlsToOpen) {
    myUrlsToOpen = urlsToOpen;
  }

  public WorkAreaImpl() {
    this(null);
  }

  @NotNull
  public final File getRootDir() {
//    File file = new File(getHomeDir(), Setup.DIR_WORKSPACE_OLD_FASHIONED);
    // todo ensure it exists - really here ?
    File file = getWorkspaceDir();
    ensureDirExists(file);
    return file;
  }

  public File getConfigFile() {
    return new File(getRootDir(), Setup.FILE_WORKSPACE_CONFIG);
  }

  public File getDatabaseDir() {
    return subdir(Setup.DIR_WORKSPACE_DATABASE);
  }

  public File getStorerDir() {
    return subdir(Setup.DIR_WORKSPACE_STATE);
  }

  public File getDatabaseBackupDir() {
    return subdir(DATABASE_BACKUP_DIR_NAME);
  }

  public File getDownloadDir() {
    return subdir(Setup.DIR_WORKSPACE_DOWNLOAD);
  }

  public File getUploadDir() {
    return subdir(Setup.DIR_WORKSPACE_UPLOAD);
  }

  public File getConfigBackupDir() {
    return subdir(Setup.DIR_CONFIG_BACKUP_DIR);
  }

  public File getLogDir() {
    return subdirOrOldFashioned(LOG_DIR_NAME, Setup.DIR_LOGS);
  }

  public File getEtcDir() {
    return subdirOrOldFashioned(ETC_DIR_NAME, Setup.DIR_ETC);
  }

  public File getComponentHintsDir() {
    return subdirOrOldFashioned(COMPONENTS_DIR_NAME, Setup.DIR_COMPONENTS);
  }

  @Nullable
  public File getEtcCollectionFile(String collectionFolder, String fileName) {
    File f;
    if (!isOldFashioned()) {
      f = getEtcCollectionFile(subdir(ETC_DIR_NAME), collectionFolder, fileName);
      if (f != null)
        return f;
    }
    return getEtcCollectionFile(getInstallationEtcDir(), collectionFolder, fileName);
  }

  @Nullable
  public Collection<File> getEtcCollectionFiles(String collectionFolder) {
    Set<File> result = Collections15.linkedHashSet();
    if (!isOldFashioned()) {
      listCollectionFiles(subdir(ETC_DIR_NAME), collectionFolder, result);
    }
    listCollectionFiles(getInstallationEtcDir(), collectionFolder, result);
    return result;
  }

  private void listCollectionFiles(File parentDirectory, String collectionFolder, Collection<File> result) {
    File dir = new File(parentDirectory, collectionFolder);
    if (dir.isDirectory()) {
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isFile() && file.canRead()) {
            result.add(file);
          }
        }
      }
    }
  }

  private File getEtcCollectionFile(File etcDir, String collectionFolder, String fileName) {
    File candidate = new File(new File(etcDir, collectionFolder), fileName);
    if (candidate.isFile() && candidate.canRead()) {
      return candidate;
    } else {
      return null;
    }
  }

  public File getEtcFile(String file) {
    if (!isOldFashioned()) {
      File etc = subdir(ETC_DIR_NAME);
      File result = new File(etc, file);
      if (result.isFile())
        return result;
    }
    File result = new File(getInstallationEtcDir(), file);
    if (result.isFile())
      return result;

    return null;
  }

  public File getInstallationEtcDir() {
    return new File(Setup.getHomeDir(), Setup.DIR_ETC);
  }

  @Override
  public File getTempDir() {
    // not using subdir: nothing bad if we cannot create temp directory
    File file = new File(getRootDir(), Setup.DIR_TEMP);
    file.mkdirs();
    return file.isDirectory() ? file : null;
  }

  @NotNull
  @Override
  public File getAuxDir(String relativePath) {
    File root = getRootDir();
    File aux = new File(root, "auxiliary");
    ensureDirExists(aux);
    File dir = new File(aux, relativePath);
    ensureDirExists(dir);
    return dir;
  }

  private File subdirOrOldFashioned(String subdir, String oldHomeDir) {
    if (isOldFashioned()) {
      File file = new File(Setup.getHomeDir(), oldHomeDir);
      ensureDirExists(file);
      return file;
    } else {
      return subdir(subdir);
    }
  }

  public File getHomeDir() {
    return Setup.getHomeDir();
  }

  protected File getWorkspaceDir() {
    return Setup.getWorkspaceDir();
  }

  protected boolean isOldFashioned() {
    return Setup.isWorkspaceOldfashioned();
  }

  private File subdir(String name) {
    File file = new File(getRootDir(), name);
    ensureDirExists(file);
    return file;
  }

  private void ensureDirExists(File file) {
    if (!file.isDirectory()) {
      if (file.exists())
        throw new Failure(file + " is not a directory, cannot continue");
      boolean success = file.mkdirs();
      if (!success)
        throw new Failure("cannot create directory " + file);
    }
  }

  @Nullable
  public File getLockFile() {
    return new File(getRootDir(), Setup.FILE_WORKSPACE_LOCK);
  }

  public void shutdown() {
    WorkAreaLocker locker = myLocker;
    if (locker != null) {
      locker.unlock();
      myLocker = null;
    }

    denounceWorkArea();
  }

  public void start() throws ControlPassedException {
    assert myLocker == null;
    File lockFile = getLockFile();
    if (lockFile != null) {
      myLocker = new WorkAreaLocker(lockFile, myUrlsToOpen);
      myLocker.lock();
    }

    announceWorkArea();
  }

  private void announceWorkArea() {
    Preferences node = Setup.getUserPreferences().node(Setup.PREFERENCES_INSTANCES);
    String workareaPath = getWorkspaceDir().getAbsolutePath();
    checkStaleAnnouncedWorkAreas(node, workareaPath);
    myWorkareaAnnouncementKey = Long.toHexString(Setup.getApplicationStartTime());
    node.put(myWorkareaAnnouncementKey, workareaPath);
    try {
      node.flush();
    } catch (BackingStoreException e) {
      // ignore
    }
  }

  private void checkStaleAnnouncedWorkAreas(Preferences node, String thisWorkarea) {
    File thisArea = new File(thisWorkarea);
    try {
      String[] keys = node.keys();
      if (keys != null) {
        for (String key : keys) {
          String w = node.get(key, null);
          if (w != null) {
            File file = new File(w);
            if (thisArea.equals(file) || !isLocked(file)) {
              node.remove(key);
            }
          }
        }
      }
    } catch (BackingStoreException e) {
      // ?
    }
  }

  private boolean isLocked(File lockFile) {
    RandomAccessFile rfile;
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
  }

  private void denounceWorkArea() {
    Preferences node = Setup.getUserPreferences().node(Setup.PREFERENCES_INSTANCES);
    String key = myWorkareaAnnouncementKey;
    if (key != null)
      node.remove(key);
    try {
      node.flush();
    } catch (BackingStoreException e) {
      // ignore
    }
  }

  public void announceApiPort(int port) {
    WorkAreaLocker locker = myLocker;
    if (locker != null) {
      locker.announceApiPort(port);
    }
  }
}
