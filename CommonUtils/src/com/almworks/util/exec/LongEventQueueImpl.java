package com.almworks.util.exec;

import org.almworks.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.concurrent.LinkedQueue;
import util.concurrent.SynchronizedBoolean;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

public class LongEventQueueImpl extends LongEventQueue {
  // debug
  private static int myInstanceCount = 0;
  private final String myName;
  private boolean myNotAliveWarned;

  // sequencing
  private final TypedKey<Object> myLongEventSequenceKeyKey;
  private final Object myDefaultSequenceKey = new Object();

  // global gates
  private final ImmediateThreadGate myKeylessImmediateGate = new ImmediateLongEventGate(null);
  private final ThreadGate myKeylessOptimalGate = new NonImmediateLongEventGate(null, true);
  private final ThreadGate myKeylessQueuedGate = new NonImmediateLongEventGate(null, false);

  // overworking guys
  private final Map<Object, Worker> myStaleSequenceWorkers = Collections15.hashMap();

  // settings
  private final boolean myDebug;
  private final boolean mySingleThreaded;
  private final int myMaxThreads;
  private final long myDebugStatPeriod;
  private final long myMinIntervalBetweenWarnings;
  private final long myNormalTaskDuration;

  // working guys
  private Worker myMainstreamWorker = null;
  private Watcher myWatcher = null;

  // state
  private boolean myStopped = false;

  // just for stats
  private int myNextWatcherNumber = 1;
  private int myNextWorkerNumber = 1;

  public LongEventQueueImpl(LongEventQueueEnv env) {
    myNormalTaskDuration = env.getNormalTaskDuration();
    myMaxThreads = env.getMaxThreads();
    myDebugStatPeriod = env.getDebugStatPeriod();
    myMinIntervalBetweenWarnings = env.getMinIntervalBetweenWarnings();
    myDebug = env.getDebug();
    mySingleThreaded = env.getSingleThread();

    myName = getNextName();
    myLongEventSequenceKeyKey = TypedKey.create(myName + ".SK");
  }

  public LongEventQueueImpl() {
    this(new LongEventQueueEnv());
  }

  public ImmediateThreadGate getImmediateGate(@Nullable final Object key) {
    if (!checkAlive()) {
      return ThreadGate.STRAIGHT;
    }
    return key == null ? myKeylessImmediateGate : new ImmediateLongEventGate(key);
  }

  public synchronized void shutdownGracefully() {
    if (!checkAlive()) {
      return;
    }
    myStopped = true;
    if (myWatcher != null) {
      log("shutting down gracefully");
      myWatcher.stop();
      myWatcher = null;
    }
    if (myMainstreamWorker != null) {
      myMainstreamWorker.stop();
      myMainstreamWorker = null;
    }
    myStaleSequenceWorkers.clear();
  }

  public synchronized void shutdownImmediately() {
    if (!checkAlive()) {
      return;
    }
    myStopped = true;
    if (myWatcher != null) {
      log("shutting down immediately");
      myWatcher.stop();
      myWatcher = null;
    }
    if (myMainstreamWorker != null) {
      myMainstreamWorker.stopNow();
      myMainstreamWorker = null;
    }
    for (Iterator<Worker> ii = myStaleSequenceWorkers.values().iterator(); ii.hasNext();) {
      ii.next().stopNow();
      ii.remove();
    }
  }

  public synchronized boolean isAlive() {
    return !myStopped;
  }

  protected ThreadGate getNonImmediateGate(@Nullable final Object key, final boolean optimal) {
    if (!checkAlive()) {
      return ThreadGate.STRAIGHT;
    }
    if (key == null) {
      return optimal ? myKeylessOptimalGate : myKeylessQueuedGate;
    } else {
      return new NonImmediateLongEventGate(key, optimal);
    }
  }

  private boolean checkAlive() {
    boolean result = isAlive();
    if (!result) {
      assert false : this;
      if (!myNotAliveWarned) {
        warn("not alive, not serving further requests");
        myNotAliveWarned = true;
      }
    }
    return result;
  }

  public String toString() {
    return myName;
  }

