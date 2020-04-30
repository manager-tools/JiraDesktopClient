package com.almworks.api.application.util.sources;

import com.almworks.api.application.ItemsCollector;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.SyncTask;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBFilter;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.progress.Progress;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class LoadingUserQueryItemSource extends AbstractItemSource {
  private final Constraint myConstraint;
  private final Procedure<SyncTask> myOnFinish;
  private final Connection myConnection;
  private final DBFilter myItemView;
  private final String myQueryName;
  private ScalarModel<LongList> myLocalResultModel;

  public LoadingUserQueryItemSource(Connection connection, String queryName, @NotNull Constraint constraint,
    DBFilter itemView, ScalarModel<LongList> localResultModel, Procedure<SyncTask> onFinish)
  {
    super(LoadingUserQueryItemSource.class.getName());

    //noinspection ConstantConditions
    assert constraint != null;
    assert queryName != null;
    assert connection != null;
    assert itemView != null;

    myQueryName = queryName;
    myOnFinish = onFinish;
    myConstraint = constraint;
    myConnection = connection;
    myItemView = itemView;
    myLocalResultModel = localResultModel;
  }

  public void reload(final ItemsCollector collector) {
    stop(collector);

    final DetachComposite detach = new DetachComposite(true);
    final Procedure<SyncTask> runFinally = new Procedure<SyncTask>() {
      public void invoke(final SyncTask syncTask) {
        ThreadGate.AWT.execute(new Runnable() {
          public void run() {
            if (myOnFinish != null)
              myOnFinish.invoke(syncTask);
            getProgressDelegate(collector).setDone();
          }
        });
      }
    };

    ScalarModel<LongList> localResultModel = myLocalResultModel;
    if (localResultModel != null) {
      // wait for local result to appear and then reload
      final Progress p = new Progress();
      p.setProgress(0.01F);
      getProgressDelegate(collector).delegate(p);
      final DetachComposite localResultWaiting = new DetachComposite();
      detach.add(localResultWaiting);
      localResultModel.getEventSource()
        .addListener(localResultWaiting, ThreadGate.LONG, new ScalarModel.Adapter<LongList>() {
          private final AtomicBoolean myProcessed = new AtomicBoolean(false);
          public void onScalarChanged(ScalarModelEvent<LongList> event) {
            LongList localResult = event.getNewValue();
            if (localResult == null) return;
            if (!myProcessed.compareAndSet(false, true)) return;
            myLocalResultModel = null;
            reloadSyncTask(detach, runFinally, localResult, p.createDelegate());
            localResultWaiting.detach();
          }
        });
    } else {
      // reload now - no local result
      reloadSyncTask(detach, runFinally, null, getProgressDelegate(collector));
    }
    collector.putValue(DETACH, detach);
  }

  private void reloadSyncTask(DetachComposite detach, Procedure<SyncTask> runFinally, LongList localResult, Progress progress) {
    final SyncTask syncTask = myConnection.synchronizeItemView(myConstraint, myItemView, localResult, myQueryName, runFinally);
    detach.add(new Detach() {
      protected void doDetach() {
        syncTask.cancel();
      }
    });
    progress.delegate(syncTask.getProgressSource());
  }

  public void stop(ItemsCollector collector) {
    Detach detach = collector.getValue(DETACH);
    if (detach != null) {
      detach.detach();
      collector.putValue(DETACH, null);
      getProgressDelegate(collector).setDone();
    }
  }
}
