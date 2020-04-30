package com.almworks.items.impl;

import com.almworks.integers.*;
import com.almworks.items.api.*;
import com.almworks.util.LazySubscription;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.Reductions;
import com.almworks.util.commons.LongObjFunction2;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DBLiveQueryImpl implements DBLiveQuery {
  private final BoolExpr<DP> myExpr;
  private final LongArray myCachedItemsSorted = new LongArray();
  private BoolExpr<DP> myLastResolvedExpr;
  private final Lifecycle myCommonListenersLife = new Lifecycle();

  private final Object myLock = myCachedItemsSorted;
  private final LazySubscription<Listener> myListeners = LazySubscription.create(Listener.class, new Subscriber());
  // these will be sent "initial" update on the next update
  private final Map<Listener, Lifespan> myUninitializedListeners = Collections15.hashMap();

  private long myIcn;

  public DBLiveQueryImpl(BoolExpr<DP> expr) {
    myExpr = expr;
  }

  @Override
  public String toString() {
    return "DBLQ[" + myExpr + "]";
  }

  public void addListener(final Lifespan life, final Listener listener) {
    synchronized (myLock) {
      myUninitializedListeners.put(listener, life);
    }
  }

  public long getLastProcessedTransactionIcn() {
    synchronized (myLock) {
      return myIcn;
    }
  }

  public void guidedUpdate(long fromIcn, long toIcn, DBEvent incomingEvent, @Nullable BoolExpr<DP> resolvedExpr, DBReader reader, LiveQueryManager.ResolutionSubscription commonSubscription) {
    assert myIcn == fromIcn : this + " " + myIcn + " " + fromIcn;
    boolean fullUpdate = false;
    if (resolvedExpr == null) {
      resolvedExpr = resolve(myExpr, reader, commonSubscription);
    }
    fullUpdate = !resolvedExpr.equals(myLastResolvedExpr);
    // todo synchorniz
    DBQuery query = reader.query(resolvedExpr);
    DBEvent fireEvent = DBEvent.EMPTY;
    if (fullUpdate) {
      synchronized (myLock) {
        LongArray oldItems = LongArray.copy(myCachedItemsSorted);
        fullUpdateLocked(toIcn, resolvedExpr, reader);
        LongArray affected = query.filterItemsSorted(incomingEvent.getAffectedSorted());
        affected.addAll(LongCollections.diffSortedUniqueLists(oldItems, myCachedItemsSorted));
        affected.sortUnique();
        fireEvent = DBEvent.create(oldItems, affected, myCachedItemsSorted);
      }
    } else if (!incomingEvent.isEmpty()) {
      LongList update = incomingEvent.getAddedAndChangedSorted();
      LongSetBuilder setBuilder = new LongSetBuilder();
      query.filterItems(update, setBuilder);
      LongList inView = setBuilder.commitToArray();
      LongList affectedSorted = incomingEvent.getAffectedSorted();
      synchronized (myLock) {
        fireEvent = DBEvent.createAndUpdateCurrent(myCachedItemsSorted, affectedSorted, inView);
        myIcn = toIcn;
      }
    }
    fire(fireEvent, reader);
  }

  public void independentUpdate(DBReader reader, LiveQueryManager.ResolutionSubscription commonSubscription) {
    assert !((DBReaderImpl) reader).getContext().isWriteAllowed() : reader;
    long lastIcn = reader.getTransactionIcn() - 1;
    long icn;
    BoolExpr<DP> resolved = resolve(myExpr, reader, commonSubscription);
    synchronized (myLock) {
      icn = myIcn;
    }
    if (icn >= lastIcn)
      return;
    if (icn == 0) {
      synchronized (myLock) {
        fullUpdateLocked(lastIcn, resolved, reader);
      }
      fireInit(reader);
    } else {
      LongList list = reader.getChangedItemsSorted(icn);
      DBEvent event = DBEvent.create(list);
      guidedUpdate(icn, lastIcn, event, null, reader, commonSubscription);
    }
  }

  @NotNull
  private BoolExpr<DP> resolve(@NotNull BoolExpr<DP> expr, DBReader reader, LiveQueryManager.ResolutionSubscription subscription) {
    DP.ResolutionSubscription newSubscription = subscription.createIntersection(myCommonListenersLife.lifespan());
    BoolExpr<DP> resolution = DP.resolve(expr, reader, newSubscription);
    return Reductions.<DP>DNF().reduce(resolution);
  }

  private void fullUpdateLocked(long lastIcn, BoolExpr<DP> resolvedExpr, DBReader reader) {
    // todo running query under synchronized?
    assert Thread.holdsLock(myLock);
    myCachedItemsSorted.clear();
    reader.query(resolvedExpr).copyItems(myCachedItemsSorted);
    myCachedItemsSorted.sortUnique();
    myIcn = lastIcn;
    myLastResolvedExpr = resolvedExpr;
  }

  private void fire(@NotNull DBEvent event, DBReader reader) {
    if (!event.isEmpty()) myListeners.getDispatcher().onDatabaseChanged(event, reader);
    else myListeners.getDispatcher().onICNPassed(reader.getTransactionIcn());
    fireInit(reader);
  }

  private void fireInit(DBReader reader) {
    Map<Listener, Lifespan> initialize = null;
    DBEvent init = null;
    synchronized (myLock) {
      if (!myUninitializedListeners.isEmpty()) {
        initialize = Collections15.hashMap(myUninitializedListeners);
        myUninitializedListeners.clear();
        init = DBEvent.create(myCachedItemsSorted);
      }
    }
    if (initialize != null) {
      for (Map.Entry<Listener, Lifespan> e : initialize.entrySet()) {
        Listener listener = e.getKey();
        Lifespan lifespan = e.getValue();
        if (lifespan.isEnded())
          continue;
        safeCall(listener, init, reader);
        myListeners.addListener(lifespan, ThreadGate.STRAIGHT, listener);
      }
    }
  }

  private void safeCall(DBListener listener, DBEvent event, DBReader reader) {
    try {
      listener.onDatabaseChanged(event, reader);
    } catch (Throwable t) {
      Log.error(t);
    }
  }

  @Override
  public boolean isInitialized() {
    synchronized (myLock) {
      return myIcn > 0;
    }
  }

  @Override
  public LongArray copyItems() {
    return copyItems(new LongArray());
  }

  @Override
  public <C extends LongCollector> C copyItems(C collector) {
    synchronized (myLock) {
      if (myIcn > 0) {
        collector.addAll(myCachedItemsSorted);
      }
    }
    return collector;
  }

  @Override
  public <T> T fold(T seed, LongObjFunction2<T> f) {
    // cannot call function under lock
    LongArray items = copyItems();
    for (LongListIterator ii = items.iterator(); ii.hasNext();) {
      seed = f.invoke(ii.nextValue(), seed);
    }
    return seed;
  }


  @Override
  public int count() {
    synchronized (myLock) {
      return myIcn > 0 ? myCachedItemsSorted.size() : 0;
    }
  }

  @Override
  public boolean isTransactionResultVisible(long icn) {
    synchronized (myLock) {
      return myIcn >= icn;
    }
  }

  private final class Subscriber implements LazySubscription.Subscriber {
    @Override
    public void subscribe() {
      myCommonListenersLife.cycleStart();
    }

    @Override
    public void unsubscribe() {
      myCommonListenersLife.cycleEnd();
    }
  }
}
