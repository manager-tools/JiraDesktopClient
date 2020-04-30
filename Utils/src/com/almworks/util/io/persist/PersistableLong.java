package com.almworks.util.io.persist;

import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
public class PersistableLong extends LeafPersistable<Long> {
  protected Long myValue;

  public PersistableLong(long value) {
    set(new Long(value));
  }

  public PersistableLong() {
  }

  protected Long doAccess() {
    return myValue;
  }

  protected Long doCopy() {
    return myValue;
  }

  protected void doSet(Long value) {
    myValue = value;
  }

  protected void doClear() {
    myValue = null;
  }

  protected void doRestore(DataInput in) throws IOException {
    set(new Long(CompactInt.readLong(in)));
  }

  protected void doStore(DataOutput out) throws IOException {
    CompactInt.writeLong(out, myValue == null ? 0 : myValue);
  }
}
