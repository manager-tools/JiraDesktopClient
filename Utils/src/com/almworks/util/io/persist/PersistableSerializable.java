package com.almworks.util.io.persist;

import org.almworks.util.Const;
import org.almworks.util.Log;
import util.external.CompactInt;

import java.io.*;

public class PersistableSerializable<T extends Serializable> extends LeafPersistable<T> {
  private final Class<T> myClass;

  private T myValue;

  public PersistableSerializable(Class<T> valueClass) {
    myClass = valueClass;
  }

  protected void doClear() {
    myValue = null;
  }

  protected T doAccess() {
    return myValue;
  }

  protected T doCopy() {
    // todo & Clonable?
    return myValue;
  }

  protected void doSet(T value) {
    myValue = value;
  }

  protected void doRestore(DataInput in) throws IOException, FormatException {
    int length = CompactInt.readInt(in);
    if (length <= 0) {
      myValue = null;
    } else if (length >= 1000000) {
      throw new IOException("object too large: " + length);
    } else {
      byte[] buffer = new byte[length];
      in.readFully(buffer);
      ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
      ObjectInputStream ois = new ObjectInputStream(bais);
      Object o = null;
      try {
        o = ois.readObject();
        if (o != null && !myClass.isInstance(o)) {
          throw new ClassCastException();
        }
        myValue = (T) o;
      } catch (ClassNotFoundException e) {
        Log.warn(e);
        throw new IOException("can't read: " + e);
      } catch (ClassCastException e) {
        Log.warn(": " + o, e);
        throw new IOException("bad object " + e);
      }
    }
  }

  protected void doStore(DataOutput out) throws IOException {
    byte[] bytes;
    if (myValue == null) {
      bytes = Const.EMPTY_BYTES;
    } else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(myValue);
      oos.close();
      baos.close();
      bytes = baos.toByteArray();
    }
    CompactInt.writeInt(out, bytes.length);
    out.write(bytes);
  }
}
