package com.almworks.util.exec;

import org.almworks.util.Failure;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;

public class SeparateThreadGate extends ThreadGate {
  private final Object myLock = new Object();
  private Runnable myCurrentJob = null;
  private Thread myThread = null;

  private void ensureThreadStarted() {
    if (myThread != null)
      return;
    Runnable runnable = new Runnable() {
      public void run() {
        while (true) {
          synchronized (myLock) {
            if (myCurrentJob == null)
              try {
                myLock.wait();
              } catch (InterruptedException e) {
                Log.error(e);
              }
            if (myThread == null) {
              myLock.notifyAll();
              return;
            }
            if (myCurrentJob == null)
              continue;
            try {
              myCurrentJob.run();
            } catch (Exception e) {
              Log.error(e);
            }
            myCurrentJob = null;
          }
        }
      }
    };
    myThread = new Thread(runnable);
    myThread.start();
  }

  public void gate(Runnable runnable) {
    synchronized (myLock) {
      ensureThreadStarted();
      myCurrentJob = runnable;
      myLock.notifyAll();
    }
  }

  protected void doDetach() {
    stopThread();
  }

  public void stopThread() {
    synchronized (myLock) {
      if (myThread == null)
        return;
      myThread = null;
      myLock.notifyAll();
      try {
        myLock.wait();
      } catch (InterruptedException e) {
        throw new Failure(e);
      }
    }
  }

  public Detach getDetach() {
    return new Detach() {
      protected void doDetach() {
        SeparateThreadGate.this.doDetach();
      }
    };
  }

  protected Target getTarget() {
    return Target.LONG;
  }

  protected Type getType() {
    return Type.QUEUED;
  }
}
