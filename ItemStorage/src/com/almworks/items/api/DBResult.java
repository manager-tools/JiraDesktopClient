package com.almworks.items.api;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Future;

public interface DBResult<T> extends Future<T> {
  Modifiable getModifiable();

  @Nullable
  List<Throwable> getErrors();

  Throwable getError();

  /**
   * Invokes procedure when transaction completes successfully
   */
  DBResult<T> onSuccess(ThreadGate gate, Procedure<T> procedure);

  /**
   * Invokes procedure when transaction does not complete, including when it's cancelled (whether before it's
   * started, after it's started, or with an DBOperationCancelledException).
   */
  DBResult<T> onFailure(ThreadGate gate, Procedure<DBResult<T>> procedure);

  /**
   * Invokes procedure when transaction is completed, cancelled, or errored.
   * @param gate
   * @param procedure
   * @return
   */
  DBResult<T> finallyDo(ThreadGate gate, Procedure<? super T> procedure);

  /**
   * Notifies callback when transaction is finished (in both cases when successful and when failed).
   * @param gate defines call thread
   * @param callback notification entry point the DBResult is passed as parameter
   * @return this DBResult
   */
  DBResult<T> finallyDoWithResult(ThreadGate gate, Procedure<? super DBResult<T>> callback);

  /**
   * Like get() but does not throw ExecutionException
   * @return
   */
  T waitForCompletion();

  boolean isSuccessful();

  /**
   * @return icn of successful write transaction or icn of read transaction<br>
   * TBD: Should not be used for failed write transactions. Failed write transaction may be treated as read transaction.
   */
  long getCommitIcn();
}
