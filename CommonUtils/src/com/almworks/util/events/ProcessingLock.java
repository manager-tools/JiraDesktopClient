package com.almworks.util.events;

import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.ExceptionUtil;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class ProcessingLock {
  protected static final boolean DEBUG = Env.getBoolean(GlobalProperties.DEBUG_PROCESSING_LOCKS);
  public static final ProcessingLock DUMMY = new Empty();

  protected final Object myLock;
  private int myLocked;

  private boolean myCorrupt = false;

  /**
   * Used for debugging
   */
  private final Map<Object, Throwable> __owners = DEBUG ? Collections15.<Object, Throwable>linkedHashMap() : null;

  public ProcessingLock(@Nullable Object initialOwner, @NotNull Object lock) {
    myLock = lock;
    myLocked = 1;
    if (DEBUG) {
      debug("created lock (" + initialOwner + ") " + this);
      if (initialOwner != null) {
        __owners.put(initialOwner, new Throwable());
      }
    }
  }

  protected void debug(String msg) {
    assert DEBUG : this + " " + msg;
    Log.debug(" -@-   " + msg);
  }

  protected void warn(String msg) {
    Log.warn(" -@-   " + msg);
  }

  @ThreadSafe
  public void lock(@Nullable Object owner) {
    synchronized (myLock) {
      if (myLocked <= 0) {
        assert myCorrupt : this;
        return;
      }
      myLocked++;
      if (DEBUG && owner != null) {
        Throwable trace = new Throwable();
        Throwable oldValue = __owners.put(owner, trace);
        if (oldValue != null) {
          String msg = owner + "\n\nOLD:\n " + ExceptionUtil.getStacktrace(oldValue) + "\n\nNEW:\n" +
            ExceptionUtil.getStacktrace(trace);
          assert false : msg;
          warn("expunged lock owner " + msg);
        }
      }
    }
    if (DEBUG) {
      debug("locked " + owner + " " + this);
    }
  }

  @ThreadSafe
  public void release(Object owner) {
    synchronized (myLock) {
      if (myLocked <= 0) {
        assert myCorrupt : this;
        return;
      }
      myLocked--;
      if (DEBUG && owner != null) {
        Throwable trace = __owners.remove(owner);
        if (trace == null) {
          assert false : owner;
          warn("no trace for lock owner " + owner);
        }
      }
      if (myLocked == 0) {
        myLock.notifyAll();
      }
    }
    if (DEBUG) {
      debug("unlocked " + owner + " " + this);
    }
  }

  public void waitRelease() throws InterruptedException {
    waitRelease(0);
  }

  public boolean waitRelease(long timeout) throws InterruptedException {
    synchronized (myLock) {
      long until = timeout > 0 ? System.currentTimeMillis() + timeout : Long.MAX_VALUE;
      while (true) {
        if (myLocked <= 0)
          return true;
        long time = System.currentTimeMillis();
        if (time >= until)
          return false;
        long wait = Math.min(2500, until - time);
        myLock.wait(wait);
      }
    }
  }

  public String toString() {
    return "PL(" + myLocked + ")";
  }

  public Object[] getOwners() {
    if (!DEBUG) {
      return Const.EMPTY_OBJECTS;
    } else {
      synchronized (myLock) {
        return __owners.keySet().toArray();
      }
    }
  }

  public Map<Object, Throwable> getOwnersAndTraces() {
    if (!DEBUG) {
      return Collections15.emptyMap();
    } else {
      synchronized(myLock) {
        LinkedHashMap<Object,Throwable> result = Collections15.linkedHashMap();
        result.putAll(__owners);
        return result;
      }
    }
  }

  public int getLocked() {
    synchronized (myLock) {
      return myLocked;
    }
  }

  /**
   * Makes this instance "corrupt", not tracking validity of operations
   */
  protected void corrupt() {
    synchronized (myLock) {
      myCorrupt = true;
    }
  }


  private static class Empty extends ProcessingLock {
    public Empty() {
      super(Empty.class, Empty.class);
    }

    @ThreadSafe
    public void lock(Object owner) {
    }

    @ThreadSafe
    public void release(Object owner) {
    }

    public void waitRelease() throws InterruptedException {
    }
  }
}
