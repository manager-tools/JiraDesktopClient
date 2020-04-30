package com.almworks.jira.provider3.services.upload.queue;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.util.LogHelper;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

public class UploadQueue {
  private final JiraConnection3 myConnection;
  private final Object myLock = new Object();
  private final LongArray myRequested = new LongArray();
  private final UploadTask3 myTask;
  private final RetryConflicts myConflicts = new RetryConflicts(this);
  private Detach myLife = null;
  private boolean myStopped = false;
  private Thread myThread = null;

  public UploadQueue(JiraConnection3 connection) {
    myConnection = connection;
    myTask = new UploadTask3(connection.getContainer(), this);
    myTask.listenConnectionState();
  }

  public JiraConnection3 getConnection() {
    return myConnection;
  }

  private void ensureStarted() {
    Thread thread;
    Lifespan life;
    synchronized (myLock) {
      if (myStopped) {
        LogHelper.warning("Already stopped");
        return;
      }
      if (myThread != null) return;
      if (myLife == null) {
        DetachComposite lifespan = new DetachComposite();
        life = lifespan;
        myLife = lifespan;
      } else life = null;
      thread = new Thread(new Runnable() {
        @Override
        public void run() {
          runLoop();
        }
      });
      myThread = thread;
    }
    thread.start();
    if (life != null) myConflicts.startListen(life);
  }

  private void runLoop() {
    try {
      while (true) {
        LongList items;
        synchronized (myLock) {
          if (myStopped) return;
          if (myRequested.isEmpty()) {
            myLock.wait(1000);
            continue;
          }
          items = LongArray.copy(myRequested);
          myRequested.clear();
        }
        doUpload(items);
      }
    } catch (InterruptedException e) {
      // exit
    } finally {
      boolean restart;
      synchronized (myLock) {
        myThread = null;
        restart = !myStopped && !myRequested.isEmpty();
      }
      if (restart) ensureStarted();
    }
  }

  private void doUpload(LongList items) {
    myConnection.subscribeToTaskUntilFinalState(myTask);
    myTask.performUpload(items);
  }

  public void addToUpload(LongList items) {
    if (items == null || items.isEmpty()) return;
    synchronized (myLock) {
      if (myStopped) {
        LogHelper.warning("Upload stopped", items);
        return;
      }
      myRequested.addAll(items);
      myLock.notifyAll();
    }
    ensureStarted();
  }

  public void stop() {
    Thread thread;
    Detach detach;
    synchronized (myLock) {
      myStopped = true;
      myLock.notifyAll();
      thread = myThread;
      detach = myLife;
      myLife = null;
    }
    if (thread != null) thread.interrupt();
    if (detach != null) detach.detach();
    myTask.stop();
  }

  public void retryConflicts(@Nullable LongList conflicts) {
    myConflicts.addConflicts(conflicts);
  }
}
