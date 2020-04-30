package com.almworks.util;

import org.almworks.util.Log;
import org.almworks.util.detach.Detach;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects runnables and executes them one-by-one when method run() is called
 * @author : Dyoma
 */
public class SequenceRunner implements Runnable {
  private final List<Runnable> myRunnables;

  private SequenceRunner(List<Runnable> runnables) {
    myRunnables = runnables;
  }

  public SequenceRunner() {
    this(new ArrayList<Runnable>());
  }
  /**
   * All-list length sequential run() calling
   */
  public void run() {
    Runnable[] runnables;
    synchronized (this) {
      runnables = myRunnables.toArray(new Runnable[myRunnables.size()]);
    }
    for (int i = 0; i < runnables.length; i++) {
      Runnable runnable = runnables[i];
      try {
        runnable.run();
      } catch (Exception e) {
        Log.error(runnable, e);
      }
    }
  }

  public synchronized void add(Runnable runnable) {
    assert runnable != null;
    myRunnables.add(runnable);
  }

  public void addAll(List<? extends Runnable> runnables) {
    myRunnables.addAll(runnables);
  }

  public synchronized void runAndClear() {
    run();
    clear();
  }

  public synchronized void clear() {
    myRunnables.clear();
  }

  public synchronized Detach addReturningDetach(final Runnable runnable) {
    myRunnables.add(runnable);
    return new Detach() {
      protected void doDetach() {
        synchronized (SequenceRunner.this) {
          myRunnables.remove(runnable);
        }
      }
    };
  }

  public synchronized int getSize() {
    return myRunnables.size();
  }

  public SequenceRunner copyAndClear() {
    SequenceRunner copy = new SequenceRunner();
    copy.addAll(myRunnables);
    clear();
    return copy;
  }
}
