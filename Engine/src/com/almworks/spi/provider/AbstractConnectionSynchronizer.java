package com.almworks.spi.provider;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.ConnectionSynchronizer;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ProgressComponentWrapper;
import com.almworks.api.engine.SyncParameters;
import com.almworks.api.engine.util.SyncNotAllowedException;
import com.almworks.api.engine.util.SynchronizationProgress;
import com.almworks.items.api.Database;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.InterruptableRunnable;
import com.almworks.util.threads.Threads;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractConnectionSynchronizer extends AbstractSyncTask implements ConnectionSynchronizer {
  private final AtomicBoolean mySyncRunning = new AtomicBoolean(false);
  private ProgressComponentWrapper myProgressComponent;

  public AbstractConnectionSynchronizer(ComponentContainer container) {
    super(container);
  }

  @Override
  public SpecificItemActivity getSpecificActivityForItem(long itemId, @Nullable Integer serverId) {
    return SpecificItemActivity.OTHER;
  }

  protected void executeTask() {
    throw new UnsupportedOperationException();
  }

  public void synchronize(SyncParameters parameters) {
    doSynchronize(parameters, null);
  }

  public Database getDatabase() {
    return myContainer.getActor(Database.ROLE);
  }

  public boolean doSynchronize(final SyncParameters parameters, final Procedure2<Boolean, String> runOnFinish) {
    if (!parameters.isAffectedConnectionShortCheck(getConnection())) {
      return false;
    }
    ThreadGate.executeLong(this, new InterruptableRunnable() {
      public void run() throws InterruptedException {
        if (!parameters.isAffectedConnection(getDatabase(), getConnection())) {
          return;
        }
        if (!mySyncRunning.compareAndSet(false, true)) return;
        try {
          myCancelFlag.setValue(null);
          AbstractConnectionSynchronizer.this.clearProblems();
          myState.setValue(State.WORKING);
          boolean result = false;
          String error = null;
          try {
            Pair<Boolean, String> pair = longSynchronize(parameters);
            if (pair != null) {
              result = pair.getFirst();
              error = pair.getSecond();
            }
          } catch (ConnectorException e) {
            notifyException(e);
            if (myCancelFlag.isCancelled() && !(e instanceof SyncNotAllowedException)) {
              Log.debug("synchronization cancelled", e);
              myState.setValue(State.CANCELLED);
            } else {
              Log.debug("synchronization failed", e);
              myProblems.add(new HttpConnectionProblem(AbstractConnectionSynchronizer.this, e));
              myState.setValue(State.FAILED);
              myProgress.addError(e.getMessage());
              myProgress.setDone();
              myCancelFlag.setValue(true);
              error = e.getMediumDescription();
            }
          } catch (ConnectionNotConfiguredException e) {
            // connection is being closed?
            myState.setValue(State.CANCELLED);
          } finally {
            notifyTaskDone();

            if (runOnFinish != null) {
              try {
                runOnFinish.invoke(result, error);
              } catch (Exception e) {
                // ignore
                Log.debug("runOnFinish", e);
              }
            }
          }
        } finally {
          mySyncRunning.set(false);
        }
      }
    });
    return true;
  }

  @Nullable
  protected abstract Pair<Boolean, String> longSynchronize(SyncParameters parameters)
    throws ConnectionNotConfiguredException, ConnectorException, InterruptedException;

  @Nullable
  public ProgressComponentWrapper getProgressComponentWrapper() {
    Threads.assertAWTThread();
    if(myProgressComponent == null) {
      myProgressComponent = new SynchronizationProgress(myProgress, myState);
    }
    return myProgressComponent;
  }

  public String getTaskName() {
    Engine engine = myContainer.getActor(Engine.ROLE);
    assert engine != null : myContainer;
    return engine.getConnectionManager().getConnectionName(getConnection().getConnectionID());
  }
}
