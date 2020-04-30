package util.concurrent;

public class SynchronizedLong extends SynchronizedVariable implements Comparable, Cloneable {

  protected long value_;

  public SynchronizedLong(long initialValue) {
    super();
    value_ = initialValue;
  }

  public SynchronizedLong(long initialValue, Object lock) {
    super(lock);
    value_ = initialValue;
  }

  /**
   * Return the current value
   */
  public final long get() {
    synchronized (lock_) {
      return value_;
    }
  }

  /**
   * Set to newValue.
   *
   * @return the old value
   */

  public long set(long newValue) {
    synchronized (lock_) {
      long old = value_;
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
  public boolean commit(long assumedValue, long newValue) {
    synchronized (lock_) {
      boolean success = (assumedValue == value_);
      if (success) {
        value_ = newValue;
        lock_.notifyAll();
      }
      return success;
    }
  }

  /**
   * Atomically swap values with another SynchronizedInt.
   * Uses identityHashCode to avoid deadlock when
   * two SynchronizedInts attempt to simultaneously swap with each other.
   * (Note: Ordering via identyHashCode is not strictly guaranteed
   * by the language specification to return unique, orderable
   * values, but in practice JVMs rely on them being unique.)
   *
   * @return the new value
   */

  public long swap(SynchronizedLong other) {
    if (other == this) return get();
    SynchronizedLong fst = this;
    SynchronizedLong snd = other;
    if (System.identityHashCode(fst) > System.identityHashCode(snd)) {
      fst = other;
      snd = this;
    }
    synchronized (fst.lock_) {
      synchronized (snd.lock_) {
        fst.set(snd.set(fst.get()));
        return get();
      }
    }
  }

  /**
   * Increment the value.
   *
   * @return the new value
   */
  public long increment() {
    synchronized (lock_) {
      lock_.notifyAll();
      return ++value_;
    }
  }

  /**
   * Decrement the value.
   *
   * @return the new value
   */
  public long decrement() {
    synchronized (lock_) {
      lock_.notifyAll();
      return --value_;
    }
  }

  /**
   * Add amount to value (i.e., set value += amount)
   *
   * @return the new value
   */
  public long add(long amount) {
    synchronized (lock_) {
      lock_.notifyAll();
      return value_ += amount;
    }
  }

  /**
   * Subtract amount from value (i.e., set value -= amount)
   *
   * @return the new value
   */
  public long subtract(long amount) {
    synchronized (lock_) {
      lock_.notifyAll();
      return value_ -= amount;
    }
  }

  /**
   * Multiply value by factor (i.e., set value *= factor)
   *
   * @return the new value
   */
  public long multiply(long factor) {
    synchronized (lock_) {
      lock_.notifyAll();
      return value_ *= factor;
    }
  }

  /**
   * Divide value by factor (i.e., set value /= factor)
   *
   * @return the new value
   */
  public long divide(long factor) {
    synchronized (lock_) {
      lock_.notifyAll();
      return value_ /= factor;
    }
  }

  /**
   * Set the value to the negative of its old value
   *
   * @return the new value
   */
  public long negate() {
    synchronized (lock_) {
      lock_.notifyAll();
      value_ = -value_;
      return value_;
    }
  }

  /**
   * Set the value to its complement
   *
   * @return the new value
   */
  public long complement() {
    synchronized (lock_) {
      lock_.notifyAll();
      value_ = ~value_;
      return value_;
    }
  }

  /**
   * Set value to value &amp; b.
   *
   * @return the new value
   */
  public long and(long b) {
    synchronized (lock_) {
      lock_.notifyAll();
      value_ = value_ & b;
      return value_;
    }
  }

  /**
   * Set value to value | b.
   *
   * @return the new value
   */
  public long or(long b) {
    synchronized (lock_) {
      lock_.notifyAll();
      value_ = value_ | b;
      return value_;
    }
  }


  /**
   * Set value to value ^ b.
   *
   * @return the new value
   */
  public long xor(long b) {
    synchronized (lock_) {
      lock_.notifyAll();
      value_ = value_ ^ b;
      return value_;
    }
  }

  public int compareTo(long other) {
    long val = get();
    return (val < other) ? -1 : (val == other) ? 0 : 1;
  }

  public int compareTo(SynchronizedLong other) {
    return compareTo(other.get());
  }

  public int compareTo(Object other) {
    return compareTo((SynchronizedLong) other);
  }

  public boolean equals(Object other) {
    if (other != null &&
      other instanceof SynchronizedLong)
      return get() == ((SynchronizedLong) other).get();
    else
      return false;
  }

  public int hashCode() {
    long value = get();
    return (int)(value ^ (value >>> 32));
  }

  public String toString() {
    return String.valueOf(get());
  }


  public long compareAndIncrement(long assumedValue, long increment) {
    synchronized (lock_) {
      long result = value_;
      if (value_ == assumedValue) {
        lock_.notifyAll();
        value_ += increment;
      }
      return result;
    }
  }

  public boolean waitForValue(final long value, long timeout) throws InterruptedException {
    return waitForCondition(new Checker() {
      public boolean check() {
        return value_ == value;
      }
    }, timeout);
  }

  public long waitForValueInRange(final long minValue, final long maxValue, long timeout) throws InterruptedException {
    synchronized (lock_) {
      waitForCondition(new Checker() {
        public boolean check() {
          return value_ >= minValue && value_ <= maxValue;
        }
      }, timeout);
      return value_;
    }
  }
}

