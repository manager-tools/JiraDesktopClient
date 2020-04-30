package com.almworks.engine;

import com.almworks.api.engine.*;
import com.almworks.api.engine.util.SyncTasksSetUnion;
import com.almworks.api.store.Store;
import com.almworks.api.store.StoreUtils;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.io.persist.PersistableLong;
import com.almworks.util.model.*;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static com.almworks.util.collections.Functional.compose;
import static com.almworks.util.collections.Functional.filter;
import static com.almworks.util.commons.Condition.cond;
import static com.almworks.util.commons.Condition.isEqual;

public class SynchronizerImpl implements Synchronizer {
  private static final String MY_PERSISTENT_ID = "st";

  private final CollectionModel<Connection> myConnections;

  private final BasicScalarModel<Date> myLastSyncTime = BasicScalarModel.createWithValue(new Date(0), true);
  private final PersistableLong myLastSyncTimePersistent = new PersistableLong();
  private final Store myStore;

  private final BasicScalarModel<State> mySyncState = BasicScalarModel.createWithValue(State.IDLE, true);
  private final SyncTasksSetUnion myTasksUnion;
  private final Map<Connection, DetachComposite> myConnectionLives = Collections15.synchronizedHashMap();
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  public SynchronizerImpl(CollectionModel<Connection> connections, Store store) {
    assert connections != null;
    myConnections = connections;
    myStore = store;

    Procedure<Collection<SyncTask>> taskStateChangeListener = new Procedure<Collection<SyncTask>>() {
      @Override
      public void invoke(Collection<SyncTask> allTasks) {
        updateState(allTasks);
      }
    };
    myTasksUnion = SyncTasksSetUnion.createLong(null, taskStateChangeListener);
  }

  @NotNull
  public SetHolder<SyncProblem> getProblems() {
    return myTasksUnion.getProblemsUnion();
  }

  @NotNull
  public Iterable<ItemSyncProblem> getItemProblems(long item) {
    if (item <= 0) return Collections.emptyList();
    Collection<SyncProblem> current = getProblems().copyCurrent();
    return filter(ItemSyncProblem.SELECT.invoke(current), cond(
      compose(isEqual(item).fun(), ItemSyncProblem.TO_ITEM.fun())));
  }

  private void updateState(Collection<SyncTask> allTasks) {
    boolean anyWorking = false;
    boolean anySuspended = false;
    boolean anyFailed = false;
    for (SyncTask syncTask : allTasks) {
      SyncTask.State state = syncTask.getState().getValue();
      if (state == SyncTask.State.FAILED)
        anyFailed = true;
      else if (state == SyncTask.State.WORKING)
        anyWorking = true;
      else if (state == SyncTask.State.SUSPENDED)
        anySuspended = true;
    }
    if (anyWorking)
      mySyncState.setValue(State.WORKING);
    else if (anySuspended)
      mySyncState.setValue(State.SUSPENDED);
    else if (anyFailed)
      mySyncState.setValue(State.FAILED);
    else
      mySyncState.setValue(State.IDLE);

    myModifiable.fireChanged();
  }

  void start() {
    readPersistent();

    myConnections.getEventSource().addStraightListener(new CollectionModel.Adapter<Connection>() {
      public void onScalarsAdded(CollectionModelEvent<Connection> event) {
        for (int i = 0; i < event.size(); i++)
          addConnection(event.get(i));
      }

      public void onScalarsRemoved(CollectionModelEvent<Connection> event) {
        for (int i = 0; i < event.size(); i++)
          removeConnection(event.get(i));
      }
    });
  }

  private void readPersistent() {
    Threads.assertLongOperationsAllowed();
    boolean success = StoreUtils.restorePersistable(myStore, MY_PERSISTENT_ID, myLastSyncTimePersistent);
    if (!success)
      myLastSyncTimePersistent.set(myLastSyncTime.getValue().getTime());
    else
      myLastSyncTime.setValue(new Date(myLastSyncTimePersistent.access()));
  }

  private void writePersistent() {
    Threads.assertLongOperationsAllowed();
    StoreUtils.storePersistable(myStore, MY_PERSISTENT_ID, myLastSyncTimePersistent);
  }

  private void addConnection(Connection connection) {
    DetachComposite detach = new DetachComposite(true);
    myTasksUnion.subscribe(detach, connection.getSyncTasks());
    myConnectionLives.put(connection, detach);
  }

  private void removeConnection(Connection connection) {
    DetachComposite connectionLife = myConnectionLives.remove(connection);
    if (connectionLife != null) {
      connectionLife.detach();
    } else {
      assert false : connection;
    }
  }

  public ScalarModel<Date> getLastSyncTime() {
    return myLastSyncTime;
  }

  public ScalarModel<State> getSyncState() {
    return mySyncState;
  }

  public SetHolder<SyncTask> getTasks() {
    return myTasksUnion.getTasksUnion();
  }

  @Override
  public Modifiable getTasksModifiable() {
    return myModifiable;
  }

  public void synchronize(SyncParameters parameters) {
    for (Connection connection : myConnections.copyCurrent()) {
      connection.getConnectionSynchronizer().synchronize(parameters);
    }
    Date syncTime = new Date();
    myLastSyncTime.setValue(syncTime);
    myLastSyncTimePersistent.set(syncTime.getTime());
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        writePersistent();
      }
    });
  }
}
