package com.almworks.jira.provider3.app.sync;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.SyncTask;
import com.almworks.jira.provider3.sync.ConnectorOperation;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import org.almworks.util.ExceptionUtil;

import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseOperation implements ConnectorOperation {
  private final AtomicReference<Boolean> mySuccessfullyDone = new AtomicReference<Boolean>(null);
  protected final ProgressInfo myProgress;
  private ConnectorException myError;

  protected BaseOperation(ProgressInfo progress) {
    myProgress = progress;
  }

  @Override
  public void onCancelled() {
    myProgress.addError("Cancelled");
    mySuccessfullyDone.compareAndSet(null, false);
  }

  @Override
  public void onCompleted(SyncTask.State result) {
    myProgress.setDone();
    mySuccessfullyDone.compareAndSet(null, result.isSuccessful());
  }

  @Override
  public void onError(ConnectorException e) {
    myProgress.addError(e.getMediumDescription());
    myError = e;
    LogHelper.warning(e);
    mySuccessfullyDone.compareAndSet(null, false);
  }

  public abstract void perform(RestSession session) throws ConnectorException;

  protected final Boolean isSuccessful() {
    return mySuccessfullyDone.get();
  }

  protected final void maybeThrowError() throws ConnectorException {
    ExceptionUtil.maybeThrow(myError);
  }
}
