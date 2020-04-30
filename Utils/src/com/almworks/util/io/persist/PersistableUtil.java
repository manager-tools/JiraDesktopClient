package com.almworks.util.io.persist;

import com.almworks.util.io.IOUtils;

import java.io.*;
import java.util.Iterator;

/**
 * :todoc:
 *
 * @author sereda
 */
public class PersistableUtil {
  public static void storePersistable(Persistable persistable, DataOutput out) throws IOException {
    if (!persistable.isInitialized())
      throw new IllegalStateException(persistable + " is not initialized");
    persistable.store(out);
    for (Iterator<Persistable> ii = persistable.getChildren().iterator(); ii.hasNext();) {
      storePersistable(ii.next(), out);
    }
  }

  public static byte[] storePersistable(Persistable persistable) throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(stream);
    storePersistable(persistable, out);
    out.close();
    return stream.toByteArray();
  }

  public static void restorePersistable(Persistable persistable, DataInput in) throws IOException, FormatException {
    persistable.restore(in);
    for (Iterator<Persistable> ii = persistable.getChildren().iterator(); ii.hasNext();) {
      restorePersistable(ii.next(), in);
    }
    if (!persistable.isInitialized())
      throw new IllegalStateException(persistable + " is not initialized after restore");
  }

  public static void restorePersistable(Persistable persistable, byte[] data) throws IOException, FormatException {
    if (data == null)
      throw new FormatException("data is null");
    ByteArrayInputStream stream = null;
    DataInputStream in = null;
    try {
      stream = new ByteArrayInputStream(data);
      in = new DataInputStream(stream);
      restorePersistable(persistable, in);
    } finally {
      IOUtils.closeStreamIgnoreExceptions(in);
      IOUtils.closeStreamIgnoreExceptions(stream);
    }
  }
}
