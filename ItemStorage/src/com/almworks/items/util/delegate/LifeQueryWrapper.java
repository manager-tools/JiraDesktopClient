package com.almworks.items.util.delegate;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongCollector;
import com.almworks.items.api.DBLiveQuery;
import com.almworks.items.api.DP;
import com.almworks.items.api.Database;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.LongObjFunction2;
import org.almworks.util.detach.Lifespan;

class LifeQueryWrapper implements DBLiveQuery {
  private final Object myLock = new Object();
  private DBLiveQuery myQuery;
  private final Lifespan myLife;
  private final BoolExpr<DP> myExpr;
  private final Listener myListener;

  public LifeQueryWrapper(Lifespan life, BoolExpr<DP> expr, Listener listener) {
    myLife = life;
    myExpr = expr;
    myListener = listener;
  }

  public void birth(Database db) {
    if (myLife.isEnded()) return;
    synchronized (myLock) {
      myQuery = db.liveQuery(myLife, myExpr, myListener);
    }
  }

  @Override
  public boolean isInitialized() {
    DBLiveQuery query;
    synchronized (myLock) {
      if (myQuery == null) return false;
      query = myQuery;
    }
    return query.isInitialized();
  }

  @Override
  public LongArray copyItems() {
    DBLiveQuery query;
    synchronized (myLock) {
      if (myQuery == null) return new LongArray();
      query = myQuery;
    }
    return query.copyItems();
  }

  @Override
  public <C extends LongCollector> C copyItems(C collector) {
    DBLiveQuery query;
    synchronized (myLock) {
      if (myQuery == null) return collector;
      query = myQuery;
    }
    return query.copyItems(collector);
  }

  @Override
  public <T> T fold(T seed, LongObjFunction2<T> f) {
    DBLiveQuery query;
    synchronized (myLock) {
      if (myQuery == null) return seed;
      query = myQuery;
    }
    return query.fold(seed, f);
  }

  @Override
  public int count() {
    DBLiveQuery query;
    synchronized (myLock) {
      if (myQuery == null) return 0;
      query = myQuery;
    }
    return query.count();
  }

  @Override
  public boolean isTransactionResultVisible(long icn) {
    DBLiveQuery query;
    synchronized (myLock) {
      if (myQuery == null) return false;
      query = myQuery;
    }
    return query.isTransactionResultVisible(icn);
  }
}
