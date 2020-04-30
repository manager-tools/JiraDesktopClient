package com.almworks.api.application.tree;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ItemsPreviewManager {
  private final Object myLock = new Object();

  /**
   * Queue of nodes waiting for being recounted
   */
  private final LinkedHashMap<Object, Procedure2<Lifespan, DBReader>> myQueue = Collections15.linkedHashMap();

  /**
   * When nodes are grabbed for recount, we fill this map with detaches that are used to
   * signal cancellation of recount request.
   */
  private final Map<Object, DetachComposite> myDetachMap = Collections15.hashMap();

  private final Bottleneck myBottleneck;

  // manager has to be activated with setActive() initially
  private boolean myActive = false;

  public ItemsPreviewManager(final Database db) {
    myBottleneck = new Bottleneck(1000, ThreadGate.STRAIGHT, new Runnable() {
      public void run() {
        db.readBackground(new ReadTransaction<Void>() {
          @Override
          public Void transaction(DBReader reader) throws DBOperationCancelledException {
            calculate(reader);
            return null;
          }
        });
      }
    });
  }

  public void setActive(boolean active) {
    synchronized(myLock) {
      if (active != myActive) {
        myActive = active;
        if (active) {
          // activation
          if (!myQueue.isEmpty()) {
            myBottleneck.requestDelayed();
          }
        } 
      }
    }
  }


  @ThreadAWT
  public void schedule(@NotNull Object jobKey, @Nullable Procedure2<Lifespan, DBReader> job, boolean cancelOngoing) {
    DetachComposite cancel;
    synchronized (myLock) {
      if (cancelOngoing) {
        cancel = myDetachMap.remove(jobKey);
        myQueue.remove(jobKey);
      } else {
        cancel = myDetachMap.get(jobKey);
        if (cancel != null && job != null) {
          // calculation is in progress, relax
          return;
        }
      }
      boolean added = job != null && (myQueue.put(jobKey, job) == null);
      if (added && myActive) {
        myBottleneck.request();
      }
    }
    if (cancel != null) {
      // already running - we have to cancel it
      cancel.detach();
    }
  }

  public boolean isJobEnqueued(@NotNull Object jobKey) {
    synchronized (myLock) {
      return myQueue.containsKey(jobKey) || myDetachMap.containsKey(jobKey);
    }
  }

  @ThreadAWT
  public void cancel(@NotNull Object jobKey) {
    DetachComposite cancel;
    synchronized (myLock) {
      cancel = myDetachMap.remove(jobKey);
      myQueue.remove(jobKey);
    }
    if (cancel != null) {
      cancel.detach();
    }
  }

  @CanBlock
  private void calculate(DBReader reader) {
    long start = System.currentTimeMillis();
    int initialSize;
    synchronized (myLock) {
      initialSize = myQueue.size();
    }
    LogHelper.debug("Starting preview computation");
    try {
      doCalculate(reader, start + 500);
    } finally {
      int leftSize;
      synchronized (myLock) {
        leftSize = myQueue.size();
      }
      long now = System.currentTimeMillis();
      LogHelper.debug("Finished preview computation. Queue:", initialSize, "->", leftSize, "Time:", (now - start), "ms");
      if (leftSize > 0) myBottleneck.requestDelayed();
    }
  }

  private void doCalculate(DBReader reader, long stopTime) {
    while (true) {
      long start = System.currentTimeMillis();
      if (start > stopTime) break;
      Object jobKey;
      Procedure2<Lifespan, DBReader> job;
      DetachComposite detach;
      synchronized (myLock) {
        if (!myActive)
          return;
        if (myQueue.isEmpty()) return;
        Iterator<Map.Entry<Object, Procedure2<Lifespan, DBReader>>> it = myQueue.entrySet().iterator();
        if (!it.hasNext()) return;
        Map.Entry<Object, Procedure2<Lifespan, DBReader>> entry = it.next();
        it.remove();
        jobKey = entry.getKey();
        job = entry.getValue();
        detach = new DetachComposite();
        myDetachMap.put(jobKey, detach);
      }
      try {
        if (detach.isEnded()) continue;
        job.invoke(detach, reader);
      } finally {
        try {
          synchronized (myLock) {
            detach = myDetachMap.remove(jobKey);
          }
          if (detach != null) {
            detach.detach();
          }
        } catch (Exception e) {
          Log.error(e);
        }
        LogHelper.debug("Done calculate", System.currentTimeMillis() - start, jobKey);
      }
    }
  }
}
