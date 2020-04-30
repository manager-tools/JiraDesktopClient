package com.almworks.util.io.persist;

import com.almworks.util.Trio;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PersistableTrio<A, B, C> extends LeafPersistable<Trio<A, B, C>> {
  private final Persistable<A> myFirstPersister;
  private final Persistable<B> mySecondPersister;
  private final Persistable<C> myThirdPersister;

  private Trio<A, B, C> myTrio;

  public PersistableTrio(Persistable<A> firstPersister, Persistable<B> secondPersister, Persistable<C> thirdPersister) {
    myFirstPersister = firstPersister;
    mySecondPersister = secondPersister;
    myThirdPersister = thirdPersister;
  }

  public static <A, B, C> PersistableTrio<A, B, C> create(Persistable<A> first, Persistable<B> second,
    Persistable<C> third)
  {
    return new PersistableTrio<A, B, C>(first, second, third);
  }

  protected void doClear() {
    myTrio = null;
  }

  protected Trio<A, B, C> doAccess() {
    return myTrio;
  }

  protected Trio<A, B, C> doCopy() {
    return myTrio;
  }

  protected void doRestore(DataInput in) throws IOException, FormatException {
    myFirstPersister.restore(in);
    mySecondPersister.restore(in);
    myThirdPersister.restore(in);
    myTrio = Trio.create(myFirstPersister.copy(), mySecondPersister.copy(), myThirdPersister.copy());
  }

  protected void doSet(Trio<A, B, C> value) {
    myTrio = value;
  }

  protected void doStore(DataOutput out) throws IOException {
    if (myTrio == null) {
      assert false : this;
      return;
    }
    myFirstPersister.set(myTrio.getFirst());
    mySecondPersister.set(myTrio.getSecond());
    myThirdPersister.set(myTrio.getThird());
    myFirstPersister.store(out);
    mySecondPersister.store(out);
    myThirdPersister.store(out);
  }
}
