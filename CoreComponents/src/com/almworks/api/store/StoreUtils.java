package com.almworks.api.store;

import com.almworks.util.CantGetHereException;
import com.almworks.util.io.persist.*;
import org.almworks.util.Log;
import org.almworks.util.Util;

import java.io.*;

public class StoreUtils {
  private StoreUtils() {}

  public static void storePersistable(Store store, String storeId, Persistable persistable) {
    StoreAccess storeAccess = store.access(storeId);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream output = new DataOutputStream(baos);

    try {
      PersistableUtil.storePersistable(persistable, output);
      output.close();
    } catch (IOException e) {
      throw new CantGetHereException();
    }

    storeAccess.store(baos.toByteArray());
  }

  /**
   * @return true if restored successfully
   */
  public static boolean restorePersistable(Store store, String storeId, Persistable persistable) {
    StoreAccess storeAccess = store.access(storeId);
    byte[] bytes = storeAccess.load();
    if (bytes == null)
      return false;
    DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
    try {
      // todo atomic read
      PersistableUtil.restorePersistable(persistable, input);
    } catch (IOException e) {
      Log.debug("cannot restore persistable objects", e);
      return false;
    }
    return true;
  }

  public static boolean restoreBoolean(Store store, String storeId, String name) {
    PersistableBoolean v = new PersistableBoolean();
    if (restorePersistable(store, storeId + "." + name, v)) {
      Boolean b = v.access();
      return b != null && b;
    } else {
      return false;
    }
  }

  public static void storeBoolean(Store store, String storeId, String name, boolean value) {
    PersistableBoolean v = new PersistableBoolean(value);
    storePersistable(store, storeId + "." + name, v);
  }

  public static long restoreLong(Store store, String storeId, String name, long defaultValue) {
    PersistableLong v = new PersistableLong();
    if (restorePersistable(store, storeId + "." + name, v)) {
      Long b = v.access();
      return b == null ? defaultValue : b;
    } else {
      return defaultValue;
    }
  }

  public static void storeLong(Store store, String storeId, String name, long value) {
    PersistableLong v = new PersistableLong(value);
    storePersistable(store, storeId + "." + name, v);
  }

  public static int restoreInt(Store store, String key, int defaultValue) {
    PersistableInteger pi = new PersistableInteger();
    if (restorePersistable(store, key, pi)) {
      Integer i = pi.access();
      return Util.NN(i, defaultValue);
    }
    return defaultValue;
  }

  public static void storeInt(Store store, String key, int value) {
    PersistableInteger pi = new PersistableInteger(value);
    storePersistable(store, key, pi);
  }
}
