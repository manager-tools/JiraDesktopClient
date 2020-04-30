package com.almworks.items.util.delegate;

import com.almworks.items.api.*;
import com.almworks.util.Pair;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.UserDataHolder;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import util.concurrent.SynchronizedBoolean;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class WaitingState extends BaseDatabase {
  private final UserDataHolder myUserData = new UserDataHolder();
  private final Object myLock = myUserData.getUserDataLock();
  private final List<TransactionWrapper<?>> myTransactions = Collections15.arrayList();
  private final List<Pair<Lifespan, DBListener>> myListeners = Collections15.arrayList();
  private final List<LifeQueryWrapper> myQueries = Collections15.arrayList();
  private final List<DBTrigger> myTriggers = Collections15.arrayList();
  private Boolean myHourKeepingAllowed = null;
  private Database myReplaced = null;

  public Object getLock() {
    return myLock;
  }

  public void replaceDB(Database db) {
    assert Thread.holdsLock(myLock);
    assert myLock == myUserData.getUserDataLock();
    myReplaced = db;
    Collection<TypedKey<?>> keys = myUserData.keySet();
    for (TypedKey<?> key : keys) UserDataHolder.copy(key, myUserData, myReplaced.getUserData());
  }

  public void enqueueTransactions(SynchronizedBoolean canRun, ArrayList<TransactionWrapper<?>> queued) {
    assert Thread.holdsLock(myLock);
    for (TransactionWrapper<?> transaction : myTransactions) transaction.enqueue(myReplaced, canRun, queued);
  }

  public void passTriggers() {
    assert Thread.holdsLock(myLock);
    assert myReplaced != null;
    for (DBTrigger trigger : myTriggers) myReplaced.registerTrigger(trigger);
    myTriggers.clear();
  }

  public void passQueries() {
    ArrayList<LifeQueryWrapper> wrappers;
    Database db;
    synchronized (myLock) {
      wrappers = Collections15.arrayList(myQueries);
      myQueries.clear();
      db = myReplaced;
    }
    assert db != null;
    for (LifeQueryWrapper wrapper : wrappers) wrapper.birth(db);
  }

  public Boolean isLongHousekeepingAllowed() {
    assert Thread.holdsLock(myLock);
    return myHourKeepingAllowed;
  }
  
  @Override
  public <T> DBResult<T> read(DBPriority priority, ReadTransaction<T> transaction) {
    Database db;
    synchronized (myLock) {
      if (myReplaced == null) {
        TransactionWrapper<T> wrapper = TransactionWrapper.read(priority, transaction);
        myTransactions.add(wrapper);
        return wrapper;
      }
      db = myReplaced;
    }
    return db.read(priority, transaction);
  }

  @Override
  public <T> DBResult<T> write(DBPriority priority, WriteTransaction<T> transaction) {
    Database db;
    synchronized (myLock) {
      if (myReplaced == null) {
        TransactionWrapper<T> wrapper = TransactionWrapper.write(priority, transaction);
        myTransactions.add(wrapper);
        return wrapper;
      }
      db = myReplaced;
    }
    return db.write(priority, transaction);
  }

  @Override
  public void addListener(Lifespan lifespan, DBListener listener) {
    Database db;
    synchronized (myLock) {
      if (myReplaced == null) {
        myListeners.add(Pair.create(lifespan, listener));
        return;
      }
      db = myReplaced;
    }
    db.addListener(lifespan, listener);
  }

  @Override
  public DBLiveQuery liveQuery(Lifespan lifespan, BoolExpr<DP> expr, DBLiveQuery.Listener listener) {
    Database db;
    synchronized (myLock) {
      if (myReplaced == null) {
        LifeQueryWrapper wrapper = new LifeQueryWrapper(lifespan, expr, listener);
        myQueries.add(wrapper);
        return wrapper;
      }
      db = myReplaced;
    }
    return db.liveQuery(lifespan, expr, listener);
  }

  @Override
  public void setLongHousekeepingAllowed(boolean housekeepingAllowed) {
    Database db;
    synchronized (myLock) {
      if (myReplaced == null) {
        myHourKeepingAllowed = housekeepingAllowed;
        return;
      }
      db = myReplaced;
    }
    db.setLongHousekeepingAllowed(housekeepingAllowed);
  }

  @Override
  public void dump(PrintStream writer) throws IOException {
    Database db;
    synchronized (myLock) {
      if (myReplaced == null) return;
      db = myReplaced;
    }
    db.dump(writer);
  }

  @Override
  public void registerTrigger(DBTrigger trigger) {
    Database db;
    synchronized (myLock) {
      if (myReplaced == null) {
        myTriggers.add(trigger);
        return;
      }
      db = myReplaced;
    }
    db.registerTrigger(trigger);
  }

  @Override
  public UserDataHolder getUserData() {
    Database db;
    synchronized (myLock) {
      if (myReplaced == null) return myUserData;
      db = myReplaced;
    }
    return db.getUserData();
  }

  @Override
  public boolean isDbThread() {
    Database db;
    synchronized (myLock) {
      if (myReplaced == null) return false;
      db = myReplaced;
    }
    return db.isDbThread();
  }
}
