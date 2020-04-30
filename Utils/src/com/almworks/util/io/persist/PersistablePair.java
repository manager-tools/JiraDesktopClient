package com.almworks.util.io.persist;

import com.almworks.util.Pair;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PersistablePair<A, B> extends LeafPersistable<Pair<A, B>> {
  private final Persistable<A> myFirstPersister;
  private final Persistable<B> mySecondPersister;

  private Pair<A, B> myPair;

  public PersistablePair(Persistable<A> firstPersister, Persistable<B> secondPersister) {
    myFirstPersister = firstPersister;
    mySecondPersister = secondPersister;
  }

  public static <A, B> PersistablePair<A, B> create(Persistable<A> first, Persistable<B> second) {
    return new PersistablePair<A, B>(first, second);
  }

  protected void doClear() {
    myPair = null;
  }

  protected Pair<A, B> doAccess() {
    return myPair;
  }

  protected Pair<A, B> doCopy() {
    return myPair;
  }

  protected void doRestore(DataInput in) throws IOException, FormatException {
    myFirstPersister.restore(in);
    mySecondPersister.restore(in);
    myPair = Pair.create(myFirstPersister.copy(), mySecondPersister.copy());
  }

  protected void doSet(Pair<A, B> value) {
    myPair = value;
  }

  protected void doStore(DataOutput out) throws IOException {
    if (myPair == null) {
      assert false : this;
      return;
    }
    myFirstPersister.set(myPair.getFirst());
    mySecondPersister.set(myPair.getSecond());
    myFirstPersister.store(out);
    mySecondPersister.store(out);
  }
}
