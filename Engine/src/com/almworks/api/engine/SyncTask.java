package com.almworks.api.engine;

import com.almworks.util.Enumerable;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.SetHolder;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.Nullable;

public interface SyncTask extends Modifiable {
  DataRole<SyncTask> DATA = DataRole.createRole(SyncTask.class);

  void cancel();

  boolean isCancellableState();

  Connection getConnection();

  SetHolder<SyncProblem> getProblems();

  boolean removeProblem(SyncProblem problem);

  @Nullable
  ProgressComponentWrapper getProgressComponentWrapper();

  ProgressSource getProgressSource();

  ScalarModel<State> getState();

  long getLastCommittedCN();

  String getTaskName();

  /** Checks for specific activity of this task in regard of the specified item.<br>
   * {@link SpecificItemActivity#UPLOAD UPLOAD} takes precedence over {@link SpecificItemActivity#DOWNLOAD DOWNLOAD} if the item is involved in both activites.<br>
   */
  @ThreadSafe
  SpecificItemActivity getSpecificActivityForItem(long itemId, @Nullable Integer serverId);


  public static final class State extends Enumerable {
    public static final State CANCELLED = new State("CANCELLED");
    public static final State DONE = new State("DONE");
    public static final State NEVER_HAPPENED = new State("NEVER_HAPPENED");
    public static final State SUSPENDED = new State("SUSPENDED");
    public static final State WORKING = new State("WORKING");
    public static final State FAILED = new State("FAILED");

    private State(String name) {
      super(name);
    }

    public boolean isFinal() {
      return this == CANCELLED || this == DONE || this == FAILED;
    }

    public boolean isSuccessful() {
      return this != CANCELLED && this != FAILED;
    }

    public static State forName(String stateName) {
      return forName(State.class, stateName);
    }

    public static boolean isWorking(State state) {
      return state == SUSPENDED || state == WORKING;
    }
  }

  public static enum SpecificItemActivity {
    UPLOAD,
    DOWNLOAD,
    OTHER,
  }
}
