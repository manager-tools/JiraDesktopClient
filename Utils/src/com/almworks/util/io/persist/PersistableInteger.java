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
public class PersistableInteger extends LeafPersistable<Integer> {
  protected Integer myValue;

  public PersistableInteger(int value) {
    set(value);
  }

  public PersistableInteger() {
  }


  protected Integer doAccess() {
    return myValue;
  }

  protected Integer doCopy() {
    return myValue;
  }

  protected void doSet(Integer value) {
    myValue = value;
  }

  protected void doClear() {
    myValue = null;
  }

  protected void doRestore(DataInput in) throws IOException {
    set(CompactInt.readInt(in));
  }

  protected void doStore(DataOutput out) throws IOException {
    CompactInt.writeInt(out, myValue.intValue());
  }
}
