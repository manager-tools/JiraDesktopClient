package com.almworks.util.io.persist;

import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

public abstract class PersistableCollection<T, C extends Collection<T>> extends LeafPersistable<C> {
  private final Persistable<T> myElementBuilder;
  private C myCollection;

  protected PersistableCollection(Persistable<T> elementBuilder) {
    assert elementBuilder != null;
    myElementBuilder = elementBuilder;
    setInitialized(true);
  }

  protected abstract C createCollection();

  protected void doClear() {
    myCollection = null;
  }

  protected C doAccess() {
    initCollection();
    return myCollection;
  }

  protected C doCopy() {
    C result = createCollection();
    if (myCollection != null)
      result.addAll(myCollection);
    return result;
  }

  protected void doSet(C collection) {
    assert collection != null;
    myCollection = createCollection();
    myCollection.addAll(collection);
  }

  protected void doRestore(DataInput in) throws IOException, FormatException {
    initCollection();
    myCollection.clear();
    int count = CompactInt.readInt(in);
    if (count < 0)
      throw new FormatException("count " + count);
    for (int i = 0; i < count; i++) {
      PersistableUtil.restorePersistable(myElementBuilder, in);
      myCollection.add(myElementBuilder.copy());
    }
    myElementBuilder.clear();
  }

  protected void doStore(DataOutput out) throws IOException {
    initCollection();
    CompactInt.writeInt(out, myCollection.size());
    for (T element : myCollection) {
      myElementBuilder.set(element);
      PersistableUtil.storePersistable(myElementBuilder, out);
    }
    myElementBuilder.clear();
  }

  private void initCollection() {
    if (myCollection == null)
      myCollection = createCollection();
  }
}
