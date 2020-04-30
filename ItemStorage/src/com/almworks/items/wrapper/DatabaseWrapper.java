package com.almworks.items.wrapper;

import com.almworks.items.api.*;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.UserDataHolder;
import org.almworks.util.detach.Lifespan;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Note: this class does <b>not</b> extend DatabaseDelegator so that if someone added more abstract methods to Database, compilation would break <i>here</i> to remind that these methods should possibly be wrapped.
 * */
public class DatabaseWrapper<D extends Database> extends BaseDatabase {
  private final D myDelegator;

  public DatabaseWrapper(D delegator) {
    myDelegator = delegator;
  }

  protected D getDelegator() {
    return myDelegator;
  }

  @Override
  public <T> DBResult<T> read(DBPriority priority, ReadTransaction<T> transaction) {
    ReadTransactionWrapper<T> wrappedTransaction = new ReadTransactionWrapper<T>(transaction);
    return new DBResultWrapper<T>(myDelegator.read(priority, wrappedTransaction));
  }

  @Override
  public <T> DBResult<T> write(DBPriority priority, WriteTransaction<T> transaction) {
    WriteTransactionWrapper<T> wrappedTransaction = new WriteTransactionWrapper<T>(transaction);
    return new DBResultWrapper<T>(myDelegator.write(priority, wrappedTransaction));
  }

  @Override
  public DBLiveQuery liveQuery(Lifespan lifespan, BoolExpr<DP> expr, DBLiveQuery.Listener listener) {
    BoolExpr<DP> wrappedExpr = ItemStorageAdaptor.wrapExpr(expr);
    DBLiveQueryListenerWrapper wrappedListener = new DBLiveQueryListenerWrapper(listener);
    return new DBLiveQueryWrapper(myDelegator.liveQuery(lifespan, wrappedExpr, wrappedListener));
  }

  DBLiveQuery liveQueryUnwrapped(Lifespan lifespan, BoolExpr<DP> expr, DBLiveQuery.Listener listener) {
    return myDelegator.liveQuery(lifespan, expr, listener);
  }

  @Override
  public void addListener(Lifespan lifespan, DBListener listener) {
    DBListenerWrapper wrappedListener = new DBListenerWrapper(listener);
    myDelegator.addListener(lifespan, wrappedListener);
  }

  void addListenerUnwrapped(Lifespan lifespan, DBListener listener) {
    myDelegator.addListener(lifespan, listener);
  }

  @Override
  public void setLongHousekeepingAllowed(boolean housekeepingAllowed) {
    myDelegator.setLongHousekeepingAllowed(housekeepingAllowed);
  }

  @Override
  public void dump(PrintStream writer) throws IOException {
    myDelegator.dump(writer);
  }

  @Override
  public void registerTrigger(DBTrigger trigger) {
    DBTriggerWrapper wrappedTrigger = new DBTriggerWrapper(trigger);
    myDelegator.registerTrigger(wrappedTrigger);
  }

  @Override
  public UserDataHolder getUserData() {
    return myDelegator.getUserData();
  }

  void registerTriggerUnwrapped(DBTrigger trigger) {
    myDelegator.registerTrigger(trigger);
  }

  @Override
  public boolean isDbThread() {
    return myDelegator.isDbThread();
  }
}
