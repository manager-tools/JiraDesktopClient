package org.almworks.util.detach;

import org.jetbrains.annotations.NotNull;

/**
 * Lifecycle is an utility class that helps to manage lifespans that start and end periodically. Lifecycle
 * may be asked to provide current lifespan. It also may be asked to "cycle", that is finish the last provided
 * lifespan (detaching all clients of that lifespan) and start a new lifespan.
 * <p/>
 * Lifecycle can be in one of three statuses that define the behavior of other methods:
 * <dl>
 * <dt>cycle not started</dt>
 * <dd>Lifecycle is idle, {@link #lifespan()} will return {@link Lifespan#NEVER}, {@link #cycleStart} will
 * start the cycle</dd>
 * <p/>
 * <dt>cycle started</dt>
 * <dd>Lifecycle is working, {@link #lifespan()} will return current lifespan, {@link #cycleEnd} will stop
 * the cycle</dd>
 * <p/>
 * <dt>disposed</dt>
 * <dd>Lifecycle is disposed, {@link #lifespan()} will return  {@link Lifespan#NEVER},  {@link #cycleStart}
 * will have no effect</dd>
 * </dl>
 * <p/>
 * In most cases, there's no need to bother about these states. Main methods are {@link #lifespan()} that gives
 * a lifespan to add to and {@link #cycle()}, that will end the last lifespan and start the new, leaving lifecycle
 * in "cycle started" state.
 * <p/>
 * The class is thread safe.
 */
public class Lifecycle {
  private DetachComposite myCurrentLifespan;
  private boolean myStarted;
  private boolean myDisposed;

  /**
   * Default constructor will place Lifecycle into "cycle started" state.
   */
  public Lifecycle() {
    this(true);
  }

  /**
   * A constructor for sophisticated cases (if started = false) where the client will call {@link #cycleStart()} later.
   *
   * @param started false means that lifecycle will be in "cycle not started" state initially
   */
  public Lifecycle(boolean started) {
    myStarted = started;
  }

  /**
   * Returns current lifespan. If lifecycle is disposed or not started, returns dummy lifecycle that will
   * immediately call detach on any add.
   *
   * @return current lifecycle
   */
  @NotNull
  public Lifespan lifespan() {
    synchronized (this) {
      if (myDisposed || !myStarted) {
        return Lifespan.NEVER;
      }
      if (myCurrentLifespan == null) {
        myCurrentLifespan = new DetachComposite();
      }
      return myCurrentLifespan;
    }
  }

  /**
   * Start the cycle. After cycle is started, {@link #lifespan()} will return a working lifespan that will be
   * ended with {@link #cycleEnd()} is called.
   *
   * @return true if lifecycle state has been changed
   */
  public boolean cycleStart() {
    boolean changed = false;
    synchronized (this) {
      if (!myDisposed) {
        changed = !myStarted;
        myStarted = true;
      }
    }
    return changed;
  }

  /**
   * Stops the cycle and ends last lifespan. All detaches that has been added to the last {@link #lifespan()}
   * will be detached.
   *
   * @return true if lifecycle state has been changed
   */
  public boolean cycleEnd() {
    return cycleEndImpl(null);
  }

  private boolean cycleEndImpl(DetachComposite assumedCurrentLifespan) {
    boolean changed;
    DetachComposite life;
    synchronized (this) {
      assert myStarted || myCurrentLifespan == null;
      if (assumedCurrentLifespan != null && myCurrentLifespan != assumedCurrentLifespan) {
        return false;
      }
      life = myCurrentLifespan;
      myCurrentLifespan = null;
      changed = myStarted;
      myStarted = false;
    }
    if (life != null) {
      life.detach();
    }
    return changed;
  }

  /**
   * An utility method for most use cases - stops current lifespan and starts the next one.
   */
  public void cycle() {
    cycleEnd();
    cycleStart();
  }

  /**
   * Disposes this lifecycle. No more active lifespans will be available and currently started lifespan will
   * stop.
   */
  public void dispose() {
    synchronized (this) {
      myDisposed = true;
    }
    cycleEnd();
  }

