package com.almworks.util.threads;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.util.List;

public class QueueBottleneck {
  private final List<Runnable> myList = Collections15.arrayList();
  private final Bottleneck myBottleneck;

  public QueueBottleneck(long minimumPeriod, ThreadGate gate) {
    myBottleneck = new Bottleneck(minimumPeriod, gate, new Runnable() {
      public void run() {
        runQueue();
      }
    });
  }

  public void run(Runnable runnable) {
    synchronized (myList) {
      myList.add(runnable);
    }
    myBottleneck.run();
  }

  private void runQueue() {
    Runnable[] runnables;
    synchronized (myList) {
      int size = myList.size();
      if (size == 0)
        return;
      runnables = myList.toArray(new Runnable[size]);
      myList.clear();
    }
    for (int i = 0; i < runnables.length; i++) {
      Runnable runnable = runnables[i];
      try {
        runnable.run();
      } catch (Throwable e) {
        handleException(runnable, e);
      }
    }
  }

  protected void handleException(Runnable runnable, Throwable e) {
    Log.error(e);
  }
}
