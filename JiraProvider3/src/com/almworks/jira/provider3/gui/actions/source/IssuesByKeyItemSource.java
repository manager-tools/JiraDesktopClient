package com.almworks.jira.provider3.gui.actions.source;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.api.application.util.sources.AbstractItemSource;
import com.almworks.api.application.util.sources.CompositeItemSource;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.explorer.SimpleCollection;
import com.almworks.integers.IntList;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.app.sync.QueryIssuesUtil;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.L;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.progress.ProgressUtil;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

public class IssuesByKeyItemSource extends AbstractItemSource {
  private static final TypedKey<Loader> LOADER = TypedKey.create("loader");
  private final String[] myKeys;
  private final JiraConnection3 myConnection;

  public IssuesByKeyItemSource(String[] keys, JiraConnection3 connection) {
    super("getIssuesByKey");
    myKeys = Collections15.arrayCopy(keys);
    myConnection = connection;
  }

  public static ItemSource create(JiraConnection3 connection, Collection<String> keys) {
    return new IssuesByKeyItemSource(keys.toArray(new String[keys.size()]), connection);
  }

  public static ItemSource create(JiraConnection3 connection, String key) {
    return new IssuesByKeyItemSource(new String[]{key}, connection);
  }

  public static ItemSource create(String name, final JiraConnection3 connection, @Nullable LongList issues,
    @Nullable Collection<String> byKey)
  {
    if (issues == null) issues = LongList.EMPTY;
    if (byKey == null) byKey = Collections15.emptySet();
    final SimpleCollection loadedSource = SimpleCollection.create(connection.getDatabase(), issues);
    if (byKey.isEmpty()) return loadedSource;
    @SuppressWarnings("ConstantConditions")
    final int loadedCount = issues.size();
    final String[] missingKeys = byKey.toArray(new String[byKey.size()]);
    return new CompositeItemSource(name, "") {
      @Override
      protected void reloadingPrepare(ItemsCollector collector) {
        clear(collector);
        add(collector, loadedSource, loadedCount);
        add(collector, new IssuesByKeyItemSource(missingKeys, connection), missingKeys.length * 100);
        super.reloadingPrepare(collector);
      }
    };
  }

  @ThreadAWT
  public void stop(@NotNull ItemsCollector collector) {
    IssuesByKeyItemSource.Loader loader = collector.getValue(LOADER);
    if (loader != null) {
      loader.stop();
      collector.putValue(LOADER, null);
    }
  }

  @ThreadAWT
  public void reload(@NotNull ItemsCollector collector) {
    Loader loader = new Loader(collector);
    collector.putValue(LOADER, loader);
    setProgress(collector, loader.getProgress());
    loader.start();
  }

  private class Loader implements Procedure<Void>, Runnable {
    private final ItemsCollector myCollector;
    private final Set<String> myKeysLeft;
    private final Progress myWholeProgress = new Progress("getIssueByKey");
    private SyncTask myDownloadTask = null;
    private boolean myCancelled = false;

    private final BoolExpr<DP> myFilter;
    private final Progress myLocalProgress;
    private final Progress myRemoteProgress;

    public Loader(ItemsCollector collector) {
      myCollector = collector;
      myKeysLeft = Collections15.hashSet(myKeys);
      myFilter = DPEquals.equalOneOf(Issue.KEY, Collections15.hashSet(myKeys))
        .and(DPEquals.create(SyncAttributes.CONNECTION, myConnection.getConnectionItem()));
      Pair<Progress,Progress> pair = ProgressUtil.splitProgress(myWholeProgress, "Search DB", 0.1, "Download", 0.9);
      myLocalProgress = pair.getFirst();
      myRemoteProgress = pair.getSecond();
    }

    public void start() {
      myLocalProgress.setProgress(0.5F, L.content("Searching local DB"));
      myConnection.getSyncManager().enquireRead(DBPriority.FOREGROUND, readDB(false, null)).finallyDo(ThreadGate.LONG(IssuesByKeyItemSource.class), this);
    }

    private void loadNewIssuesFromDB(IntList issueIds) {
      myConnection.getSyncManager().enquireRead(DBPriority.FOREGROUND, readDB(true, issueIds)).finallyDoWithResult(ThreadGate.STRAIGHT, new Procedure<DBResult<Void>>() {
        @Override
        public void invoke(DBResult<Void> result) {
          setDone();
        }
      });
    }

    private ReadTransaction<Void> readDB(final boolean addDummies, @Nullable final IntList issueIds) {
      return new ReadTransaction<Void>() {
        @Override
        public Void transaction(DBReader reader) throws DBOperationCancelledException {
          BoolExpr<DP> query = myFilter;
          loadAll(reader, query, addDummies);
          if (issueIds != null) {
            BoolExpr<DP> queryIds = DPEquals.equalOneOf(Issue.ID, issueIds).and(DPEquals.create(SyncAttributes.CONNECTION, myConnection.getConnectionItem()));
            loadAll(reader, queryIds, addDummies);
          }
          return null;
        }

        private void loadAll(DBReader reader, BoolExpr<DP> query, boolean addDummies) {
          LongArray aa = reader.query(query).copyItemsSorted();
          for (int i = 0; i < aa.size(); i++) {
            long a = aa.get(i);
            boolean isNotDummy = ItemDownloadStage.getValue(SyncUtils.readTrunk(reader, a)) != ItemDownloadStage.DUMMY;
            if (isNotDummy) myKeysLeft.remove(Issue.KEY.getValue(a, reader));
            if (isNotDummy || addDummies) myCollector.addItem(a, reader);
          }
        }
      };
    }


    @Override
    public void invoke(Void arg) {
      if (myKeysLeft.isEmpty()) {
        setDone();
        return;
      }
      SyncTask task = QueryIssuesUtil.synchronizeByKey(myConnection, myKeysLeft,
        Local.parse("Loading " + Terms.ref_artifacts + " from server"), new Procedure<IntList>() {
        @Override
        public void invoke(IntList issueIds) {
          loadNewIssuesFromDB(issueIds);
        }
      });
      boolean cancelled;
      synchronized (this) {
        myDownloadTask = task;
        cancelled = myCancelled;
      }
      if (cancelled) task.cancel();
      myRemoteProgress.delegate(task.getProgressSource());
    }

    public void stop() {
      SyncTask task;
      synchronized (this) {
        myCancelled = true;
        task = myDownloadTask;
      }
      if (task != null) task.cancel();
    }

    public ProgressSource getProgress() {
      return myWholeProgress;
    }

    private void setDone() {
      ThreadGate.AWT.execute(this);
    }

    @Override
    public void run() {
      myWholeProgress.setDone();
    }
  }
}
