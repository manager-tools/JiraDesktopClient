package com.almworks.util.io.persist;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
public abstract class AbstractPersistable <V> implements Persistable<V> {
  private boolean myInitialized = false;

  public final void store(DataOutput out) throws IOException {
    if (!myInitialized)
      throw new IllegalStateException("not initialized");
    doStore(out);
  }

  public final void restore(DataInput in) throws IOException, FormatException {
    doRestore(in);
    setInitialized(true);
  }

  public boolean isInitialized() {
    return myInitialized;
  }

  protected void setInitialized(boolean initialized) {
    myInitialized = initialized;
  }

  public final V access() {
    if (!myInitialized)
      throw new IllegalStateException("not initialized");
    return doAccess();
  }

  public V copy() {
    if (!myInitialized)
      throw new IllegalStateException("not initialized");
    return doCopy();
  }

  public final void set(V value) {
    doSet(value);
    setInitialized(true);
  }

  public void clear() {
    doClear();
    setInitialized(false);
  }

  protected abstract void doClear();

  protected abstract V doAccess();

  protected abstract V doCopy();

  protected abstract void doRestore(DataInput in) throws IOException, FormatException;

  protected abstract void doSet(V value);

  protected abstract void doStore(DataOutput out) throws IOException;
}
