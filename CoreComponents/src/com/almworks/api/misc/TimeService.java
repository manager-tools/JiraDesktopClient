package com.almworks.api.misc;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ValueModel;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.Computable;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.ContextWatcher;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.*;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.picocontainer.Startable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// todo reduce wait to detect time zone change/system time change
public class TimeService implements Startable, Runnable {
  public static final Role<TimeService> ROLE = Role.role(TimeService.class);
  private static final long WAIT_PERIOD = Const.SECOND;

  private final List<Task<?>> myTasks = Collections15.arrayList();
  private Thread myThread;
  private boolean myStop = false;

  public TimeService() {
  }

  @Override
  public void start() {
    synchronized (myTasks) {
      if (myThread != null) return;
      myThread = new Thread(null, this, "TimeService");
      myStop = false;
    }
    myThread.start();
  }

  @Override
  public void stop() {
    synchronized (myTasks) {
      if (myThread != null) myThread.interrupt();
      myStop = true;
      myTasks.notify();
    }
  }

  @Override
  public void run() {
    long lastTime = 0L;
    while (true) {
      Task[] tasks = null;
      synchronized (myTasks) {
        if (myStop) break;
        long now = System.currentTimeMillis();
        boolean backwardTime = lastTime > 0L && now < lastTime;
        lastTime = now;
        if (backwardTime) {
          tasks = myTasks.toArray(new Task[myTasks.size()]);
          myTasks.clear();
        } else if (!myTasks.isEmpty()) {
          Task t = myTasks.get(0);
          if (t.myInvokeTime > now) tasks = null;
          else {
            myTasks.remove(0);
            tasks = new Task[]{t};
          }
        }
        if (tasks == null) {
          try {
            myTasks.wait(WAIT_PERIOD);
            continue;
          } catch (InterruptedException e) {
            continue;
          }
        }
      }
      for (Task task : tasks) task.invoke();
    }
  }

  /**
   * Invokes computation on given time in lifespan is not elapsed earlier. Computation performed within specified thread gate.
   * Task is scheduled for execution not earlier the specified time
   * @param life computation task life. Task never performs if life has ended before time event occurred
   * @param time absolute time when the task has to be started (ms)
   * @param gate gate to execute task
   * @param job the task
   * @return future to control the task execution
   * @see System#currentTimeMillis()
   */
  public <T> Future<T> invokeOn(Lifespan life, long time, ThreadGate gate, Computable<T> job) {
    final Task<T> task = new Task<T>(this, time, job, gate);
    if (life.isEnded()) {
      Log.warn("Task is late " + job);
      task.cancel(false);
      return task;
    }
    synchronized (myTasks) {
      int index = Collections.binarySearch(myTasks, task);
      if (index < 0) index = -index - 1;
      myTasks.add(index, task);
      myTasks.notify();
    }
    if (life != Lifespan.FOREVER)
      life.add(new Detach() {
        @Override
        protected void doDetach() throws Exception {
          task.cancel(false);
        }
      });
    return task;
  }

  public <T> Future<T> awtInvokeIn(Lifespan life, long interval, Computable<T> computable) {
    return invokeIn(life, interval, ThreadGate.AWT, computable);
  }

  public <T> Future<T> invokeIn(Lifespan life, long interval, ThreadGate gate, Computable<T> computable) {
    return invokeOn(life, System.currentTimeMillis() + interval, gate, computable);
  }

  public void notifyOn(final Lifespan life, long time, ThreadGate gate, final ChangeListener listener) {
    invokeOn(life, time, gate, new Computable<Object>() {
      @Override
      public Object compute() {
        listener.onChange();
        return null;
      }
    });
  }

  private void taskCancelled(Task<?> task) {
    synchronized (myTasks) {
      myTasks.remove(task);
    }
  }

  private static class Task<T> implements Comparable<Task<?>>, Runnable, Future<T> {
    private static final int STATE_WAITING = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_DONE = 2;
    private static final int STATE_FAILED = 3;
    private static final int STATE_CANCELLED = 4;
    private final TimeService myService;
    private final long myInvokeTime;
    private final Computable<T> myTask;
    private final ThreadGate myGate;
    private Throwable myThrowable = null;
    private int myState = STATE_WAITING;
    private boolean myCancelled = false;
    private T myResult = null;

    private Task(TimeService service, long invokeTime, Computable<T> task, ThreadGate gate) {
      myService = service;
      myInvokeTime = invokeTime;
      myTask = task;
      myGate = gate;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      boolean doCancel = false;
      synchronized (this) {
        if (myState == STATE_CANCELLED) return true;
        if (myState == STATE_DONE) return false;
        if (myState == STATE_WAITING) {
          myState = STATE_CANCELLED;
          doCancel = true;
        }
        myCancelled = true;
        notify();
      }
      if (doCancel) myService.taskCancelled(this);
      return false;
    }