  // todo not synchronized?
  // #1600
  private synchronized void enqueue(Task task) {
    if (myStopped) {
      log("(shutting down)   task " + task + " is not enqueued");
      return;
    }

    try {
      Object key = task.getSequenceKey();
      if (key != null) {
        Worker staleWorker = myStaleSequenceWorkers.get(key);
        if (staleWorker != null) {
          staleWorker.getQueue().put(task);
          return;
        }
      }

      lateStart();
      myMainstreamWorker.getQueue().put(task);
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }

  private synchronized void lateStart() {
    if (myMainstreamWorker == null) {
      myMainstreamWorker = new Worker(new LinkedQueue<Task>());
      log("starting " + myMainstreamWorker);
      myMainstreamWorker.start();
    }
    if (myWatcher == null && !mySingleThreaded) {
      myWatcher = new Watcher();
      log("starting " + myWatcher);
      myWatcher.start();
    }
  }

  private synchronized int getNextWatcherNumber() {
    return myNextWatcherNumber++;
  }

  private synchronized int getNextWorkerNumber() {
    return myNextWorkerNumber++;
  }

  private synchronized void rememberStale(@NotNull Object staleKey, Worker worker) {
    Worker previousWorker = myStaleSequenceWorkers.put(staleKey, worker);
    if (previousWorker != null) {
      assert previousWorker == null : staleKey + " " + previousWorker + " " + worker;
      warn("expunged older stale worker: " + staleKey + " " + previousWorker + " " + worker);
    }
    log(worker + " is stale (" + staleKey + ")");
  }

  private void warn(String message) {
    Log.warn(myName + ": " + message);
  }

  private void log(String message) {
    if (myDebug) {
      Log.debug(myName + ": " + message);
    }
  }

  private synchronized Worker removeStale(@NotNull Object staleKey) {
    Worker result = myStaleSequenceWorkers.remove(staleKey);
    if (result != null) {
      log("removed stale " + result + " for key (" + staleKey + ") " + getStats());
    } else {
      if (!myStopped) {
        warn("! no stale worker for key (" + staleKey + ") " + getStats());
      }
    }
    return result;
  }

  private synchronized String getStats() {
    if (myStopped)
      return "<shutting down>";
    else
      return "[main: " + myMainstreamWorker + "(" + myMainstreamWorker.getTaskCount() + "); " +
        myStaleSequenceWorkers.size() + " stale]";
  }

  private static synchronized String getNextName() {
    return "LongEventQueue#" + (++myInstanceCount);
  }


  private static final class Task {
    public static final Task DIE_WORKER_DIE = new Task(Const.EMPTY_RUNNABLE, Task.class);
    public static final Task STALE_MARKER = new Task(Const.EMPTY_RUNNABLE, Task.class);

    @NotNull
    private final Object mySequenceKey;

    @NotNull
    private final Runnable myRunnable;

    public Task(@NotNull Runnable runnable, @NotNull Object sequenceKey) {
      myRunnable = runnable;
      mySequenceKey = sequenceKey;
    }

    @NotNull
    public Runnable getRunnable() {
      return myRunnable;
    }

    @NotNull
    public Object getSequenceKey() {
      return mySequenceKey;
    }

    public String toString() {
      return "task [" + mySequenceKey + "]:[" + myRunnable + "]";
    }
  }


  private final class Worker implements Runnable {
    private final LinkedQueue<Task> myQueue;
    private final Thread myThread;
    private Object myStaleSequenceKey = null;
    private Task myCurrentlyRunningTask = null;

    private volatile boolean myStoppingNow = false;
    private int myTaskCount = 0;

    private long myLastJobDuration = 0;
    private long myLastJobStart = 0;

    public Worker(@NotNull LinkedQueue<Task> queue) {
      myQueue = queue;
      myThread = ThreadFactory.create("worker#" + getNextWorkerNumber(), this);
    }

    public void run() {
      boolean normalExit = false;
      log(this + " started");
      try {
        while (!myStoppingNow) {
          synchronized (LongEventQueueImpl.this) {
            if (isStale()) {
              if (myQueue.peek() == null) {
                log(this + " is stale (" + myStaleSequenceKey + "), queue is empty, terminating");
                Worker worker = removeStale(myStaleSequenceKey);
                assert worker == this || myStopped;
                break;
              }
            }
          }
          Task task = myQueue.take();
          assert task != null;
          if (task == Task.DIE_WORKER_DIE)
            break;
          prepareThread(task);
          beforeTaskStart(task);

          Context.add(InstanceProvider.instance(task.getSequenceKey(), myLongEventSequenceKeyKey), "LEQI:Worker");
          try {
            Runnable runnable = task.getRunnable();
            runnable.run();
          } catch (Throwable e) {
            if (e instanceof ThreadDeath)
              throw ((ThreadDeath) e);
            Log.error(task, e);
          } finally {
            Context.pop();
            afterTaskFinish(task);
          }

          if (Thread.interrupted()) {
            log(this + " is interrupted, exiting");
            return;
          }
        }
        normalExit = true;
      } catch (InterruptedException e) {
        if (!myStoppingNow)
          throw new RuntimeInterruptedException(e);
        normalExit = true;
      } finally {
        log(this + " is exiting");
        synchronized (LongEventQueueImpl.this) {
          synchronized (this) {
            if (!normalExit) {
              if (!isStale()) {
                if (myMainstreamWorker != this) {
                  assert false : myMainstreamWorker + " " + this;
                } else {
                  // reincarnate
                  warn("main worker is reincarnating");
                  // heritage
                  myMainstreamWorker = new Worker(myQueue);
                  myMainstreamWorker.start();
                }
              }
            }
          }
        }
      }
    }

    public synchronized Task getCurrentlyRunningTask() {
      return myCurrentlyRunningTask;
    }

    public synchronized long getLastJobRunningTime() {
      return isRunningTask() ? System.currentTimeMillis() - myLastJobStart : 0;
    }

    public LinkedQueue<Task> getQueue() {
      return myQueue;
    }

    public synchronized int getTaskCount() {
      return myTaskCount;
    }

    public void setStale(Object sequenceKey) {
      log(this + " is becoming stale (" + sequenceKey + ")");
      myStaleSequenceKey = sequenceKey;
    }

    public void start() {
      myThread.start();
    }

    public void stop() {
      try {
        getQueue().put(Task.DIE_WORKER_DIE);
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
      log("requesting stop of " + this);
    }

    public void stopNow() {
      log("interrupting " + this);
      myStoppingNow = true;
      myThread.interrupt();
    }

    public String toString() {
      return myThread.getName();
    }

    private synchronized void afterTaskFinish(Task task) {
      assert isRunningTask();
      myLastJobDuration = System.currentTimeMillis() - myLastJobStart;
      myCurrentlyRunningTask = null;
      myTaskCount++;
    }

    private synchronized boolean isRunningTask() {
      return myCurrentlyRunningTask != null;
    }

    private synchronized void beforeTaskStart(Task task) {
      assert !isRunningTask() : this;
      myLastJobStart = System.currentTimeMillis();
      myCurrentlyRunningTask = task;
    }

    private synchronized boolean isStale() {
      return myStaleSequenceKey != null;
    }

    private void prepareThread(Task task) {
      Runnable runnable = task.getRunnable();
      if (runnable != null) {
        ClassLoader loader = runnable.getClass().getClassLoader();
        myThread.setContextClassLoader(loader);
      }
    }
  }


  private final class Watcher implements Runnable {
    private final Thread myThread;
    private final long PERIOD = myNormalTaskDuration / 5;
    private boolean myStarted = false;
    private boolean myStopRequested = false;
    private long myLastDebugOutput = 0;
    private long myLastTooManyThreadsWarningOutputTime = 0;

    public Watcher() {
      myThread = ThreadFactory.create("watcher#" + getNextWatcherNumber(), this);
//      myThread.setDaemon(true);
    }

    public void run() {
      log(this + " started");
      try {
        while (!isStopRequested()) {
          long time = System.currentTimeMillis();
          if (myDebugStatPeriod > 0 && time - myLastDebugOutput > myDebugStatPeriod) {
            myLastDebugOutput = time;
            stats();
          }

          watch();

          synchronized (this) {
            wait(PERIOD);
          }
        }
      } catch (InterruptedException e) {
        if (!isStopRequested()) {
          Log.warn(myThread.getName() + " is interrupted", e);
        }
      } finally {
        synchronized (LongEventQueueImpl.this) {
          if (!isStopRequested()) {
            log("reincarnating watcher");
            myWatcher = new Watcher();
            myWatcher.start();
          }
        }
      }
    }

    public synchronized void start() {
      if (myStarted)
        return;
      myThread.start();
      myStarted = true;
    }

    public synchronized void stop() {
      if (!myStarted || myStopRequested)
        return;
      myStopRequested = true;
      myThread.interrupt();
    }

    public String toString() {
      return myThread.getName();
    }

    private synchronized boolean isStopRequested() {
      return myStopRequested;
    }

    private void stats() {
      // todo - add a property
      //Log.debug("thread manager: " + getStats());
    }

    private void watch() {
      try {
        synchronized (LongEventQueueImpl.this) {
          final LongEventQueueImpl.Worker mainWorker = myMainstreamWorker;
          if (mainWorker == null)
            return;
          synchronized (mainWorker) {
            long time = mainWorker.getLastJobRunningTime();
            if (time <= myNormalTaskDuration)
              return;
            log("detected stale " + mainWorker);
            int threadCount = myStaleSequenceWorkers.size() + 1;
            if (threadCount >= myMaxThreads) {
              long now = System.currentTimeMillis();
              if (now > myLastTooManyThreadsWarningOutputTime + myMinIntervalBetweenWarnings) {
                warn("There are currently " + threadCount + " threads distributing events yet events don't " +
                  "get distributed on time. We won't create more threads and user interface slowdown is anticipated.");
                myLastTooManyThreadsWarningOutputTime = now;
              }
              return;
            }
            Task task = mainWorker.getCurrentlyRunningTask();
            if (task == null) {
              assert false : this;
              log(this + ": task = null");
              return;
            }
            Object staleKey = task.getSequenceKey();
            LinkedQueue<Task> queue = mainWorker.getQueue();
            LinkedQueue<Task> newQueue = new LinkedQueue<Task>();
            Task moveTask = null;
            boolean success = false;
            queue.put(Task.STALE_MARKER);
            int totalCount = 0;
            int staleWaitingCount = 0;
            try {
              while (true) {
                try {
                  if (moveTask == null) {
                    moveTask = queue.poll(0);
                    totalCount++;
                  }
                  if (moveTask == null || moveTask == Task.STALE_MARKER)
                    break;
                  Object moveKey = moveTask.getSequenceKey();
                  if (moveKey.equals(staleKey)) {
                    // events with the same key keep waiting
                    queue.put(moveTask);
                    staleWaitingCount++;
                  } else {
                    // others go to the new worker
                    newQueue.put(moveTask);
                  }
                  moveTask = null;
                } catch (InterruptedException e) {
                  log("interrupted while moving tasks, won't stop");
                }
              }
              success = true;
            } finally {
              if (!success)
                Log.error(
                  "Exception thrown while moving tasks, some events may be lost. It's recommended to restart the application.");
            }
            log("extracted " + totalCount + " events from stale " + myMainstreamWorker + "; " + staleWaitingCount +
              " events are for the same key (" + staleKey + ")");
            log("creating new main worker");

            rememberStale(staleKey, myMainstreamWorker);
            myMainstreamWorker.setStale(staleKey);

            myMainstreamWorker = new Worker(newQueue);
            myMainstreamWorker.start();
          }
        }
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }
  }


  private class NonImmediateLongEventGate extends ThreadGate {
    private final Object myKey;
    private final boolean myOptimal;

    public NonImmediateLongEventGate(Object key, boolean optimal) {
      myKey = key;
      myOptimal = optimal;
    }

    protected void gate(Runnable runnable) throws InterruptedException, InvocationTargetException {
      Object key = myKey != null ? myKey : Context.get(myLongEventSequenceKeyKey, myDefaultSequenceKey);
      if (myOptimal && !Context.isAWT() && key.equals(Context.get(myLongEventSequenceKeyKey))) {
        runnable.run();
      } else {
        enqueue(new Task(runnable, key));
      }
    }

    protected Target getTarget() {
      return Target.LONG;
    }

    protected Type getType() {
      return myOptimal ? Type.OPTIMAL : Type.QUEUED;
    }
  }


  private class ImmediateLongEventGate extends ImmediateThreadGate {
    @Nullable
    private final Object myKey;

    public ImmediateLongEventGate(@Nullable Object key) {
      myKey = key;
    }

    protected void gate(final Runnable runnable) throws InterruptedException, InvocationTargetException {
      Object key = myKey != null ? myKey : Context.get(myLongEventSequenceKeyKey, myDefaultSequenceKey);
      if (!Context.isAWT() && key.equals(Context.get(myLongEventSequenceKeyKey))) {
        runnable.run();
      } else {
        final SynchronizedBoolean flag = new SynchronizedBoolean(false);
        Runnable wrapper = new Runnable() {
          public void run() {
            try {
              runnable.run();
            } finally {
              flag.set(true);
            }
          }
        };
        enqueue(new Task(wrapper, key));
        flag.waitForValue(true);
      }
    }

    protected Target getTarget() {
      return Target.LONG;
    }
  }
}
