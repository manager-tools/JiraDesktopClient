package com.almworks.api.engine.util;

import com.almworks.api.engine.SyncProblem;
import com.almworks.api.engine.SyncTask;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.model.SetHolder;
import com.almworks.util.model.SetHolderModel;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.almworks.util.model.SetHolderUtils.actualizingSequentialListener;
import static org.almworks.util.Collections15.arrayList;
import static org.almworks.util.Collections15.hashMap;

/**
 * Represenents a union of sync task sets. Maintains image of the union and union of problems of all tasks in all sets.<br><br>
 * Allows to listen to changes of state of any task in the set union. Listener receives a consistent snapshot of the sync tasks set union. Consistent means that some time ago there was a point at which the union of tasks sets contained these and only these tasks.<br>
 * Listener is called in a sequential manner (non-concurrently) using the same gate that this class uses to listen to task sets.<br>
 * <em>Note:</em> Don't use this listener if you are interested not only in the recent snapshot of the union, but in <em>every</em> state in the past also: some of the past snapshots may be never delivered to the listener (if this class received notification about more recent state before the notification about the past state).<br>
 */
public class SyncTasksSetUnion {
  private final ThreadGate myGate;
  
  private final SetHolderModel<SyncTask> myTasksUnion = new SetHolderModel<SyncTask>();
  private final SetHolderModel<SyncProblem> myProblemsUnion = new SetHolderModel<SyncProblem>();

  private final Procedure<Collection<SyncTask>> myOnStateChanged;
  private final ScalarModel.Consumer<SyncTask.State> myTasksStateListener;
  private boolean myInhibitStateChangeNotification;

  public static SyncTasksSetUnion createAwt(@Nullable Procedure<Collection<SyncTask>> onStateChanged) {
    return new SyncTasksSetUnion(ThreadGate.AWT, onStateChanged);
  }

  /**
   * @param key long gate key; if null, new object is created for that purpose
   * */
  public static SyncTasksSetUnion createLong(@Nullable Object key, @Nullable Procedure<Collection<SyncTask>> onStateChanged) {
    if (key == null) key = new Object();
    return new SyncTasksSetUnion(ThreadGate.LONG_QUEUED(key), onStateChanged);
  }

  private SyncTasksSetUnion(ThreadGate gate, @Nullable final Procedure<Collection<SyncTask>> onStateChanged) {
    myGate = gate;
    myOnStateChanged = Util.NN(onStateChanged, Procedure.Stub.<Collection<SyncTask>>instance());
    myTasksStateListener = new ScalarModel.Adapter() {
      @Override
      public void onScalarChanged(ScalarModelEvent unused) {
        if (!myInhibitStateChangeNotification) notifyStateChanged();
      }
    };
  }

  private void notifyStateChanged() {
    myOnStateChanged.invoke(myTasksUnion.copyCurrent());
  }

  public SetHolder<SyncTask> getTasksUnion() {
    return myTasksUnion;
  }

  public SetHolderModel<SyncProblem> getProblemsUnion() {
    return myProblemsUnion;
  }

  public SyncTasksSetUnion subscribe(Lifespan life, SetHolder<SyncTask> set) {
    set.addInitListener(life, myGate, actualizingSequentialListener(new SyncTaskSetListener(life)));
    return this;
  }

  private class SyncTaskSetListener implements SetHolder.Listener<SyncTask> {
    private final Map<SyncTask, DetachComposite> mySubscriptions = hashMap();
    private final Lifespan myLife;

    public SyncTaskSetListener(Lifespan life) {
      myLife = life;
      myLife.add(new Detach() { @Override protected void doDetach() throws Exception {
        dispose();
      }});
    }

    @Override
    public void onSetChanged(@NotNull SetHolder.Event<SyncTask> evt) {
      if (myLife.isEnded()) return;
      try {
        myInhibitStateChangeNotification = true;
        for (SyncTask task : evt.getAdded()) {
          subscribe(task);
        }
        for (SyncTask task : evt.getRemoved()) {
          unsubscribe(task);
        }
      } finally {
        myInhibitStateChangeNotification = false;
      }
      myTasksUnion.changeSet(evt.getAdded(), evt.getRemoved());
      notifyStateChanged();
    }

    private void subscribe(SyncTask task) {
      DetachComposite subscriptionLife = new DetachComposite();
      mySubscriptions.put(task, subscriptionLife);
      task.getState().getEventSource().addListener(subscriptionLife, myGate, myTasksStateListener);
      task.getProblems().addInitListener(subscriptionLife, ThreadGate.STRAIGHT, new TaskProblemsListener(subscriptionLife));
    }

    private void unsubscribe(SyncTask task) {
      DetachComposite life = mySubscriptions.remove(task);
      if (life != null) {
        life.detach();
      } else {
        assert false : task;
      }
    }

    private void dispose() {
      for (DetachComposite life : mySubscriptions.values()) {
        life.detach();
      }
      Set<SyncTask> removedTasks = mySubscriptions.keySet();
      myTasksUnion.remove(removedTasks);
      mySubscriptions.clear();
    }

    /**
     * This class is needed to maintain actual state of tasks' problems per task. Otherwise, when we unsubscribe from task, we have to use its latest state of problems collection.<br>
     * The latter alternative fails for the following sequence of events.
     * <ol>
     * <li>Subscription lifespan on a task is over.</li>
     * <li>Problems listener is unsubscribed from the task.</li>
     * <li>A problem, P, is removed from the task.</li>
     * <li>Detach is called that removes task problems from the problems union.</li>
     * </ol>
     * If detach called on the last step calls {@link SetHolder#copyCurrent copyCurrent} on the task, it does not receive P hence does not remove it from the union and P stays there forever.
     * */
    private class TaskProblemsListener implements SetHolder.Listener<SyncProblem> {
      /** guarded by {@link #myLock} */
      private List<SyncProblem> myTaskProblems = null;
      /** guarded by {@link #myLock} */
      private long myVersion = 0L;
      private final Object myLock = new Object();
      private final Lifespan myLife;

      public TaskProblemsListener(Lifespan life) {
        myLife = life;
        myLife.add(new Detach() { @Override protected void doDetach() throws Exception {
          dispose();
        }});
      }

      private void dispose() {
        synchronized (myLock) {
          myProblemsUnion.changeSet(null, myTaskProblems);
        }
      }

      @Override
      public void onSetChanged(@NotNull SetHolder.Event<SyncProblem> event) {
        synchronized (myLock) {
          if (myLife.isEnded()) return;
          myVersion = event.actualize(myVersion);
          if (event.isEmpty()) return;
          if (myTaskProblems == null) myTaskProblems = arrayList();
          myTaskProblems.addAll(event.getAdded());
          myTaskProblems.removeAll(event.getRemoved());
          myProblemsUnion.changeSet(event.getAdded(), event.getRemoved());
        }
      }
    }
  }
}