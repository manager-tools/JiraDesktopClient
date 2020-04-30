package com.almworks.util.threads;

import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.util.List;
import java.util.Set;

public abstract class AbstractJobs<T> {
  private final Set<T> myJobs = Collections15.hashSet();
  protected final Runnable myRunnable = new Runnable() {
    public void run() {
      runJobs();
    }
  };

  protected abstract void execute(T job);
  protected abstract void request(boolean delayed);
  protected abstract void gate(Runnable runnable);
  protected abstract void abortBottleneck();
  
  public void addJob(T job) {
    addJob(job, false);
  }

  @ThreadSafe
  public void addJobDelayed(T job) {
    addJob(job, true);
  }

  public void addJob(T job, boolean delayed) {
    synchronized (myJobs) {
      myJobs.add(job);
    }
    request(delayed);
  }

  public boolean removeJob(T job) {
    synchronized (myJobs) {
      return myJobs.remove(job);
    }
  }

  private void runJobs() {
    List<T> jobs;
    synchronized (myJobs) {
      if (myJobs.isEmpty())
        return;
      jobs = Collections15.arrayList(myJobs);
      myJobs.clear();
    }
    executeJobs(jobs);
  }

  private void executeJobs(final List<T> jobs) {
    for (T job : jobs) {
      try {
        execute(job);
      } catch (Exception e) {
        Log.error(e);
      }
    }
  }


  /**
   * Stops all jobs
   */
  public final void abort() {
    abortBottleneck();
    synchronized(myJobs) {
      myJobs.clear();
    }
  }

  public void executeJobsNow() {
    abortBottleneck();
    gate(myRunnable);
  }
}
