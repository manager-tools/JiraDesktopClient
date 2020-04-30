package com.almworks.util.concurrent;

import com.almworks.util.TODO;
import com.almworks.util.collections.ArrayIdentityMap;
import com.almworks.util.threads.InterruptableRunnable;
import org.almworks.util.RuntimeInterruptedException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author dyoma
 */
public class SingleWriteLock {
  private final ReadLock myReadLock = new ReadLock();
  private final ArrayIdentityMap<Thread, Counter> myReadLocked = ArrayIdentityMap.create(2);
  private InterruptableRunnable myFirstReadNotification = null;
  private InterruptableRunnable myLastUnlock = null;
  private boolean myEnterNotified = false;
  private Thread myWriter = null;
  private int myWritesReady = 0;
  private Counter myPooledCounter = null;

  public Lock readLock() {
    return myReadLock;
  }

  private void lockRead() throws InterruptedException {
    InterruptableRunnable notification;
    Thread thread;
    synchronized(myReadLocked) {
      thread = Thread.currentThread();
      while (myWriter != null && myWriter != thread && myWritesReady < 0)
        myReadLocked.wait();
      if (myFirstReadNotification != null && myWriter == null && myReadLocked.isEmpty()) {
        assert !myEnterNotified;
        assert myWritesReady == 0;
        lockReadThenWrite();
        notification = myFirstReadNotification;
      } else {
        doReadlock(thread);
        return;
      }
    }
    notification.run();
    synchronized(myReadLocked) {
      myEnterNotified = true;
      assert myWritesReady == 1;
      assert myWriter == thread;
      unlockReadThenWrite();
      assert myWriter == null;
      assert myWritesReady == 0;
      doReadlock(thread);
    }
  }

  private void doReadlock(Thread thread) {
    Counter count = myReadLocked.get(thread);
    if (count == null) {
      if (myPooledCounter != null) {
        count = myPooledCounter;
        myPooledCounter = null;
        count.setValue(0);
      } else
        count = new Counter();
      myReadLocked.put(thread, count);
    }
    count.myValue++;
  }

  private void unlockRead() throws InterruptedException {
    InterruptableRunnable unlock = null;
    Thread thread;
    synchronized (myReadLocked) {
      thread = Thread.currentThread();
      Counter count = myReadLocked.get(thread);
      if (count == null) {
        assert false;
        return;
      }
      //noinspection UnnecessaryLocalVariable
      int iCount = count.getValue();
      assert iCount > 0;
      if (iCount <= 1) {
        if (myReadLocked.size() == 1) {
          if (myWriter == null && myLastUnlock != null) {
            assert myEnterNotified;
            unlock = myLastUnlock;
          } else
            assert !myEnterNotified;
        }
        if (unlock == null) {
          Counter counter = myReadLocked.remove(thread);
          if (myPooledCounter == null)
            myPooledCounter = counter;
        }
      } else
        count.myValue--;
      myReadLocked.notifyAll();
    }
    if (unlock == null)
      return;
    unlock.run();
    synchronized(myReadLocked) {
      assert myReadLocked.size() == 1;
      assert myReadLocked.get(thread).getValue() == 1;
      assert myWriter == null;
      myReadLocked.remove(thread);
      myReadLocked.notifyAll();
      myEnterNotified = false;
    }
  }

  public void setNotifications(InterruptableRunnable firstReadLock, InterruptableRunnable lastReadUnlock) {
    synchronized(myReadLocked) {
      assert !myEnterNotified;
      myFirstReadNotification = firstReadLock;
      myLastUnlock = lastReadUnlock;
    }
  }

  public void lockReadThenWrite() throws InterruptedException {
    synchronized(myReadLocked) {
      Thread thread = Thread.currentThread();
      while (myWriter != null && myWriter != thread)
        myReadLocked.wait();
      if (myWriter == thread)
        assert myWritesReady > 0 : myWritesReady;
      else {
        assert myWritesReady == 0 : myWritesReady;
        myWriter = thread;
      }
      myWritesReady++;
    }
  }

  public void unlockReadThenWrite() {
    synchronized(myReadLocked) {
      assert myWriter == Thread.currentThread();
      if (myWritesReady <= 0) {
        assert false : myWritesReady;
        myWritesReady = 1;
      }
      myWritesReady--;
      if (myWritesReady == 0) {
        myWriter = null;
        myReadLocked.notifyAll();
      }
    }
  }

  public void startWrite() throws InterruptedException {
    synchronized(myReadLocked) {
      Thread thread = Thread.currentThread();
      if (myWriter != thread || myWritesReady <= 0)
        throw new IllegalStateException(String.valueOf(myWritesReady) + " " + myWritesReady + " " + thread);
      if (myReadLocked.containsKey(thread))
        throw new IllegalStateException(thread + " " + myReadLocked.get(thread));
      while(!myReadLocked.isEmpty())
        myReadLocked.wait();
      myWritesReady = -myWritesReady;
    }
  }

  public void stopWrite() {
    synchronized(myReadLocked) {
      Thread thread = Thread.currentThread();
      if (myWriter != thread || myWritesReady >= 0)
        throw new IllegalStateException(String.valueOf(myWritesReady) + " " + myWritesReady + " " + thread);
      myWritesReady = -myWritesReady;
      myReadLocked.notifyAll();
    }
  }

  public boolean isReadAllowed() {
    synchronized(myReadLocked) {
      Thread thread = Thread.currentThread();
      return myWriter == thread || myReadLocked.containsKey(thread);
    }
  }

  public boolean isWriteAllowed() {
    synchronized(myReadLocked) {
      return myWriter == Thread.currentThread();
    }
  }

  private class ReadLock implements Lock {
    public void lock() {
      try {
        lockRead();
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }

    public void lockInterruptibly() throws InterruptedException {
      throw TODO.notImplementedYet();
    }

    public boolean tryLock() {
      assert false;
      return false;
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      assert false;
      return false;
    }

    public void unlock() {
      try {
        unlockRead();
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }

    public Condition newCondition() {
      throw TODO.notImplementedYet();
    }
  }


  public static final Lock READY = new Lock() {
    public void lock() {
    }

    public void lockInterruptibly() throws InterruptedException {
    }

    public boolean tryLock() {
      return true;
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      return true;
    }

    public void unlock() {
    }

    public Condition newCondition() {
      throw TODO.notImplementedYet();
    }
  };

  private static class Counter {
    private int myValue = 0;

    public int getValue() {
      return myValue;
    }

    public String toString() {
      return String.valueOf(myValue);
    }

    public void setValue(int value) {
      myValue = value;
    }
  }
}
