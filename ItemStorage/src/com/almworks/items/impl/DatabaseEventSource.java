package com.almworks.items.impl;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBEvent;
import com.almworks.items.api.DBListener;
import com.almworks.items.api.DBPriority;
import com.almworks.items.impl.dbadapter.DBRead;
import com.almworks.items.impl.sqlite.DatabaseJob;
import com.almworks.items.impl.sqlite.DatabaseManager;
import com.almworks.items.impl.sqlite.QueryProcessor;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Lifespan;

class DatabaseEventSource implements QueryProcessor.Client {
  private final FireEventSupport<DBListener> myEvents = FireEventSupport.createSynchronized(DBListener.class);
  private final Object myLock = new Object();
  private long myIcn = -1;

  public void start(DatabaseManager dbm) {
    dbm.read(DBPriority.FOREGROUND, new DBRead() {
      @Override
      public void read(TransactionContext context) throws SQLiteException {
        long value = context.getIcn();
        synchronized (myLock) {
          assert myIcn == -1 : myIcn + " " + value;
          myIcn = value;
        }
      }

      @Override
      public void dbSuccess() {
      }

      @Override
      public void dbFailure(Throwable throwable) {
      }
    });
    QueryProcessor processor = dbm.getQueryProcessor();
    processor.attach(Lifespan.FOREVER, this);
  }

  public DatabaseJob createJob() {
    return new DatabaseJob() {
      @Override
      protected void dbrun(TransactionContext context) throws Exception {
        notifyListeners(context);
      }

      @Override
      public TransactionType getTransactionType() {
        return TransactionType.READ_ROLLBACK;
      }
    };
  }

  private void notifyListeners(TransactionContext context) throws SQLiteException {
    long icn = context.getIcn();
    long from;
    synchronized (myLock) {
      from = myIcn;
      if (icn > from) {
        myIcn = icn;
      }
    }
    if (myEvents.getListenersCount() == 0)
      return;
    if (from < 0 || from >= icn)
      return;
    LongList change = context.getChangedItemsSorted(from);
    if (change.isEmpty())
      return;
    DBEvent event = DBEvent.create(change);
    DBReaderImpl reader = new DBReaderImpl(context);
    myEvents.getDispatcher().onDatabaseChanged(event, reader);
  }

  public void addListener(Lifespan lifespan, DBListener listener) {
    myEvents.addListener(lifespan, ThreadGate.STRAIGHT, listener);
  }
}
