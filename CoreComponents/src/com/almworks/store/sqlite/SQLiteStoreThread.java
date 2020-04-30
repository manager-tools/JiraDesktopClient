package com.almworks.store.sqlite;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Log;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

class SQLiteStoreThread implements Runnable {
  private final SQLiteConnection myConnection;
  private final AtomicInteger myThreadNumber = new AtomicInteger(0);
  private final ArrayDeque<StoreJob> myQueue = new ArrayDeque<StoreJob>(10);
  private final StoreJob myInitProcedure;
  private Thread myThread = null;
  private boolean myStopRequested = false;

  public SQLiteStoreThread(SQLiteConnection connection, StoreJob initProcedure) {
    myConnection = connection;
    myInitProcedure = initProcedure;
  }

  @ThreadSafe
  public void start() {
    final Thread thread;
    String warning = "";

    synchronized (this) {
      if(myThread != null) {
        thread = null;
        warning = "Another thread";
      } else if(myStopRequested) {
        thread = null;
        warning = "Stop requested";
      } else {
        final int n = myThreadNumber.incrementAndGet();
        thread = new Thread(this, "storeThread-" + n);
        myThread = thread;
      }
    }

    if(thread != null) {
      thread.start();
    } else {
      Log.warn("SQLiteStoreThread.start(): " + warning, new Exception());
    }
  }

  public void stop() {
    synchronized (this) {
      myStopRequested = true;
      notifyAll();
    }
  }

  @ThreadSafe
  public void enqueue(StoreJob job) {
    if (job == null) return;
    boolean cancel = false;
    boolean start = false;
    synchronized (this) {
      if (myStopRequested) cancel = true;
      else {
        if (myThread == null) start = true;
        myQueue.addLast(job);
        notify();
      }
    }
    if (cancel) job.cancelled();
    if (start) start();
  }

  @Override
  public void run() {
    synchronized (this) {
      if (myStopRequested || myThread != Thread.currentThread()) return;
    }
    try {
      runLoop();
    } finally {
      boolean restart;
      synchronized (this) {
        boolean worker = myThread == Thread.currentThread();
        restart = !myStopRequested && worker;
        if (worker) myThread = null;
      }
      if (restart) start();
    }
  }

  private void runLoop() {
    try {
      openConnection();
      if (myInitProcedure != null) performJob(myConnection, myInitProcedure, "Initialize failed");
      while (myConnection.isOpen()) {
        StoreJob job = getNextJob();
        if (job == null) break;
        try {
          job.perform(myConnection);
          job = null;
        } catch (SQLiteException e) {
          String message = "Failed job ";
          boolean retry = job.sqliteFailed(e);
          if (retry) {
            myQueue.addFirst(job);
            message = message + "(retring) ";
          }
          Log.warn(message + job + " DB " + myConnection.getDatabaseFile(), e);
          if (!retry) job.fail(e);
          job = null;
        } finally {
          if (job != null) job.cancelled();
        }
      }
    } catch (SQLiteException e) {
      Log.error("Cannot create store table", e);
    } finally {
      myConnection.dispose();
      StoreJob[] tail;
      synchronized (this) {
        if (myStopRequested) tail = clearQueue();
        else tail = null;
      }
      if (tail != null) for (StoreJob j : tail) j.cancelled();
    }
  }

  private StoreJob getNextJob() {
    StoreJob job;
    Thread thisThread = Thread.currentThread();
    synchronized (this) {
      while (true) {
        if (myThread != thisThread || myStopRequested) {
          job = null;
          break;
        } else {
          job = myQueue.pollFirst();
          if (job != null) break;
        }
        try {
          wait(500);
        } catch (InterruptedException e) {
          myStopRequested = true;
        }
      }
    }
    return job;
  }

  private StoreJob[] clearQueue() {
    assert Thread.holdsLock(this);
    StoreJob[] tail;
    tail = myQueue.toArray(new StoreJob[myQueue.size()]);
    myQueue.clear();
    return tail;
  }

  private void openConnection() throws SQLiteException {
    try {
      myConnection.open();
    } catch (SQLiteException e) {
      synchronized (this) {
        myStopRequested = true;
        myThread = null;
      }
      Log.error("Failed to open SQLite connection");
      throw e;
    }
  }

  static boolean performJob(SQLiteConnection connection, StoreJob job, String failureMessage) {
    Exception failure;
    try {
      job.perform(connection);
      failure = job.getFailedReason();
    } catch (SQLiteException e) {
      failure = e;
    }
    if (failure == null) return true;
    Log.error(failureMessage + " DB " + connection.getDatabaseFile(), failure);
    return false;
  }
}