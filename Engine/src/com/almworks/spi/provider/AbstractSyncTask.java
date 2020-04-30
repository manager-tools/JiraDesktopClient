package com.almworks.spi.provider;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.*;
import com.almworks.api.http.FeedbackHandler;
import com.almworks.spi.provider.util.BasicHttpAuthHandler;
import com.almworks.util.L;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.model.*;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.properties.Role;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public abstract class AbstractSyncTask extends SimpleModifiable implements SyncTask {
  protected final BasicScalarModel<State> myState = BasicScalarModel.createWithValue(State.NEVER_HAPPENED, true);
  protected final SetHolderModel<SyncProblem> myProblems = new SetHolderModel<SyncProblem>();
  protected final ComponentContainer myContainer;
  protected final CancelFlag myCancelFlag = new CancelFlag();
  protected final BasicScalarModel<Long> myLastCommittedCN = BasicScalarModel.createWithValue(0l, true);
  protected final Progress myProgress;
  protected final FeedbackHandler myFeedbackHandler;
  private final Object myLock = new Object();

  private final Lifecycle myLife = new Lifecycle();

  protected AbstractSyncTask(ComponentContainer container) {
    myContainer = container;
    myProgress = Progress.delegator(getClass().getName());
    myFeedbackHandler = container.instantiate(BasicHttpAuthHandler.class);
  }

  public ScalarModel<State> getState() {
    return myState;
  }

  public SetHolder<SyncProblem> getProblems() {
    return myProblems;
  }

  protected Object getLock() {
    return myLock;
  }

  public Connection getConnection() {
    Connection connection = myContainer.getActor(Connection.class);
    assert connection != null;
    return connection;
  }

  public <T>  T getActor(Role<T> role) {
    return myContainer.getActor(role);
  }

  protected ComponentContainer getContainer() {
    return myContainer;
  }

  public boolean removeProblem(SyncProblem problem) {
    return myProblems.remove(problem);
  }

  public boolean removeItemProblems(long item) {
    boolean removed = false;
    Collection<SyncProblem> problems = myProblems.copyCurrent();
    for (SyncProblem problem : problems) {
      if (problem instanceof AbstractItemProblem) {
        if (item == ((AbstractItemProblem) problem).getItem()) {
          if (removeProblem(problem)) {
            removed = true;
          }
        }
      }
    }
    return removed;
  }

  public void cancel() {
    myCancelFlag.setValue(Boolean.TRUE);
  }

  protected void checkCancelled() throws CancelledException {
    myCancelFlag.checkCancelled();
  }

  public boolean isCancellableState() {
    State state = getState().getValue();
    return state == State.WORKING || state == State.SUSPENDED;
  }

  public long getLastCommittedCN() {
    return myLastCommittedCN.getValue();
  }

  @Nullable
  public ProgressComponentWrapper getProgressComponentWrapper() {
    return null;
  }

  protected void clearProblems() {
    myProblems.remove(myProblems.copyCurrent());
  }

  public ProgressSource getProgressSource() {
    return myProgress;
  }

  protected final FeedbackHandler getFeedbackHandler() {
    return myFeedbackHandler;
  }

  protected final void detach() {
    myLife.cycle();
  }

  public final void startTask() {
    detach();
    init();
    listenConnectionState();
    executeTask();
  }

  protected void listenConnectionState() {
    ScalarModel<ConnectionState> state = getConnection().getState();
    state.getEventSource().addStraightListener(getLifespan(), new ScalarModel.Adapter<ConnectionState>() {
      public void onScalarChanged(ScalarModelEvent<ConnectionState> event) {
        if (event.getNewValue().isDegrading())
          cancel();
      }
    });
  }

  private Lifespan getLifespan() {
    return myLife.lifespan();
  }

  protected abstract void executeTask();

  protected void init() {
    myCancelFlag.setValue(null);
    myProgress.setStarted();
  }

  protected void notifyException(ConnectorException e) {
    String errmsg = L.tooltip("Cannot run query (" + e.getShortDescription() + ")");
    myProgress.addError(errmsg);
    myProgress.setDone();
  }

  protected void notifyInterruptedException(InterruptedException e) {
    myState.setValue(State.CANCELLED);
    throw new RuntimeInterruptedException(e);
  }

//  protected Convertor<List<ProgressSource>, String> createActivityProducer(final String defaultActivity) {
//    Connection connection = getConnection();
//    Engine engine = connection.getConnectionContainer().getActor(Engine.ROLE);
//    assert engine != null : connection;
//    final String name = engine.getConnectionManager().getConnectionName(connection.getConnectionID());
//    return (Convertor) new ProgressAggregator.StringActivityProducer() {
//      public String convert(List<ProgressSource<Object>> progressSources) {
//        return Util.NN(super.convert(progressSources), defaultActivity) + " (" + name + ")";
//      }
//    };
//  }

  protected void notifyTaskDone() {
    try {
      myProgress.setDone();
    } catch (Exception e) {
      Log.error(e);
    }
    try {
      myState.commitValue(State.WORKING, State.DONE);
    } catch (Exception e) {
      Log.error(e);
    }
  }
}
