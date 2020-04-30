/*
  File: Synchronized.java

  Originally written by Doug Lea and released into the public domain.
  This may be used for any purposes whatsoever without acknowledgment.
  Thanks for the assistance and support of Sun Microsystems Labs,
  and everyone contributing, testing, and using this code.

  History:
  Date       Who                What
  11Jun1998  dl               Create public version
*/

package util.concurrent;

import com.almworks.util.commons.Condition;


/**
 * A simple class maintaining a single reference variable that
 * is always accessed and updated under synchronization.
 * <p/>
 * <p>[<a href="http://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 */

public class Synchronized <T> extends SynchronizedVariable {
  /**
   * The maintained reference *
   */
  protected T value_;
  private static final long SAFE_PAUSE = 1000;

  /**
   * Create a Synchronized initially holding the given reference
   * and using its own internal lock.
   */
  public Synchronized(T initialValue) {
    super();
    value_ = initialValue;
  }

  /**
   * Make a new Synchronized with the given initial value,
   * and using the supplied lock.
   */
  public Synchronized(T initialValue, Object lock) {
    super(lock);
    value_ = initialValue;
  }

  /**
   * Return the current value
   */
  public final T get() {
    synchronized (lock_) {
      return value_;
    }
  }

  /**
   * Set to newValue.
   *
   * @return the old value
   */

  public T set(T newValue) {
    synchronized (lock_) {
      T old = value_;
      value_ = newValue;
      lock_.notifyAll();
      return old;
    }
  }

  /**
   * Set value to newValue only if it is currently assumedValue.
   *
   * @return true if successful
   */
  public boolean commit(T assumedValue, T newValue) {
    synchronized (lock_) {
      boolean success = (assumedValue == value_);
      if (success) value_ = newValue;
      lock_.notifyAll();
      return success;
    }
  }


  /**
   * Atomically swap values with another Synchronized.
   * Uses identityHashCode to avoid deadlock when
   * two SynchronizedRefs attempt to simultaneously swap with each other.
   * (Note: Ordering via identyHashCode is not strictly guaranteed
   * by the language specification to return unique, orderable
   * values, but in practice JVMs rely on them being unique.)
   *
   * @return the new value
   */

/*
  public T swap(Synchronized<T> other) {
    if (other == this) return access();
    Synchronized fst = this;
    Synchronized snd = other;
    if (System.identityHashCode(fst) > System.identityHashCode(snd)) {
      fst = other;
      snd = this;
    }
    synchronized (fst.lock_) {
      synchronized (snd.lock_) {
        fst.set(snd.set(fst.access()));
        return access();
      }
    }
  }

*/

  public T waitForNotNull() throws InterruptedException {
    synchronized (lock_) {
      while (value_ == null)
        lock_.wait(SAFE_PAUSE);
      return value_;
    }
  }

  public void waitForNotNull(long timeout) throws InterruptedException {
    waitForCondition(Condition.<T>notNull(), timeout);
  }


  public void waitForValue(T value) throws InterruptedException {
    synchronized (lock_) {
      while (value_ != value)
        lock_.wait();
    }
  }

  public boolean waitForValue(T value, long timeout) throws InterruptedException {
    if (timeout <= 0) {
      waitForValue(value);
      return true;
    }
    long deadline = System.currentTimeMillis() + timeout;
    synchronized (lock_) {
      while (value_ != value) {
        long delay = deadline - System.currentTimeMillis();
        if (delay <= 0)
          return false;
        lock_.wait(delay);
      }
      return true;
    }
  }

  public void waitForCondition(final Condition<T> condition) throws InterruptedException {
    waitForCondition(condition, 0);
  }
  
  public boolean waitForCondition(final Condition<T> condition, long timeout) throws InterruptedException {
    return waitForCondition(new Checker() {
      public boolean check() throws InterruptedException {
        return condition.isAccepted(value_);
      }
    }, timeout);
  }

  public static <T> Synchronized<T> create(T initialValue) {
    return new Synchronized<T>(initialValue);
  }
}
