package com.almworks.items.impl.sqlite;

import com.almworks.util.exec.ThreadFactory;
import org.almworks.util.Log;
import util.concurrent.SynchronizedBoolean;

class DatabaseMonitor implements TransactionObserver, Runnable {
  private static final int PERIOD = 2000;

  private final DatabaseQueue myQueue;
  private final QueryProcessor myQueryProcessor;
  private final Thread myThread = ThreadFactory.create("db:monitor:timer", this);
  private final SynchronizedBoolean myStopped = new SynchronizedBoolean(false);

  private long myLastIcn;

  public DatabaseMonitor(DatabaseQueue queue, QueryProcessor queryProcessor) {
    myQueue = queue;
    myQueryProcessor = queryProcessor;
  }

  public void start() {
    if (!myThread.isAlive()) {
      myThread.start();
    }
  }

  public void stop() throws InterruptedException {
    if (!myStopped.commit(false, true))
      return;
    if (myThread.isAlive()) {
      myThread.join(PERIOD * 2);
    }
  }

  @Override
  public void run() {
    try {
      while (true) {
        if (myStopped.waitForValue(true, PERIOD)) return;
        myQueue.execute(new MonitorJob());
      }
    } catch (InterruptedException e) {
      Log.warn(this + " interrupted, exiting", e);
    }
  }

  public void notifyTransaction(long icn) {
    update(icn);
  }

  private void update(long icn) {
    boolean fire;
    synchronized (this) {
      fire = icn > myLastIcn;
      if (fire) {
        myLastIcn = icn;
      }
    }
    if (fire) {
      myQueryProcessor.process();
    }
  }

  private class MonitorJob extends DatabaseJob {
    protected void dbrun(TransactionContext context) throws Exception {
      update(context.getIcn());
    }

    public TransactionType getTransactionType() {
      return TransactionType.READ_ROLLBACK;
    }
  }
}
