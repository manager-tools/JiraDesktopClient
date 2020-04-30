package com.almworks.util.http;

import com.almworks.util.RunnableRE;
import util.concurrent.Synchronized;
import util.concurrent.SynchronizedBoolean;

import java.io.IOException;

/**
 * This class is required only as a workaround for bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6223635
 */
public class InfiniteOperationGuard {
  private int myCounter = 0;

  public synchronized Job execute(RunnableRE<Void, IOException> operation) {
    Job job = new Job(++myCounter, operation);
    job.start();
    return job;
  }

  public class Job {
    private final SynchronizedBoolean myFinished = new SynchronizedBoolean(false);
    private final Thread myThread;
    private final Synchronized<Exception> myException = new Synchronized<Exception>(null);
    private final RunnableRE<Void, IOException> myTask;

    public Job(int count, RunnableRE<Void, IOException> task) {
      myTask = task;
      myThread = new Thread(new Runnable() {
        public void run() {
          executeTask();
        }
      }, "connect-job-" + count);
    }

    private void start() {
      assert !myThread.isAlive();
      myThread.start();
    }

    private void executeTask() {
      try {
        myTask.run();
      } catch (Exception e) {
        myException.set(e);
      }
      myFinished.set(true);
    }

    public SynchronizedBoolean getFinished() {
      return myFinished;
    }

    public void abort() {
      if (myThread.isAlive()) {
        myThread.interrupt();
      }
    }

    public Exception getException() {
      return myException.get();
    }
  }
}
