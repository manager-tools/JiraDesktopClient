package com.almworks.util.exec;

import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import util.concurrent.Synchronized;
import util.concurrent.SynchronizedBoolean;
import util.concurrent.SynchronizedInt;

import java.util.LinkedList;

public class SeparateEventQueueGate extends ThreadGate {
  private static final int TIMEOUT = 500;
  private static final SynchronizedInt myInstanceCounter = new SynchronizedInt(0);

  private final LinkedList<Runnable> myQueue = Collections15.linkedList();
  private final Synchronized<Runnable> myRunning = new Synchronized<Runnable>(null);
  private final SynchronizedBoolean myStopping = new SynchronizedBoolean(false);
  private final SynchronizedInt myTaskCounter;
  private final Thread myThread;
  private final Throwable myCreationStackTrace = new Throwable();

  private boolean myStarted = false;

  public SeparateEventQueueGate(SynchronizedInt taskCounter, String threadName) {
    this(taskCounter, threadName, false);
  }

  public SeparateEventQueueGate(SynchronizedInt taskCounter, String threadName, boolean isDaemon) {
    myTaskCounter = taskCounter;
    if (threadName == null)
      threadName = "seq#" + myInstanceCounter.increment();
    myThread = ThreadFactory.create(threadName, new Runnable() {
      public void run() {
        SeparateEventQueueGate.this.run();
      }
    });
    myThread.setDaemon(isDaemon);
  }

  public SeparateEventQueueGate() {
    this(null, null, false);
  }

  public SeparateEventQueueGate(String threadName) {
    this(null, threadName, false);
  }

  public SeparateEventQueueGate(SynchronizedInt taskCounter) {
    this(taskCounter, null, false);
  }

  public void gate(Runnable runnable) {
    synchronized (myQueue) {
      myQueue.addLast(runnable);
      if (myTaskCounter != null)
        myTaskCounter.increment();
      myQueue.notifyAll();
    }
  }

  public boolean isIdle() {
    synchronized (myQueue) {
      return myQueue.isEmpty() && myRunning.get() == null;
    }
  }

  public SeparateEventQueueGate start() {
    synchronized (myQueue) {
      assert!myStopping.get();
      myThread.start();
      myStarted = true;
      return this;
    }
  }

  public void ensureStarted() {
    if (!myStarted) {
      synchronized (myQueue) {
        if (!myThread.isAlive())
          start();
      }
    }
  }

  public void stop() {
    if (myStopping.commit(false, true)) {
      synchronized (myQueue) {
        if (!myThread.isAlive())
          return;
        if (myTaskCounter != null)
          myTaskCounter.subtract(myQueue.size());
        myQueue.clear();
        myThread.interrupt();
        myStarted = false;
      }
    }
  }

  private void run() {
    while (!myStopping.get()) {
      Runnable job;
      synchronized (myQueue) {
        if (myQueue.isEmpty()) {
          try {
            myQueue.wait(TIMEOUT);
          } catch (InterruptedException e) {
            return;
          }
          continue;
        }
        job = myQueue.removeFirst();
        myRunning.set(job);
      }
      try {
        job.run();
      } catch (RuntimeInterruptedException e) {
        Log.debug(e);
      } catch (Throwable e) {
        Log.error(job, e);
      } finally {
        myRunning.set(null);
        if (myTaskCounter != null)
          myTaskCounter.decrement();
      }
    }
  }

  protected Target getTarget() {
    return Target.LONG;
  }

  protected Type getType() {
    return Type.QUEUED;
  }
}
