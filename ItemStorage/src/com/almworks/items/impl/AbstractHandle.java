package com.almworks.items.impl;

import com.almworks.items.api.DBResult;
import com.almworks.items.impl.dbadapter.DBCallback;
import com.almworks.util.Env;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Lifespan;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractHandle<T> implements DBResult<T>, DBCallback {
  public static final boolean DEBUG_HANDLE = Env.getBoolean("debug.db.job.handle");
  public static final ThreadLocal<AbstractHandle> EXECUTING_HANDLE = new ThreadLocal<AbstractHandle>();

  private final long myTimestamp = System.currentTimeMillis();
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final CountDownLatch myDone = new CountDownLatch(1);
  private final Throwable myCreationStack;

  private List<Throwable> myErrors;
  private T myResult;
  private Boolean mySuccess;
  private long myIcn = -1;

  public AbstractHandle() {
    myCreationStack = DEBUG_HANDLE ? new Throwable() : null;
  }

  public void printCreationStack() {
    if (myCreationStack != null)
      myCreationStack.printStackTrace();
  }

  public static void whoCalled() {
    if (!DEBUG_HANDLE)
      return;
    AbstractHandle handle = EXECUTING_HANDLE.get();
    if (handle != null)
      handle.printCreationStack();
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public synchronized List<Throwable> getErrors() {
    return myErrors == null ? Collections.<Throwable>emptyList() : Collections.unmodifiableList(myErrors);
  }

  public synchronized Throwable getError() {
    return myErrors == null || myErrors.isEmpty() ? null : myErrors.get(0);
  }

  @Override
  public long getCommitIcn() {
    return myIcn;
  }

  public void setIcn(long icn) {
    myIcn = icn;
  }

  public synchronized void addError(Throwable error) {
    List<Throwable> errors = myErrors;
    if (errors == null) {
      myErrors = errors = Collections15.arrayList();
    } else {
      if (errors.contains(error))
        return;
    }
    errors.add(error);
  }

  public void setResult(T result) {
    myResult = result;
  }

  public boolean cancel(boolean mayInterruptIfRunning) {
    // todo
    return false;
  }

  public boolean isCancelled() {
    // todo
    return false;
  }

  public synchronized boolean isDone() {
    return mySuccess != null;
  }

  public void finished(boolean success) {
    try {
      synchronized (this) {
        if (mySuccess != null) {
          assert false : this + " " + mySuccess;
          return;
        }
        mySuccess = success;
        if (!success) {
          myResult = null;
        }
      }
      myDone.countDown();
      myModifiable.fireChanged();
      myModifiable.dispose();
    } catch (Exception e) {
      Log.error(e);
    }
  }

  private synchronized T getResult() {
    return myResult;
  }

  @Override
  public synchronized boolean isSuccessful() {
    return mySuccess != null && mySuccess;
  }

  public DBResult<T> onSuccess(ThreadGate gate, final Procedure<T> procedure) {
    return finally0(gate, new Callback() {
      @Override
      protected void callback() {
        if (isSuccessful()) {
          procedure.invoke(getResult());
        }
      }
    });
  }

  @Override
  public DBResult<T> onFailure(ThreadGate gate, final Procedure<DBResult<T>> procedure) {
    return finally0(gate, new Callback() {
      @Override
      protected void callback() {
        if (!isSuccessful()) {
          procedure.invoke(AbstractHandle.this);
        }
      }
    });
  }


  public DBResult<T> finallyDo(ThreadGate gate, final Procedure<? super T> procedure) {
    return finally0(gate, new Callback() {
      @Override
      protected void callback() {
        procedure.invoke(getResult());
      }
    });
  }

  @Override
  public DBResult<T> finallyDoWithResult(ThreadGate gate, final Procedure<? super DBResult<T>> callback) {
    return finally0(gate, new Callback() {
      @Override
      protected void callback() {
        callback.invoke(AbstractHandle.this);
      }
    });
  }

  private DBResult<T> finally0(ThreadGate gate, Callback callback) {
    boolean run;
    synchronized (this) {
      run = mySuccess != null;
      if (!run) {
        myModifiable.addChangeListener(Lifespan.FOREVER, gate, callback);
      }
    }
    if (run) {
      gate.execute(callback);
    }
    return this;
  }

  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    if (!myDone.await(timeout, unit))
      throw new TimeoutException();
    return getResult();
  }

  public T get() throws InterruptedException, ExecutionException {
    myDone.await();
    return getResult();
  }

  public T waitForCompletion() {
    try {
      return get();
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void dbSuccess() {
    finished(true);
  }

  @Override
  public void dbFailure(Throwable throwable) {
    if (throwable != null)
      addError(throwable);
    finished(false);
  }

  private abstract class Callback implements ChangeListener, Runnable {
    protected abstract void callback();

    public void onChange() {
      callback();
    }

    public void run() {
      onChange();
    }
  }
}
