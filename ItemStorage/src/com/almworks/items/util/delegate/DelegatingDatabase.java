package com.almworks.items.util.delegate;

import com.almworks.items.api.*;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.UserDataHolder;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import util.concurrent.SynchronizedBoolean;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class DelegatingDatabase implements Database {
  private final AtomicReference<Database> myState = new AtomicReference<Database>(new WaitingState());

  private Database getCurrentDB() {
    Database database = myState.get();
    if (database == null) throw new IllegalStateException("Already discarded");
    if (database instanceof WaitingState) onWaitingDBCall();
    return database;
  }

  protected void onWaitingDBCall() {}

  public Database replaceWaitingState(Database db) {
    Database state = myState.get();
    WaitingState waiting = Util.castNullable(WaitingState.class, state);
    if (waiting == null) {
      LogHelper.error("Wrong state", state);
      return state;
    }
    SynchronizedBoolean canRun = new SynchronizedBoolean(false);
    ArrayList<TransactionWrapper<?>> queued = Collections15.arrayList();
    synchronized (waiting.getLock()) {
      Boolean allowed = waiting.isLongHousekeepingAllowed();
      if (allowed != null) db.setLongHousekeepingAllowed(allowed);
      waiting.replaceDB(db);
      waiting.enqueueTransactions(canRun, queued);
      myState.set(db);
      waiting.passTriggers();
    }
    for (TransactionWrapper<?> wrapper : queued) wrapper.addListeners();
    canRun.set(true);
    waiting.passQueries();
    return db;
  }
  
  public Database setNullDB() {
    Database db = myState.get();
    myState.set(null);
    return db;
  }
  
  @Override
  public <T> DBResult<T> read(DBPriority priority, ReadTransaction<T> transaction) {
    return getCurrentDB().read(priority, transaction);
  }

  @Override
  public <T> DBResult<T> write(DBPriority priority, WriteTransaction<T> transaction) {
    return getCurrentDB().write(priority, transaction);
  }

  @Override
  public void addListener(Lifespan lifespan, DBListener listener) {
    getCurrentDB().addListener(lifespan, listener);
  }

  @Override
  public DBLiveQuery liveQuery(Lifespan lifespan, BoolExpr<DP> expr, DBLiveQuery.Listener listener) {
    return getCurrentDB().liveQuery(lifespan, expr, listener);
  }

  @Override
  public void setLongHousekeepingAllowed(boolean housekeepingAllowed) {
    getCurrentDB().setLongHousekeepingAllowed(housekeepingAllowed);
  }

  @Override
  public void dump(PrintStream writer) throws IOException {
    getCurrentDB().dump(writer);
  }

  @Override
  public void registerTrigger(DBTrigger trigger) {
    getCurrentDB().registerTrigger(trigger);
  }

  @Override
  public UserDataHolder getUserData() {
    return getCurrentDB().getUserData();
  }

  @Override
  public DBFilter filter(BoolExpr<DP> expr) {
    return getCurrentDB().filter(expr);
  }

  @Override
  public <T> DBResult<T> readForeground(ReadTransaction<T> transaction) {
    return getCurrentDB().readForeground(transaction);
  }

  @Override
  public <T> DBResult<T> readBackground(ReadTransaction<T> transaction) {
    return getCurrentDB().readBackground(transaction);
  }

  @Override
  public <T> DBResult<T> writeBackground(WriteTransaction<T> transaction) {
    return getCurrentDB().writeBackground(transaction);
  }

  @Override
  public <T> DBResult<T> writeForeground(WriteTransaction<T> transaction) {
    return getCurrentDB().writeForeground(transaction);
  }

  @Override
  public void dump(String filename) {
    getCurrentDB().dump(filename);
  }

  @Override
  public String dumpString() {
    return getCurrentDB().dumpString();
  }

  @Override
  public boolean isDbThread() {
    return getCurrentDB().isDbThread();
  }
}
