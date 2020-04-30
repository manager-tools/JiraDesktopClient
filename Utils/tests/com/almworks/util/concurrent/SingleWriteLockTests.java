package com.almworks.util.concurrent;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.threads.InterruptableRunnable;
import org.almworks.util.RuntimeInterruptedException;
import util.concurrent.SynchronizedInt;

/**
 * @author dyoma
 */
public class SingleWriteLockTests extends BaseTestCase {
  private final SingleWriteLock myLock = new SingleWriteLock();

  public void test() throws InterruptedException {
    MyNotification first = new MyNotification();
    MyNotification last = new MyNotification();
    myLock.setNotifications(first, last);
    myLock.lockReadThenWrite();
    first.checkNotified(false);
    myLock.readLock().lock();
    first.checkNotified(false);
    last.checkNotified(false);
    myLock.readLock().unlock();
    last.checkNotified(false);
    myLock.unlockReadThenWrite();
    last.checkNotified(false);
    first.checkNotified(false);

    myLock.readLock().lock();
    first.checkNotified(true);
    last.checkNotified(false);
    myLock.readLock().lock();
    first.checkNotified(false);
    last.checkNotified(false);
    myLock.readLock().unlock();
    first.checkNotified(false);
    last.checkNotified(false);
    myLock.readLock().unlock();
    first.checkNotified(false);
    last.checkNotified(true);
  }
  
  public void testReadWhileWrite() throws InterruptedException {
    myLock.lockReadThenWrite();
    myLock.readLock().lock();
    myLock.readLock().unlock();
    myLock.startWrite();
    myLock.readLock().lock();
    myLock.readLock().unlock();
    myLock.stopWrite();
    myLock.unlockReadThenWrite();
  }

  public void testWriteLock() throws InterruptedException {
    final SynchronizedInt state = new SynchronizedInt(0);
    final SynchronizedInt threadState = new SynchronizedInt(0);
    new Thread() {
      public void run() {
        try {
          state.waitForValue(1, 0);
          myLock.readLock().lock();
          System.out.println("L-read");
          threadState.set(1);
          Thread.sleep(50);
          threadState.set(3);
          assertEquals(1, state.get());
          System.out.println("before U-read");
          myLock.readLock().unlock();
          System.out.println("U-read");
          state.waitForValue(2, 0);
          myLock.readLock().lock();
          threadState.set(4);
          myLock.readLock().unlock();
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
      }
    }.start();
    myLock.lockReadThenWrite();
    state.set(1);
    threadState.waitForValue(1, 0);
    myLock.startWrite();
    System.out.println("L-write");
    assertEquals(3, threadState.get());
    myLock.stopWrite();
    System.out.println("U-write");
    state.set(2);
    threadState.waitForValue(4, 0);
    myLock.startWrite();
    myLock.stopWrite();
    myLock.unlockReadThenWrite();
  }

  private static class MyNotification implements InterruptableRunnable {
    private boolean myNotified = false;

    public void run() throws InterruptedException {
      myNotified = true;
    }

    public void checkNotified(boolean notified) {
      assertEquals(notified, myNotified);
      myNotified = false;
    }
  }
}
