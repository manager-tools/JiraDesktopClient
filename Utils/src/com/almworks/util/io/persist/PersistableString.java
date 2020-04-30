package com.almworks.util.io.persist;

import util.external.CompactChar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PersistableString extends LeafPersistable<String> {
  protected String myValue;

  public PersistableString(String value) {
    set(value);
  }

  public PersistableString() {
  }

  protected String doAccess() {
    return myValue;
  }

  protected String doCopy() {
    return myValue;
  }

  protected void doSet(String value) {
    myValue = value;
  }

  protected void doClear() {
    myValue = null;
  }

  protected void doRestore(DataInput in) throws IOException {
    set(CompactChar.readString(in));
  }

  protected void doStore(DataOutput out) throws IOException {
    CompactChar.writeString(out, myValue);
  }
}
