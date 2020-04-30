package com.almworks.items.impl;

import com.almworks.items.api.DBPriority;
import com.almworks.items.impl.dbadapter.DBProperty;
import com.almworks.items.impl.dbadapter.DBRead;
import com.almworks.items.impl.dbadapter.DBTransaction;
import com.almworks.items.impl.sqlite.DatabaseJob;
import com.almworks.items.impl.sqlite.DatabaseManager;
import com.almworks.items.impl.sqlite.Schema;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Bottleneck;
import org.almworks.util.Log;
import org.almworks.util.detach.Lifespan;

public class DatabaseHouseKeeper {
  private static final DBPriority HOUSEKEEPING = new DBPriority(false, -100);

  // properties contain times when analyze was last run
  private static final DBProperty<Long> ANALYZE_ICN = DBProperty.create("analyzeICN", Long.class);
  private static final DBProperty<Long> ANALYZE_TCID = DBProperty.create("analyzeTCID", Long.class);
  private static final DBProperty<Long> ANALYZE_TIMESTAMP = DBProperty.create("analyzeTimestamp", Long.class);
  private static final DBProperty<Long> ANALYZE_COUNT = DBProperty.create("analyzeCount", Long.class);
  private static final DBProperty<Long> ANALYZE_LAST_DURATION = DBProperty.create("analyzeDuration", Long.class);

  // next analyze may happen only some time after this analyze - use this factor * analyze duration
  private static final long MINIMUM_FACTOR = 100;

  private static final long MINIMUM_TIME_BETWEEN_QUEUE_CAUSED_CHECKS = 600000;

  // analyze after each 200 transactions
  private static final long ANALYZE_TRANSACTION_THRESHOLD = 200;

  private DatabaseManager myDatabase;
  private boolean myEnabled;
  private volatile DatabaseJob myCurrentJob;
  private volatile long myLastCheckTime;

  private final Bottleneck myBottleneck = new Bottleneck(500, ThreadGate.AWT, new Runnable() {
    @Override
    public void run() {
      check();
    }
  });


  @Override
  public String toString() {
    return myDatabase + " DHK";
  }

  private void check() {
    Log.debug(this + " checking");
    final DatabaseManager db;
    try {
      db = db();
    } catch (IllegalStateException e) {
      Log.debug(e);
      return;
    }
    boolean enabled = isEnabled();
    DatabaseJob job = myCurrentJob;
    DatabaseJob.State state = job == null ? null : job.getState();
    if (!enabled) {
      // check & interrupt job
      if (state == DatabaseJob.State.PENDING || state == DatabaseJob.State.RUNNING) {
        Log.debug(this + " cancelling " + job);
        job.cancel();
      }
    } else {
      myLastCheckTime = System.currentTimeMillis();
      if (state != DatabaseJob.State.PENDING && state != DatabaseJob.State.RUNNING) {
        checkAndRun(db);
      }
    }
  }

  private void checkAndRun(final DatabaseManager db) {
    Log.debug(this + " running db check");
    final boolean[] allowed = {false};
    myCurrentJob = db.read(HOUSEKEEPING, new DBRead() {
      @Override
      public void read(TransactionContext context) throws SQLiteException {
        allowed[0] = isHousekeepingRequired(context);
      }

      @Override
      public void dbSuccess() {
        myCurrentJob = null;
        Log.debug(DatabaseHouseKeeper.this + " db check complete (" + allowed[0] + ")");
        if (allowed[0]) {
          runHouseKeeping(db);
        }
      }

      @Override
      public void dbFailure(Throwable throwable) {
        Log.debug(DatabaseHouseKeeper.this + " db check failed", throwable);
        myCurrentJob = null;
      }
    });
  }

  private boolean isHousekeepingRequired(TransactionContext context) throws SQLiteException {
    long icn = context.getIcn();
    long tcid = context.getLongProperty(Schema.TCID, 0);
    long now = System.currentTimeMillis();

    long analyzeIcn = context.getLongProperty(ANALYZE_ICN, 0);
    long analyzeTcid = context.getLongProperty(ANALYZE_TCID, 0);
    long analyzeTime = context.getLongProperty(ANALYZE_TIMESTAMP, 0);
    long analyzeDuration = context.getLongProperty(ANALYZE_LAST_DURATION, 0);

    if (analyzeTime < now && now - analyzeTime < analyzeDuration * MINIMUM_FACTOR) {
      Log.debug(this + " check: time not reached");
      return false;
    }
    if (analyzeTcid < tcid) {
      Log.debug(this + " check: positive by tcid (" + analyzeTcid + "," + tcid + ")");
      return true;
    }
    if (analyzeIcn + ANALYZE_TRANSACTION_THRESHOLD <= icn) {
      Log.debug(this + " check: positive by icn (" + analyzeIcn + "," + icn + ")");
      return true;
    }

    return false;
  }

  private void runHouseKeeping(DatabaseManager db) {
    if (!isEnabled())
      return;
    myCurrentJob = db.write(HOUSEKEEPING, new DBTransaction() {
      @Override
      public void transaction(TransactionContext context) throws Exception {
        Log.warn(DatabaseHouseKeeper.this + " starting analysis");
        long started = System.currentTimeMillis();
        SQLiteStatement st = context.prepare(context.sql().append("ANALYZE"));
        context.addCancellable(st);
        try {
          st.stepThrough();
        } finally {
          context.removeCancellable(st);
        }

        long now = System.currentTimeMillis();
        long duration = now - started;
        Log.warn(DatabaseHouseKeeper.this + " analysis finished in " + duration + "ms");
        
        context.setProperty(ANALYZE_ICN, context.getIcn());
        context.setProperty(ANALYZE_TCID, context.getLongProperty(Schema.TCID, 0));
        context.setProperty(ANALYZE_TIMESTAMP, now);
        context.setProperty(ANALYZE_LAST_DURATION, duration);
        context.setProperty(ANALYZE_COUNT, context.getLongProperty(ANALYZE_COUNT, 0) + 1);
      }

      @Override
      public void dbSuccess() {
        Log.debug(DatabaseHouseKeeper.this + " analysis successful");
        myCurrentJob = null;
      }

      @Override
      public void dbFailure(Throwable throwable) {
        Log.warn(DatabaseHouseKeeper.this + " analysis failed", throwable);
        myCurrentJob = null;
      }
    });
  }

  private synchronized boolean isEnabled() {
    return myEnabled;
  }

  public void setEnabled(boolean enabled) {
    synchronized (this) {
      myEnabled = enabled;
    }
    myBottleneck.request();
  }

  public synchronized void start(DatabaseManager database) {
    if (myDatabase != null) {
      assert false : this;
      return;
    }
    myDatabase = database;
    database.getMainQueueEmptySignal().addChangeListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, new ChangeListener() {
      @Override
      public void onChange() {
        if (System.currentTimeMillis() - myLastCheckTime > MINIMUM_TIME_BETWEEN_QUEUE_CAUSED_CHECKS) {
          check();
        }
      }
    });
  }

  private synchronized DatabaseManager db() {
    DatabaseManager db = myDatabase;
    if (db == null)
      throw new IllegalStateException();
    return db;
  }
}
