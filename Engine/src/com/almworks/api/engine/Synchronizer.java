package com.almworks.api.engine;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.SetHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public interface Synchronizer {
  ScalarModel<Date> getLastSyncTime();

  @NotNull
  SetHolder<SyncProblem> getProblems();

  @NotNull
  Iterable<ItemSyncProblem> getItemProblems(long item);

  ScalarModel<State> getSyncState();

  SetHolder<SyncTask> getTasks();

  /** Update is fired each time sync task (dis)appears or changes its state. */
  Modifiable getTasksModifiable();

  /**
   * @param parameters map of parameters, may be null
   */
  void synchronize(SyncParameters parameters);


  public static final class State {
    public static final State IDLE = new State("idle");
    public static final State SUSPENDED = new State("suspended");
    public static final State WORKING = new State("working");
    public static final State FAILED = new State("failed");

    private final String myName;

    private State(String name) {
      myName = name;
    }

    public String getName() {
      return myName;
    }
  }
}
