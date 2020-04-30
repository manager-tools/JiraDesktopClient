package com.almworks.spi.provider;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.engine.util.SyncNotAllowedException;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.progress.Progress;
import com.almworks.util.threads.InterruptableRunnable;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractConnectorTask extends AbstractSyncTask {
  private final String myQueryName;
  private final AtomicReference<Procedure<SyncTask>> myRunFinally;

  public AbstractConnectorTask(ComponentContainer container, String queryName, Procedure<SyncTask> runFinally) {
    super(container);
    myQueryName = queryName;
    myRunFinally = new AtomicReference<Procedure<SyncTask>>(runFinally);
  }

  public String getTaskName() {
    return L.progress("Synchronizing " + Terms.query + " " + myQueryName);
  }

  @Override
  public AbstractConnection2 getConnection() {
    return (AbstractConnection2) super.getConnection();
  }

  protected void executeTask() {
    myState.setValue(State.WORKING);
    getConnection().subscribeToTaskUntilFinalState(this);
    doLoad();
  }

  private void doLoad() {
    final Progress progress = new Progress("Download");
    myProgress.delegate(progress);
    ThreadGate.executeLong(this, new InterruptableRunnable() {
      @Override
      public void run() throws InterruptedException {
        long lastIcn = 0;
        try {
          lastIcn = doLongLoad(progress);
        } catch (InterruptedException e) {
          progress.addError("Cancelled");
          Thread.currentThread().interrupt();
        } catch (CancelledException e) {
          progress.addError("Cancelled");
        } catch (ConnectionNotConfiguredException e) {
          progress.addError(e.getLocalizedMessage());
        } catch (SyncNotAllowedException e) {
          // ignoring
        } catch (ConnectorException e) {
          progress.addError(e.getMediumDescription());
          notifyException(e);
        } finally {
          progress.setDone();
          onDone(lastIcn);
        }
      }
    });
  }

  protected abstract long doLongLoad(Progress progress) throws ConnectorException, ConnectionNotConfiguredException, SyncNotAllowedException, InterruptedException;

  protected void onDone(long lastICN) {
    myState.commitValue(State.WORKING, State.DONE);
    myLastCommittedCN.setValue(lastICN);
    detach();
    Procedure<SyncTask> procedure = myRunFinally.getAndSet(null);
    if (procedure != null) procedure.invoke(this);
  }
}
