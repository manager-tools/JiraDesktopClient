package com.almworks.items.util.delegate;

import com.almworks.items.api.*;
import com.almworks.util.Pair;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;
import util.concurrent.SynchronizedBoolean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract class TransactionWrapper<T> implements DBResult<T> {
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final Object myLock = new Object();
  private final List<Pair<ThreadGate, Procedure<T>>> myOnSuccess = Collections15.arrayList();
  private final List<Pair<ThreadGate, Procedure<DBResult<T>>>> myOnFailure = Collections15.arrayList();
  private final List<Pair<ThreadGate, ? extends Procedure<? super T>>> myFinally = Collections15.arrayList();
  private final List<Pair<ThreadGate, ? extends Procedure<? super DBResult<T>>>> myFinallyWithResult = Collections15.arrayList();
  private boolean myCancelled = false;
  private DBResult<T> myResult;

  public static <T> TransactionWrapper<T> read(final DBPriority priority, final ReadTransaction<T> transaction) {
    return new TransactionWrapper<T>() {
      @Override
      protected DBResult<T> doEnqueue(Database db, final SynchronizedBoolean canRun) {
        return db.read(priority, new ReadTransaction<T>() {
          @Override
          public T transaction(DBReader reader) throws DBOperationCancelledException {
            try {
              canRun.waitForValue(true);
            } catch (InterruptedException e) {
              throw new RuntimeInterruptedException(e);
            }
            return transaction.transaction(reader);
          }
        });
      }
    };
  }

  public static <T> TransactionWrapper<T> write(final DBPriority priority, final WriteTransaction<T> transaction) {
    return new TransactionWrapper<T>() {
      @Override
      protected DBResult<T> doEnqueue(Database db, final SynchronizedBoolean canRun) {
        return db.write(priority, new WriteTransaction<T>() {
          @Override
          public T transaction(DBWriter writer) throws DBOperationCancelledException {
            try {
              canRun.waitForValue(true);
            } catch (InterruptedException e) {
              throw new RuntimeInterruptedException(e);
            }
            return transaction.transaction(writer);
          }
        });
      }
    };
  }

  public void enqueue(Database db, SynchronizedBoolean canRun, ArrayList<TransactionWrapper<?>> queued) {
    synchronized (myLock) {
      if (myCancelled || myResult != null) return;
      myResult = doEnqueue(db, canRun);
      if (myResult == null) return;
      queued.add(this);
      myLock.notifyAll();
    }
  }

  public void addListeners() {
    DBResult<T> result;
    List<Pair<ThreadGate, Procedure<T>>> onSuccess;
    List<Pair<ThreadGate, Procedure<DBResult<T>>>> onFailure;
    List<Pair<ThreadGate, ? extends Procedure<? super T>>> finallyDo;
    List<Pair<ThreadGate, ? extends Procedure<? super DBResult<T>>>> finallyWithResult;
    synchronized (myLock) {
      result = myResult;
      onSuccess = myOnSuccess;
      onFailure = myOnFailure;
      finallyDo = myFinally;
      finallyWithResult = myFinallyWithResult;
    }
    result.getModifiable().addChangeListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, myModifiable);
    onSuccess.clear();
    onFailure.clear();
    finallyDo.clear();
    finallyWithResult.clear();
  }

  protected abstract DBResult<T> doEnqueue(Database db, SynchronizedBoolean canRun);

  private DBResult<T> waitForResult(long timeout, @Nullable TimeUnit unit) throws TimeoutException {
    DBResult<T> result;
    boolean infinite;
    long end;
    long time;
    if (timeout < 0 || unit == null) {
      infinite = true;
      end = 0;
    } else {
      time = unit.toNanos(timeout);
      end = System.nanoTime() + time;
      infinite = false;
    }
    synchronized (myLock) {
      while (myResult == null && !myCancelled)
        try {
          if (infinite) myLock.wait();
          else {
            long wait = end - System.nanoTime();
            if (wait < 0) throw new TimeoutException();
            else myLock.wait(wait / 1000*1000, (int) (wait % (1000*1000)));
          }
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
      result = myResult;
    }
    return result;
  }

  @Override
  public Modifiable getModifiable() {
    return myModifiable;
  }

  @Override
  public List<Throwable> getErrors() {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) return null;
      result = myResult;
    }
    return result.getErrors();
  }

  @Override
  public Throwable getError() {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) return null;
      result = myResult;
    }
    return result.getError();
  }

  @Override
  public DBResult<T> onSuccess(ThreadGate gate, Procedure<T> procedure) {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) {
        myOnSuccess.add(Pair.create(gate, procedure));
        return this;
      }
      result = myResult;
    }
    return result.onSuccess(gate, procedure);
  }

  @Override
  public DBResult<T> onFailure(ThreadGate gate, Procedure<DBResult<T>> procedure) {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) {
        myOnFailure.add(Pair.create(gate, procedure));
        return this;
      }
      result = myResult;
    }
    return result.onFailure(gate, procedure);
  }

  @Override
  public DBResult<T> finallyDo(ThreadGate gate, Procedure<? super T> procedure) {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) {
        myFinally.add(Pair.create(gate, procedure));
        return this;
      }
      result = myResult;
    }
    return result.finallyDo(gate, procedure);
  }

  @Override
  public DBResult<T> finallyDoWithResult(ThreadGate gate, Procedure<? super DBResult<T>> callback) {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) {
        myFinallyWithResult.add(Pair.create(gate, callback));
        return this;
      }
      result = myResult;
    }
    return result.finallyDoWithResult(gate, callback);
  }

  @Override
  public T waitForCompletion() {
    DBResult<T> result;
    try {
      result = waitForResult(0, null);
    } catch (TimeoutException e) {
      return null;
    }
    return result != null ? result.waitForCompletion() : null;
  }

  @Override
  public boolean isSuccessful() {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) return false;
      result = myResult;
    }
    return result.isSuccessful();
  }

  @Override
  public long getCommitIcn() {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) return -1;
      result = myResult;
    }
    return result.getCommitIcn();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) {
        myCancelled = true;
        myLock.notifyAll();
        return true;
      }
      result = myResult;
    }
    return result.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) {
        return myCancelled;
      }
      result = myResult;
    }
    return result.isCancelled();
  }

  @Override
  public boolean isDone() {
    DBResult<T> result;
    synchronized (myLock) {
      if (myResult == null) {
        return false;
      }
      result = myResult;
    }
    return result.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    DBResult<T> result;
    try {
      result = waitForResult(0, null);
    } catch (TimeoutException e) {
      return null;
    }
    return result != null ? result.get() : null;
  }

  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    DBResult<T> result = waitForResult(timeout, unit);
    return result != null ? result.get() : null;
  }
}