    @Override
    public boolean isCancelled() {
      synchronized (this) {
        return myCancelled;
      }
    }

    @Override
    public boolean isDone() {
      synchronized (this) {
        if (myCancelled) return true;
        if (myState == STATE_DONE || myState == STATE_FAILED || myState == STATE_CANCELLED) return true;
      }
      return false;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
      try {
        return get(Long.MAX_VALUE);
      } catch (TimeoutException e) {
        Log.error(e);
        return null;
      }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get(System.currentTimeMillis() + unit.toMillis(timeout));
    }

    private T get(long stopWait) throws ExecutionException, TimeoutException, InterruptedException {
      while (true) {
        synchronized (this) {
          if (myThrowable != null) throw new ExecutionException(myThrowable);
          if (myCancelled || myState == STATE_FAILED || myState == STATE_CANCELLED) return null;
          if (myState == STATE_DONE) return myResult;
          long wait = stopWait - System.currentTimeMillis();
          if (wait <= 0) throw new TimeoutException();
          wait(wait);
        }
      }
    }

    @Override
    public int compareTo(Task o) {
      if (o == null) return -1;
      return Util.compareLongs(myInvokeTime, o.myInvokeTime);
    }

    @Override
    public void run() {
      synchronized (this) {
        if (myCancelled) return;
        if (myState != STATE_WAITING) {
          Log.error("Already started " + myState);
          return;
        }
        myState = STATE_RUNNING;
      }
      int finalState = STATE_FAILED;
      T result = null;
      try {
        result = myTask.compute();
        finalState = STATE_DONE;
      } finally {
        synchronized (this) {
          if (!myCancelled) {
            if (myState != STATE_RUNNING) {
              Log.error("Already finished " + myState);
            } else {
              myState = finalState;
              myResult = result;
            }
            notify();
          }
        }
      }
    }

    public void invoke() {
      if (ThreadGate.isRightNow(myGate)) {
        try {
          run();
        } catch (Throwable e) {
          Log.error(e);
        }
      } else myGate.execute(this);
    }
  }

  public static class PeriodicalUpdate implements ContextWatcher.WatchType {
    private static final int MIN_ABSOLUTE_DELAY = 50;
    private static final int MIN_DELAY_FRACTION = 10;
    private final long myPeriod;
    private final TypedKey<ValueModel<Boolean>> myOnFlag;

    public PeriodicalUpdate(long period, TypedKey<ValueModel<Boolean>> onFlag) {
      myPeriod = period;
      myOnFlag = onFlag;
    }

    public static PeriodicalUpdate createSwitchable(long period) {
      return new PeriodicalUpdate(period, TypedKey.<ValueModel<Boolean>>create("timeSwitch"));
    }

    public TypedKey<ValueModel<Boolean>> getOnFlag() {
      return myOnFlag;
    }

    @Override
    public void watch(final UpdateRequest request) {
      TimeService time = request.getSourceObjectOrNull(ROLE);
      if (time == null) {
        request.watchRole(ROLE);
        return;
      }
      final ValueModel<Boolean> flag;
      if (myOnFlag != null) {
        flag = request.getSourceObjectOrNull(myOnFlag);
        if (flag != null) request.updateOnChange(flag);
        else request.watchRole(myOnFlag);
      } else flag = null;
      if (!isOn(flag)) return;
      long delay = myPeriod - System.currentTimeMillis() % myPeriod;
      if (delay < MIN_ABSOLUTE_DELAY || delay < myPeriod / MIN_DELAY_FRACTION) delay += myPeriod;
      time.invokeIn(request.getLifespan(), delay, ThreadGate.AWT, new Computable<Object>() {
        @Override
        public Object compute() {
          if (isOn(flag)) request.getChangeListener().onChange();
          return null;
        }
      });
    }

    private boolean isOn(ValueModel<Boolean> flag) {
      if (flag == null) return true;
      Boolean value = flag.getValue();
      return value != null && value;
    }

    public void install(SimpleAction action) {
      ContextWatcher watcher = action.getWatcher();
      install(watcher);
    }

    public void install(ContextWatcher watcher) {
      watcher.addWatchType(this);
    }

    public void installSwitch(JComponent component, ValueModel<Boolean> switchModel) {
      if (myOnFlag == null) {
        Log.error("Missing switch flag");
        return;
      }
      ConstProvider.addRoleValue(component, myOnFlag, switchModel);
    }
  }
}
