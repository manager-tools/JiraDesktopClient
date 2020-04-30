package org.almworks.util.detach;

import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.jetbrains.annotations.NotNull;

/**
 * An instance of Detach is a procedure that that disconnects objects in memory so they are possibly collected by GC.
 * <p/>
 * Detach happens only once. After {@link #detach} is called, all subsequent calls will be ignored. This is ensured
 * by thread-safe code. This class also makes sure that detach is not called concurrently or recursively.
 * <p/>
 * Before {@link #detach()} is called, any number of calls to preDetach() method may happen. See {@link #preDetach()}
 * for details.
 * <p/>
 * {@link #detach()} method will catch exceptions from the implementation of {@link #doDetach()} and log or rethrow them
 * according to {@link #onException} method. It is safe to place a call to detach into finally block.
 * <p/>
 * <b>NB:</b> When subclassing Detach, do not forget about implicit final fields in inner and anonymous classes!
 */
public abstract class Detach {
  /**
   * A special value that does nothing. Used as a stub when a Detach instance is required.
   */
  public static final Detach NOTHING = new DetachNothing();

  /**
   * Has detach() finished?
   */
  private boolean myDetached;

  /**
   * If not null, this thread is running the detach now.
   */
  private Thread myDetachingThread;

  /**
   * If not null, this thread is requested by diagnostics code to dump stack trace after detach.
   */
  private Thread myRequestStacktraceThread;

  /**
   * Implement this method to do the job - clear references, remove listeners or call other detaches. Do not call
   * this method directly!
   * <p/>
   * <b>NB: </b> When this method is called, the lock (see {@link #getLock()} is not held.
   *
   * @throws Exception any exception may be thrown, but it is not welcome - it will be logged or rethrown, according
   *                   to {@link #onException(Exception)}.
   */
  protected abstract void doDetach() throws Exception;

  /**
   * Call this method to run the detach. {@link #doDetach()} will be called unless detach has already happened.
   * <p/>
   * NB: Although Detach class is thread-safe, the implementation of {@link #doDetach()} may be not thread-safe. Look
   * for a contract at the point where you get the instance of Detach.
   */
  public final void detach() {
    detachImpl();
  }

  /**
   * Notifies this instance that detach is about to start. Event listeners are advised to ignore events after
   * preDetach is signalled. Composite detaches (instances that own other detach instances) are advised to
   * call preDetach on all owned detaches.
   * <p/>
   * The reason for this method is that when a complex composite detach takes place, it may affect interconnected
   * models. A model, when detached, may fire an event that will come to another model. This other model may not
   * yet be detached from its listeners and the event will affect user data.
   * <p/>
   * <b>NB:</b> If you override this method, you MUST NOT perform detaching or any other activity that could affect
   * other classes. Only a private flag may be set and other Detach instances may be notified with preDetach call.
   * <p/>
   * This method is invoked prior to detach(), in the same thread.
   */
  public void preDetach() {
  }

  /**
   * Returns a lock for running the detach. By default, returns this. May be overridden if an instance of
   * Detach must not be used as a synchronized() parameter.
   * <p/>
   * <b>NB: </b> Override with great care! This method must return the same object throughout whole Detach life.
   *
   * @return an object to be used as synchronization monitor
   */
  @NotNull
  protected Object getLock() {
    return this;
  }

  /**
   * Handles exception that occured while calling {@link #preDetach} or {@link #detach}. By default, logs all
   * exceptions. InterruptedException is handled differently - it is rethrown with {@link RuntimeInterruptedException}.
   * <p/>
   * Override this method to handle exceptions differently.
   *
   * @param e an exception that occured in detach
   */
  protected void onException(@NotNull Exception e) {
    if (e instanceof InterruptedException) {
      throw new RuntimeInterruptedException((InterruptedException) e);
    } else {
      Log.error(e);
    }
  }

  /**
   * Checks if detach has been completed.
   *
   * @return true if detach has completed ({@link #doDetach()} finished)
   */
  public boolean isDetached() {
    synchronized (getLock()) {
      return myDetached;
    }
  }

  /**
   * Checks if detach has ever started. If detach has started and has already finished, returns true also.
   *
   * @return true if detach has been started with {@link #detach} method.
   */
  public boolean isDetachStarted() {
    synchronized (getLock()) {
      return myDetached || myDetachingThread != null;
    }
  }

  /**
   * The implementation of detach method, ensures single execution of {@link #doDetach()} under no lock.
   */
  void detachImpl() {
    Thread thread = Thread.currentThread();
    synchronized (getLock()) {
      if (myDetached) {
        return;
      }
      if (myDetachingThread != null) {
        if (myDetachingThread == thread) {
          Log.warn("recursive detach", new Throwable());
        } else {
          Log.warn("concurrent detach --- (" + thread + ";" + myDetachingThread + ")", new Throwable());
          myRequestStacktraceThread = myDetachingThread;
        }
        return;
      }
      myDetachingThread = thread;
    }
    try {
      try {
        preDetach();
      } catch (Exception e) {
        onException(e);
      }
      try {
        doDetach();
      } catch (Exception e) {
        onException(e);
      }
    } finally {
      synchronized (getLock()) {
        assert thread == myDetachingThread : thread + " " + myDetachingThread;
        myDetachingThread = null;
        myDetached = true;
        if (myRequestStacktraceThread == thread) {
          Log.warn("concurrent detach +++ (" + thread + ")", new Throwable());
        }
        myRequestStacktraceThread = null;
      }
    }
  }


  /**
   * A special class for {@link Detach#NOTHING}.
   */
  private static final class DetachNothing extends Detach {
    private DetachNothing() {
    }

    void detachImpl() {
    }

    protected void doDetach() throws Exception {
    }

    public boolean isDetached() {
      return true;
    }

    public boolean isDetachStarted() {
      return true;
    }
  }
}
