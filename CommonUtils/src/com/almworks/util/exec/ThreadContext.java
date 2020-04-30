package com.almworks.util.exec;

import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ThreadContext {
  private final Thread myThread;

  /**
   * top frame variable is confined to myThread, thus not synchronized or volatile 
   */
  private ContextFrame myTopFrame;

  /**
   *
   */
  private boolean myLocked;

  ThreadContext() {
    this(null, Thread.currentThread());
  }

  ThreadContext(ContextFrame topFrame, Thread thread) {
    myThread = thread;
    myTopFrame = topFrame;
  }

  public <T> T getObject(Class<T> objectClass, TypedKey<T> key) throws ContextDepthException {
    return checkThread() ? ContextFrame.getObject(myTopFrame, objectClass, key, 0) : null;
  }

  public Thread getThread() {
    return myThread;
  }

  public void push(@NotNull ContextDataProvider provider, boolean inherit, @Nullable String name) {
    if (checkThread()) {
      myTopFrame = new ContextFrame(myTopFrame, provider, inherit, name);
    }
  }

  public void pop() {
    if (checkThread()) {
      ContextFrame topFrame = myTopFrame;
      if (topFrame == null) {
        assert false : "underflow " + Thread.currentThread();
      } else {
        myTopFrame = topFrame.getParentFrame();
      }
    }
  }

  @Nullable
  ContextFrame getTopFrame() {
    return myTopFrame;
  }

  public void restoreTo(int id) {
    if (!checkThread())
      return;
    if (id < 0) {
      myTopFrame = null;
    } else {
      ContextFrame f = myTopFrame;
      while (f != null && f.getId() != id) {
        f = f.getParentFrame();
      }
      if (f != null && f.getId() == id) {
        myTopFrame = f;
      } else {
        assert false : "cannot restore to a non-existing frame " + id + " (" + f + ")";
        Log.warn("cannot restore to a non-existing frame " + id + " (" + f + ")");
      }
    }
  }

  void lock(boolean lock) {
    if (!checkThread())
      return;
    if (myLocked == lock)
      throw new IllegalStateException(this + (myLocked ? " locked" : " unlocked"));
    myLocked = lock;
  }

  private boolean checkThread() {
    if (myThread != Thread.currentThread()) {
      assert false : myThread + " " + Thread.currentThread();
      Log.error("internal error (" + myThread + " " + Thread.currentThread() + ")", new Throwable());
      return false;
    }
    return true;
  }

  public String toString() {
    // may give bad values for other threads!
    return (myThread != Thread.currentThread() ? "~~" : "") + myThread + ":" + myTopFrame + ":" + myLocked;
  }
}