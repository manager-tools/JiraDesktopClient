package org.almworks.util.detach;

import org.almworks.util.Collections15;
import org.almworks.util.ExceptionUtil;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * DetachComposite is a container for detaches. It is also the main implementation of {@link Lifespan} interface.
 * DetachComposite forwards {@link #detach} and {@link #preDetach} method calls to all contained detaches.
 * After detach completes, the container is cleared.
 *
 * @see Detach
 */
public class DetachComposite extends Detach implements Lifespan {
  // todo remove
  public static final boolean __DEBUG_CATCH_ADDS_AFTER_DETACH = true;

  /**
   * The first detach is stored in this field. ArrayList is not created until a second detach is added.
   */
  private Detach mySingleDetach;

  /**
   * Other contained detaches.
   */
  private List<Detach> myDetaches;

  /**
   * Is set to true when {@link #preDetach()} is called.
   */
  private boolean myDetachSignalled;

  // todo remove
  private String __detachTrace;

  // todo remove
  public DetachComposite() {
  }

  // todo remove
  public DetachComposite(boolean __noDebug) {
    if (__noDebug)
      __detachTrace = "nodebug";
  }

  /**
   * Creates an optimal detach that will detach all passed parameters.
   *
   * @param detaches a list of detaches
   * @return an instance of Detach that will detach all passed detaches
   */
  @NotNull
  public static Detach create(Detach... detaches) {
    if (detaches == null)
      return NOTHING;
    DetachComposite composite = null;
    Detach single = null;
    for (Detach detach : detaches) {
      if (detach == NOTHING || detach == null) {
        continue;
      }
      if (composite != null) {
        composite.add(detach);
      } else if (single == null) {
        single = detach;
      } else {
        composite = new DetachComposite();
        composite.add(single);
        single = null;
      }
    }
    return composite != null ? composite : (single != null ? single : NOTHING);
  }

  /**
   * Adds another detach to this instance. If DetachComposite is already detached, the parameter's detach method
   * will be called immediately.
   *
   * @param detach a detach to be added
   * @return this
   * @see Lifespan#add(Detach)
   */
  public DetachComposite add(@Nullable Detach detach) {
    if (detach == NOTHING || detach == null)
      return this;
    maybePurge(false);
    boolean detachNow;
    synchronized (getLock()) {
      detachNow = isEnded();
      if (!detachNow) {
        if (mySingleDetach == null) {
          mySingleDetach = detach;
        } else {
          List<Detach> detaches = myDetaches;
          if (detaches == null)
            myDetaches = detaches = Collections15.arrayList(4);
          detaches.add(detach);
        }
      }
    }
    if (detachNow) {
      assert checkDetach(detach);
      detach.detach();
    }
    return this;
  }

  private static final long MIN_PURGE_INTERVAL = 1000;
  private long myLastPurgeTime = 0;
  void maybePurge(boolean force) {
    synchronized (getLock()) {
      if (isEnded()) return;
      if (mySingleDetach == null || myDetaches == null) return;
      if (!force) {
        long now = System.currentTimeMillis();
        if (now - myLastPurgeTime < MIN_PURGE_INTERVAL) return;
        myLastPurgeTime = now;
      }
      if (mySingleDetach.isDetached()) mySingleDetach = null;
      int size = myDetaches.size();
      int store = 0;
      for (int i = 0; i < size; i++) {
        Detach detach = myDetaches.get(i);
        if (detach.isDetached()) continue;
        if (store != i) myDetaches.set(store, detach);
        store++;
      }
      if (mySingleDetach == null && store > 0) {
        mySingleDetach = myDetaches.get(store - 1);
        store--;
      }
      if (store < size) myDetaches.subList(store, size).clear();
    }
  }

  private boolean checkDetach(Detach detach) {
    if (__DEBUG_CATCH_ADDS_AFTER_DETACH && !"nodebug".equals(__detachTrace)) {
      Log.warn("adding " + detach + " after detach\n  >>>>>\n" + __detachTrace + "\n  >>>>>\n");
    }
    return true;
  }

  /**
   * Pass notification to all child detaches (only once).
   */
  public void preDetach() {
    Detach single;
    Object[] multiple;
    synchronized (getLock()) {
      if (myDetachSignalled)
        return;
      myDetachSignalled = true;
      single = mySingleDetach;
      List<Detach> detaches = myDetaches;
      multiple = detaches == null ? null : detaches.toArray();
    }
    if (single != null) {
      try {
        single.preDetach();
      } catch (Exception e) {
        onException(e);
      }
    }
    if (multiple != null) {
      for (Object detachable : multiple) {
        try {
          ((Detach) detachable).preDetach();
        } catch (Exception e) {
          onException(e);
        }
      }
    }
  }

  /**
   * Checks if this instance has been detached or signalled to detach.
   *
   * @return true if this composite's {@link #detach} or {@link #preDetach} method has been called
   */
  public boolean isEnded() {
    synchronized (getLock()) {
      return isDetachStarted() || myDetachSignalled;
    }
  }

  protected void doDetach() {
    assert rememberDetachTrace(); // todo remove
    Detach single;
    Object[] multiple;
    synchronized (getLock()) {
      single = mySingleDetach;
      List<Detach> detaches = myDetaches;
      multiple = detaches == null ? null : detaches.toArray();
    }

    if (single != null) {
      try {
        single.detach();
      } catch (Exception e) {
        onException(e);
      }
    }
    if (multiple != null) {
      for (Object detach : multiple) {
        try {
          ((Detach) detach).detach();
        } catch (Exception e) {
          onException(e);
        }
      }
    }

    synchronized(getLock()) {
      mySingleDetach = null;
      List<Detach> list = myDetaches;
      if (list != null) {
        list.clear();
      }
      myDetaches = null;
    }
  }

  /**
   * for tests
   * todo package-private
   */
  public int count() {
    int c = 0;
    if (mySingleDetach != null)
      c++;
    List<Detach> detaches = myDetaches;
    if (detaches != null)
      c += detaches.size();
    return c;
  }

  // todo remove
  private boolean rememberDetachTrace() {
    if (__DEBUG_CATCH_ADDS_AFTER_DETACH && !"nodebug".equals(__detachTrace)) {
      __detachTrace = ExceptionUtil.getStacktrace(new Throwable());
    }
    return true;
  }
}