  /**
   * Checks if the lifecycle is in "cycle started" state.
   *
   * @return true if the lifespan has been started
   */
  public boolean isCycleStarted() {
    synchronized (this) {
      return myStarted;
    }
  }

  /**
   * Checks if the lifecycle is disposed.
   *
   * @return true if this instance has been disposed.
   */
  public boolean isDisposed() {
    synchronized (this) {
      return myDisposed;
    }
  }

  /**
   * Creates an instance of detach that will stop any active cycle. Any number of cycles may happen between
   * this method call and the use of Detach.
   *
   * @return a detach that will call {@link #cycle()} on this instance
   */
  @NotNull
  public Detach getAnyCycleDetach(boolean restart) {
    synchronized (this) {
      return myDisposed ? Detach.NOTHING : new AnyCycleDetach(this, restart);
    }
  }

  public Detach getAnyCycleDetach() {
    return getAnyCycleDetach(true);
  }
  
  /**
   * Creates an instance of detach that will stop currently running cycle. The detach will have no effect
   * if currently active cycle has already ended.
   *
   * @return a detach that will call {@link #cycle()} on this instance, but only if the lifespan is the
   *         same as it was at the moment of this method call
   */
  public Detach getCurrentCycleDetach(boolean restart) {
    synchronized (this) {
      if (myDisposed || !myStarted) {
        return Detach.NOTHING;
      } else {
        if (myCurrentLifespan == null) {
          myCurrentLifespan = new DetachComposite();
        }
        return new CurrentCycleDetach(this, myCurrentLifespan, restart);
      }
    }
  }

  public Detach getCurrentCycleDetach() {
    return getCurrentCycleDetach(true);
  }

  /**
   * Creates an instance of detach that will dispose this lifecycle.
   *
   * @return a detach that will call {@link #dispose()}
   */
  public Detach getDisposeDetach() {
    synchronized (this) {
      return myDisposed ? Detach.NOTHING : new DisposeDetach(this);
    }
  }


  /**
   * A special class for {@link org.almworks.util.detach.Lifecycle#getAnyCycleDetach()}
   */
  private static class AnyCycleDetach extends Detach {
    private final boolean myRestart;
    private Lifecycle myLifecycle;

    public AnyCycleDetach(Lifecycle lifecycle, boolean restart) {
      myLifecycle = lifecycle;
      myRestart = restart;
    }

    protected void doDetach() throws Exception {
      Lifecycle lifecycle = myLifecycle;
      myLifecycle = null;
      if (lifecycle != null) {
        if (myRestart) {
          lifecycle.cycle();
        } else {
          lifecycle.cycleEnd();
        }
      }
    }
  }


  /**
   * A special class for {@link Lifecycle#getDisposeDetach()}
   */
  private static class DisposeDetach extends Detach {
    private Lifecycle myLifecycle;

    public DisposeDetach(Lifecycle lifecycle) {
      myLifecycle = lifecycle;
    }

    protected void doDetach() throws Exception {
      Lifecycle lifecycle = myLifecycle;
      myLifecycle = null;
      if (lifecycle != null) {
        lifecycle.dispose();
      }
    }

    @Override
    public boolean isDetached() {
      Lifecycle lifecycle = myLifecycle;
      return lifecycle == null || lifecycle.isDisposed();
    }
  }


  /**
   * A special class for {@link Lifecycle#getCurrentCycleDetach()}
   */
  private static class CurrentCycleDetach extends Detach {
    private final boolean myRestart;
    private Lifecycle myLifecycle;
    private DetachComposite myCurrentLifespan;

    public CurrentCycleDetach(Lifecycle lifecycle, DetachComposite currentLifespan, boolean restart) {
      myLifecycle = lifecycle;
      myCurrentLifespan = currentLifespan;
      myRestart = restart;
    }

    protected void doDetach() throws Exception {
      Lifecycle lifecycle = myLifecycle;
      DetachComposite lifespan = myCurrentLifespan;
      myLifecycle = null;
      myCurrentLifespan = null;
      if (lifecycle != null && lifespan != null) {
        boolean ended = lifecycle.cycleEndImpl(lifespan);
        if (myRestart && ended) {
          lifecycle.cycleStart();
        }
      }
    }
  }
}
