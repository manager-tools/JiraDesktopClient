package com.almworks.items.wrapper;

import com.almworks.items.api.DBResult;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Even if this class does in fact only wrap ItemStorage methods without adding any extra action, it is here to not forget wrap them once they are added.
 * See note for {@link DatabaseWrapper} about compilation breaking.
 * */
class DBResultWrapper<T> implements DBResult<T> {
  private final DBResult<T> myDelegate;

  public DBResultWrapper(DBResult<T> delegate) {
    myDelegate = delegate;
  }

  @Override
  public Modifiable getModifiable() {
    return myDelegate.getModifiable();
  }

  @Override
  @Nullable
  public List<Throwable> getErrors() {
    return myDelegate.getErrors();
  }

  @Override
  public Throwable getError() {
    return myDelegate.getError();
  }

  public DBResult<T> onSuccess(ThreadGate gate, Procedure<T> tProcedure) {
    return myDelegate.onSuccess(gate, tProcedure);
  }

  public DBResult<T> onFailure(ThreadGate gate, Procedure<DBResult<T>> dbResultProcedure) {
    return myDelegate.onFailure(gate, dbResultProcedure);
  }

  public DBResult<T> finallyDo(ThreadGate gate, Procedure<? super T> procedure) {
    return myDelegate.finallyDo(gate, procedure);
  }

  public DBResult<T> finallyDoWithResult(ThreadGate gate, Procedure<? super DBResult<T>> callback) {
    return myDelegate.finallyDoWithResult(gate, callback);
  }

  @Override
  public T waitForCompletion() {
    return myDelegate.waitForCompletion();
  }

  @Override
  public boolean isSuccessful() {
    return myDelegate.isSuccessful();
  }

  @Override
  public long getCommitIcn() {
    return myDelegate.getCommitIcn();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return myDelegate.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return myDelegate.isCancelled();
  }

  @Override
  public boolean isDone() {
    return myDelegate.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return myDelegate.get();
  }

  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return myDelegate.get(timeout, unit);
  }
}
