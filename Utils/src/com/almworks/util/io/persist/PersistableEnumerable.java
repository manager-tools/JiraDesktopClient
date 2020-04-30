package com.almworks.util.io.persist;

import com.almworks.util.Enumerable;
import util.external.CompactChar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PersistableEnumerable<E extends Enumerable> extends LeafPersistable<E> {
  private final Class<E> myClass;
  private E myValue;

  public PersistableEnumerable(Class<E> clazz) {
    assert clazz != null;
    myClass = clazz;
  }

  public static <E extends Enumerable> PersistableEnumerable<E> create(Class<E> clazz) {
    return new PersistableEnumerable<E>(clazz);
  }

  protected void doClear() {
    myValue = null;
  }

  protected E doAccess() {
    return myValue;
  }

  protected E doCopy() {
    return myValue;
  }

  protected void doRestore(DataInput in) throws IOException, FormatException {
    String name = CompactChar.readString(in);
    if (name == null) {
      myValue = null;
    } else {
      E e = Enumerable.forName(myClass, name);
      if (e == null)
        throw new FormatException("unknown " + myClass + " " + name);
      myValue = e;
    }
  }

  protected void doSet(E e) {
    myValue = e;
  }

  public void doStore(DataOutput out) throws IOException {
    String name = myValue == null ? null : myValue.getName();
    CompactChar.writeString(out, name);
  }
}
