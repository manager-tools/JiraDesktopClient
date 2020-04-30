package com.almworks.store;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.store.Store;
import com.almworks.sqlite4java.SQLite;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.io.IOUtils;
import com.almworks.util.model.ModelUtils;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Log;
import org.almworks.util.StringUtil;
import org.picocontainer.Startable;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SQLiteChecker implements Startable, Runnable {
  private static final String STORE_KEY = "$SQLiteChecker$";

  private final Store myStore;
  private final ApplicationLoadStatus myStatus;

  public SQLiteChecker(Store store, ApplicationLoadStatus status) {
    myStore = store;
    myStatus = status;
  }

  @Override
  public void start() {
    ModelUtils.whenTrue(myStatus.getApplicationLoadedModel(), ThreadGate.LONG, this);
  }

  @Override
  public void stop() {}

  @Override
  public void run() {
    try {
      SQLite.loadLibrary();
      handleSuccess();
    } catch(SQLiteException e) {
      handleFailure(e);
    }
  }

  private void handleSuccess() {
    myStore.access(STORE_KEY).clear();
  }

  private void handleFailure(SQLiteException e) {
    final String hashes = getUpdatedHashes(e);
    if(hashes != null) {
      reportAndSave(e, hashes);
    }
  }

  private String getUpdatedHashes(SQLiteException e) {
    final String hash = getTraceHash(e);
    if(hash == null) {
      return null;
    }

    final String[] shown = loadOldHashes();
    if(shown == null) {
      return null;
    }

    if(ArrayUtil.contains(shown, hash)) {
      return null;
    }

    if(shown.length == 0) {
      return hash;
    }

    return StringUtil.implode(Arrays.asList(shown), " ") + " " + hash;
  }

  private String getTraceHash(SQLiteException e) {
    try {
      return IOUtils.md5sum(getTrace(e));
    } catch(NoSuchAlgorithmException e1) {
      Log.warn(e);
      return null;
    } catch(UnsupportedEncodingException e1) {
      Log.warn(e);
      return null;
    }
  }

  private String getTrace(SQLiteException e) {
    return appendExceptionChain(new StringBuilder(), e).toString();
  }

  private StringBuilder appendExceptionChain(StringBuilder b, Throwable e) {
    if(e == null) {
      return b;
    }

    b.append(e.getClass().getName()).append(e.getMessage());
    for(final StackTraceElement ste : e.getStackTrace()) {
      if(!isVolatile(ste)) {
        b.append(ste.toString());
      }
    }

    return appendExceptionChain(b, e.getCause());
  }

  private boolean isVolatile(StackTraceElement ste) {
    final String className = ste.getClassName();
    return className.startsWith("sun.reflect.");
  }

  private String[] loadOldHashes() {
    final byte[] bytes = myStore.access(STORE_KEY).load();
    if(bytes == null || bytes.length == 0) {
      return new String[0];
    }

    try {
      return new String(bytes, "UTF-8").split(" ");
    } catch (UnsupportedEncodingException e) {
      Log.warn(e);
      return null;
    }
  }

  private void reportAndSave(SQLiteException e, final String hashes) {
    Log.error(hashes, e);
  }
}
