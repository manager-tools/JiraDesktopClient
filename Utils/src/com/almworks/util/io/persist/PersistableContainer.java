package com.almworks.util.io.persist;

import org.almworks.util.Collections15;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class PersistableContainer extends AbstractPersistable<Object> {
  private final List<Persistable> myPersistables = Collections15.arrayList();
  private final List<Persistable> myPersistablesReadOnly = Collections.unmodifiableList(myPersistables);
  private boolean myGetChildrenCalled = false;

  protected PersistableContainer() {
    setInitialized(true);
  }

  protected final void doRestore(DataInput in) throws IOException {
    // do nothing. use children persistable to do storing
  }

  protected final void doStore(DataOutput out) throws IOException {
    // do nothing. use children persistable to do storing
  }

  protected void doClear() {
  }

  protected Object doAccess() {
    return null;
  }

  protected Object doCopy() {
    return null;
  }

  protected void doSet(Object o) {
  }

  public List<Persistable> getChildren() {
    myGetChildrenCalled = true;
    return myPersistablesReadOnly;
  }

  protected final void persist(Persistable persistable) {
    if (myGetChildrenCalled)
      throw new IllegalStateException("cannot add persistable - getChildren called");
    myPersistables.add(persistable);
  }

  public static PersistableContainer create(final Persistable[] components) {
    return new PersistableContainer() {
      {
        for (int i = 0; i < components.length; i++)
          persist(components[i]);
      }
    };
  }
}
