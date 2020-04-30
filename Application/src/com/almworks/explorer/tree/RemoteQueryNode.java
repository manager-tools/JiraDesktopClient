package com.almworks.explorer.tree;

import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.api.application.NodeTabKey;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.application.util.sources.AbstractItemSource;
import com.almworks.api.application.util.sources.ItemViewAdapter;
import com.almworks.api.engine.QueryUrlInfo;
import com.almworks.api.engine.RemoteQuery;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DP;
import com.almworks.items.api.Database;
import com.almworks.util.Constant;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
class RemoteQueryNode extends GenericNodeImpl {
  private final String myConnectionNodeId;
  private final RemoteQuery myRemoteQuery;
  private final QueryResult myResult;

  public RemoteQueryNode(Database db, String connectionNodeId, RemoteQuery remoteQuery) {
    super(db, new FixedText(remoteQuery.getQueryName(), Icons.NODE_REMOTE_QUERY), Configuration.EMPTY_CONFIGURATION);
    myConnectionNodeId = connectionNodeId;
    myRemoteQuery = remoteQuery;
    myResult = new QueryResult.AlwaysReady() {
      @Override
      public DBFilter getDbFilter() {
        GenericNode parent = getParent();
        return parent != null ? parent.getQueryResult().getDbFilter() : null;
      }

      @Nullable
      public ItemSource getItemSource() {
        DBFilter userView = getDbFilter();
        if (userView == null) {
          assert false;
          return null;
        }
        ThreadGate gate = ThreadGate.LONG(RemoteQueryNode.class);
        return new RemoteQueryItemSource(myRemoteQuery, userView, gate);
      }

      public ItemCollectionContext getCollectionContext() {
        return ItemCollectionContext.createRemoteQuery(RemoteQueryNode.this, getName(),
          new NodeTabKey(RemoteQueryNode.this));
      }

      public long getVersion() {
        return 0;
      }

      @ThreadAWT
      public void getQueryURL(@NotNull @ThreadAWT final Procedure<QueryUrlInfo> urlConsumer) {
        myRemoteQuery.getQueryUrl(new Procedure<String>() {
          public void invoke(String arg) {
            urlConsumer.invoke(new QueryUrlInfo(arg));
          }
        }, ThreadGate.AWT);
      }

      @ThreadAWT
      public boolean hasQueryUrl() {
        return true;
      }

      @Nullable
      public ItemHypercube getHypercube(boolean precise) {
        return RemoteQueryNode.this.getHypercube(precise);
      }
    };
  }

  public String getName() {
    return myRemoteQuery.getQueryName();
  }

  public RemoteQuery getRemoteQuery() {
    return myRemoteQuery;
  }

  public boolean isCopiable() {
    return false;
  }

  public String getNodeId() {
    return "Remote." + getName() + "@" + myConnectionNodeId;
  }

  @NotNull
  public QueryResult getQueryResult() {
    return myResult;
  }

  private static class RemoteQueryItemSource extends AbstractItemSource {
    private final RemoteQuery myQuery;
    private final ThreadGate myGate;
    private final DBFilter myWholeView;

    public RemoteQueryItemSource(RemoteQuery query, DBFilter wholeView, ThreadGate gate) {
      super(RemoteQueryItemSource.class.getName());
      myQuery = query;
      myWholeView = wholeView;
      myGate = gate;
    }

    public void stop(ItemsCollector collector) {
      DetachComposite detach = collector.getValue(DETACH);
      if (detach == null)
        return;
      detach.detach();
      collector.putValue(DETACH, null);
    }

    public void reload(final ItemsCollector collector) {
      collector.putValue(DETACH, new DetachComposite(true));

      final Progress delegate = getProgressDelegate(collector);
      final Progress progress = new Progress("RQLA");
      delegate.delegate(progress);

      myGate.execute(new Runnable() {
        public void run() {
          startQueryLoading(collector, progress);
        }
      });
    }

    private void startQueryLoading(final ItemsCollector collector, Progress progress) {
      final DetachComposite detach = collector.getValue(DETACH);
      if (detach == null)
        return;

      final Progress syncProgress = progress.createDelegate(0.83, "RQLA-L");
      final Progress viewProgress = progress.createDelegate(0.17, "RQLA-Q");

      Procedure<SyncTask> runFinally = new Procedure<SyncTask>() {
        public void invoke(SyncTask syncTask) {
          syncProgress.setDone();
          startViewLoading(collector, viewProgress, syncTask, detach);
        }
      };
      final SyncTask syncTask = myQuery.reload(runFinally);

      final DetachComposite syncDetach = new DetachComposite(true);
      detach.add(syncDetach);
      detach.add(new Detach() {
        protected void doDetach() {
          syncTask.cancel();
        }
      });
      syncProgress.delegate(syncTask.getProgressSource());
    }

    private void startViewLoading(final ItemsCollector collector, Progress viewProgress, SyncTask syncTask,
      DetachComposite detach) {

      BoolExpr<DP> filter = myQuery.getFilter();
      int count = myQuery.getCount();
      if (filter == null || count <= 0) {
        viewProgress.setDone();
        detach.detach();
        return;
      }

      Constant<Integer> totalCount = new Constant<Integer>(count);
      final ItemViewAdapter source = ItemViewAdapter.create(myWholeView, filter, totalCount);
      source.setRequiredCNModel(collector, BasicScalarModel.createConstant(syncTask.getLastCommittedCN()));
      source.reload(collector);

      ProgressSource progress = source.getProgress(collector);
      viewProgress.delegate(progress);
      detach.add(new Detach() {
        protected void doDetach() {
          source.stop(collector);
        }
      });
    }
  }

/*
  public String toString() {
    return getName();
  }
*/
}
