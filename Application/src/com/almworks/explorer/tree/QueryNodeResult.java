package com.almworks.explorer.tree;

import com.almworks.api.application.*;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.application.tree.QueryUtil;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.application.util.sources.CompositeItemSource;
import com.almworks.api.application.util.sources.ItemViewAdapter;
import com.almworks.api.application.util.sources.LoadingUserQueryItemSource;
import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.QueryUrlInfo;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.explorer.DelegatingItemSource;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.concurrent.SynchronizedBoolean;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @author dyoma
 */
class QueryNodeResult extends SimpleModifiable implements QueryResult, ChangeListener {
  private static final Procedure2<ExplorerComponent, GenericNode> RUN_LOCALLY = new MyRunLocally();
  private static final Procedure2<ExplorerComponent, GenericNode> RUN_RELOAD = new MyRunReload();

  private final AbstractQueryNode myNode;
  private WeakReference<DBFilter> myViewCache = null;
  private WeakReference<Constraint> myCachedConstraint = null;

  private volatile BoolExpr<DP> myFilter = null;
  private FilterNode myFilterTree = null;
  private int myVersion = 0;

  private WeakReference<ItemHypercube> myCachedEncompassingHypercube = null;
  private WeakReference<ItemHypercube> myCachedExactHypercube = null;

  private Detach myParentDetach = Detach.NOTHING;


  public QueryNodeResult(@NotNull AbstractQueryNode node) {
    myNode = node;
  }

  @Override
  @Nullable
  public DBFilter getDbFilter() {
    if (myFilter == null)
      return null;
    synchronized (this) {
      WeakReference<DBFilter> cache = myViewCache;
      if (cache != null) {
        DBFilter cached = cache.get();
        if (cached != null) {
          //      assert getParentResult().getDbFilter() != null : myNode.getName();
          return cached;
        }
      }
    }
    QueryResult parentResult = getParentResult();
    if (parentResult == null)
      return null;
    DBFilter parentView = parentResult.getDbFilter();
    if (parentView == null)
      return null;
    DBFilter view = parentView.filter(myFilter);
    synchronized (this) {
      myViewCache = new WeakReference<DBFilter>(view);
    }
    return view;
  }

  @Nullable
  @ThreadAWT
  public Procedure2<ExplorerComponent, GenericNode> getRunLocallyProcedure() {
    return RUN_LOCALLY;
  }

  @Nullable
  @ThreadAWT
  public Procedure2<ExplorerComponent, GenericNode> getRunWithReloadProcedure() {
    return RUN_RELOAD;
  }

  @ThreadAWT
  @Nullable
  public Constraint getValidConstraint() {
    Threads.assertAWTThread();
    WeakReference<Constraint> cached = myCachedConstraint;
    if (cached != null) {
      Constraint c = cached.get();
      if (c != null)
        return c;
    }
    FilterNode filterStructure = myFilterTree;
    QueryResult parentResult = getParentResult();
    if (filterStructure != null && parentResult != null) {
      Constraint parentConstraint = parentResult.getValidConstraint();
      if (parentConstraint != null) {
        ItemHypercube hypercube = parentResult.getHypercube(false);
        NameResolver resolver = getResolver();
        if (resolver != null) {
          filterStructure.normalizeNames(resolver, hypercube);
        }
        Constraint constraint = CompositeConstraint.Simple.and(parentConstraint, filterStructure.createConstraint(hypercube));
        Constraint c = KnownConstraints.isValid(constraint) ? constraint : null;
        myCachedConstraint = new WeakReference<Constraint>(c);
        return c;
      }
    }
    myCachedConstraint = null;
    return null;
  }

  @Nullable
  public ItemHypercube getHypercube(boolean precise) {
    Constraint constraint = getValidConstraint();
    if (constraint == null) {
      if (precise)
        return null;
      GenericNode parent = myNode.getParent();
      return parent == null ? null : parent.getHypercube(precise);
    }
    synchronized (this) {
      WeakReference<ItemHypercube> ref = precise ? myCachedExactHypercube : myCachedEncompassingHypercube;
      ItemHypercube cached = ref == null ? null : ref.get();
      if (cached != null)
        return cached;

      ItemHypercubeImpl hypercube = calculateHypercube(precise, constraint);

      ref = new WeakReference<ItemHypercube>(hypercube);
      if (precise)
        myCachedExactHypercube = ref;
      else
        myCachedEncompassingHypercube = ref;
      return hypercube;
    }
  }

  private ItemHypercubeImpl calculateHypercube(boolean precise, Constraint constraint) {
    ItemHypercubeImpl hypercube = ItemHypercubeUtils.getHypercube(constraint, precise);
    if (hypercube != null) {
      ItemHypercubeUtils.adjustForConnection(hypercube, myNode.getConnection());
    }
    return hypercube;
  }

  @NotNull
  public ItemHypercube getEncompassingHypercube() {
    return AlwaysReady.getEncompassingHypercube(this);
  }

  public boolean isRunnable() {
    return myNode.getConnection() != null || myNode.isSynchronized();
  }

  public boolean canRunNow() {
    QueryResult parentResult = getParentResult();
    return myFilter != null && parentResult != null && parentResult.canRunNow() && getValidConstraint() != null;
  }

  public FilterNode getFilterTree() {
    return myFilterTree;
  }

  @Nullable
  private NameResolver getResolver() {
    return myNode.getResolver();
  }

  public void setFilterTree(FilterNode filterTree) {
    Threads.assertAWTThread();
    myVersion++;
    myFilterTree = filterTree;
    updateFilter();
  }

  @ThreadAWT
  public void attach() {
    Threads.assertAWTThread();
    GenericNode parent = myNode.getParent();
    assert parent != null : myNode;
    QueryResult parentResult = parent.getQueryResult();
    DetachComposite life = new DetachComposite(true);
    parentResult.addAWTChangeListener(life, new ChangeListener() {
      public void onChange() {
        boolean updated = updateFilter();
        if (!updated) {
          // fire anyway, 'cause parent has changed
          fireChanged();
        }
      }
    });
    myParentDetach = life;
    updateFilter();
  }

  @ThreadAWT
  public void detach() {
    myParentDetach.detach();
    myParentDetach = Detach.NOTHING;
  }

  boolean updateFilter() {
    return updateFilter(false);
  }

  boolean updateFilter(boolean inhibitFire) {
    if (!myNode.isNode())
      return false;
    myCachedConstraint = null;
    synchronized (this) {
      if (myCachedEncompassingHypercube != null) {
        myCachedEncompassingHypercube.clear();
        myCachedEncompassingHypercube = null;
      }
      if (myCachedExactHypercube != null) {
        myCachedExactHypercube.clear();
        myCachedExactHypercube = null;
      }
      if (myViewCache != null) {
        myViewCache.clear();
        myViewCache = null;
      }
    }
    BoolExpr<DP> newFilter = null;
    NameResolver resolver = getResolver();
    if (resolver != null) {
      QueryResult parent = getParentResult();
      ItemHypercube hypercube = parent == null ? new ItemHypercubeImpl() : parent.getHypercube(false);
      myFilterTree.normalizeNames(resolver, hypercube);
      try {
        newFilter = myFilterTree.createFilter(hypercube);
      } catch (UnresolvedNameException unresolvedName) {
        newFilter = null;
      }
      getValidConstraint();
    }
    boolean updated = setFilter(newFilter);
    if (updated && !inhibitFire)
      fireChanged();
    myNode.onQueryNodeResultStirred(updated);
    return updated;
  }

  /**
   * @param source - null means default (remote if not synchonized source)
   */
  private void showQueryResult(ExplorerComponent explorer, boolean rerun, boolean local,
    @Nullable ItemSource source)
  {
    if (source == null)
      source = getItemSource();
    explorer.showItemsInTab(source, createCollectionContext(local, rerun), false);
  }

  @NotNull
  public ItemSource getItemSource() {
    return new DelegatingItemSource() {
      protected ItemSource createDelegate() {
        Connection connection = myNode.getConnection();
        if (myNode.isSynchronized() || connection == null) {
          ItemViewAdapter localSource = createLocalSource();
          if (localSource == null)
            return EMPTY;
          else
            return localSource;
        } else {
          return new RemoteItemSource();
        }
      }
    };
  }

  public ItemCollectionContext getCollectionContext() {
    return createCollectionContext(false, false);
  }

  private ItemCollectionContext createCollectionContext(boolean local, boolean rerun) {
    NodeTabKey queryKey = new UserQueryKey(myNode, getVersion(), local);
    ItemCollectionContext context = ItemCollectionContext.createQueryNode(myNode, myNode.getName(), queryKey);
    if (rerun)
      context.forceRerun();
    return context;
  }

  @Nullable
  private ItemViewAdapter createLocalSource() {
    DBFilter view = QueryUtil.maybeGetHintedView(myNode, this);
    if (view == null)
      return null;
    return ItemViewAdapter.create(view, myNode);
  }

  private boolean setFilter(BoolExpr<DP> filter) {
    if (!Util.equals(filter, myFilter)) {
      myFilter = filter;
      return true;
    } else {
      return false;
    }
  }

  public long getVersion() {
    Threads.assertAWTThread();
    QueryResult result = getParentResult();
    return (result != null ? result.getVersion() : 0) + myVersion;
  }

  @ThreadAWT
  void forceVersionIncrease() {
    Threads.assertAWTThread();
    myVersion++;
  }

  @ThreadAWT
  public void getQueryURL(@NotNull @ThreadAWT final Procedure<QueryUrlInfo> urlConsumer) {
    final Connection connection = myNode.getConnection();
    final Constraint constraint = getValidConstraint();
    RootNode root = myNode.getRoot();
    if (connection == null || constraint == null || root == null) {
      urlConsumer.invoke(null);
    } else {
      root.getEngine().getDatabase().readBackground(new ReadTransaction<QueryUrlInfo>() {
        @Override
        public QueryUrlInfo transaction(DBReader reader) throws DBOperationCancelledException {
          try {
            return connection.getQueryURL(constraint, reader);
          } catch (InterruptedException e) {
            throw new DBOperationCancelledException();
          }
        }
      }).onSuccess(ThreadGate.AWT, urlConsumer);
    }
  }

  @ThreadAWT
  public boolean hasQueryUrl() {
    final Connection connection = myNode.getConnection();
    final Constraint constraint = getValidConstraint();
    return !(constraint == null || connection == null);
  }

  @ThreadAWT
  public void fireChanged() {
    super.fireChanged();
  }

  @Nullable
  private QueryResult getParentResult() {
    GenericNode parent = myNode.getParent();
    return parent == null ? null : parent.getQueryResult();
  }


  private class RemoteItemSource extends CompositeItemSource {
    public RemoteItemSource() {
      super(RemoteItemSource.class.getName(), "Executing query");
    }

    public void stop(@NotNull ItemsCollector collector) {
      Detach detach = collector.getValue(DETACH);
      if (detach != null) {
        detach.detach();
        collector.putValue(DETACH, null);
      }
      super.stop(collector);
    }

    public void reloadingPrepare(final ItemsCollector collector) {
      stop(collector);
      clear(collector);

      final BasicScalarModel<Long> requiredWCNModel = BasicScalarModel.create();
      final SynchronizedBoolean stopped = new SynchronizedBoolean(false);

      DetachComposite once = new DetachComposite(true);
      once.add(new Detach() {
        protected void doDetach() {
          stopped.set(true);
        }
      });
      Detach old = collector.putValue(DETACH, once);
      if (old != null)
        old.detach();

      Constraint constraint = getValidConstraint();
      DBFilter artifactView = QueryUtil.maybeGetHintedView(myNode, QueryNodeResult.this);
      if (artifactView == null || constraint == null) {
        assert false : artifactView + " " + constraint;
        return;
      }

      BasicScalarModel<LongList> localResultModel = BasicScalarModel.create(true);
      ItemViewAdapter db = ItemViewAdapter.create(artifactView, myNode, localResultModel);
      db.setRequiredCNModel(collector, requiredWCNModel);
      add(collector, db, 1000);

      Connection connection = myNode.getConnection();
      if (connection != null) {
        Procedure<SyncTask> runFinally = new Procedure<SyncTask>() {
          public void invoke(SyncTask syncTask) {
            requiredWCNModel.setValue(syncTask.getLastCommittedCN());
            boolean success = (syncTask.getState().getValue() == SyncTask.State.DONE) && !stopped.get();
            if (success) {
              // additional check for errors
              List<String> errors = syncTask.getProgressSource().getErrors(null);
              if (errors != null && errors.size() > 0) {
                success = false;
              }
            }

            if (success) {
              myNode.setSyncFlag(success, false);
            }
          }
        };
        LoadingUserQueryItemSource remote = new LoadingUserQueryItemSource(connection, myNode.getName(),
          constraint, artifactView, localResultModel, runFinally);
        add(collector, remote, 10000);
      } else {
        assert false : myNode;
        requiredWCNModel.setValue(-1l);
      }
    }
  }


  private static class UserQueryKey extends NodeTabKey {
    private final long myVersion;
    private final boolean myLocal;

    public UserQueryKey(AbstractQueryNode node, long version, boolean local) {
      super(node);
      myVersion = version;
      myLocal = local;
    }

    protected boolean isReplaceNodeTab(NodeTabKey nodeKey) {
      if (!(nodeKey instanceof UserQueryKey))
        return false;
      UserQueryKey other = (UserQueryKey) nodeKey;
      assert other.getNode() == getNode();
      return myVersion != other.myVersion;
    }

    public boolean equals(Object obj) {
      return super.equals(obj) && ((UserQueryKey) obj).myLocal == myLocal;
    }
  }


  private static class MyRunLocally implements Procedure2<ExplorerComponent, GenericNode> {
    public void invoke(ExplorerComponent explorer, GenericNode node) {
      QueryResult qr = node.getQueryResult();
      if (qr instanceof QueryNodeResult) {
        QueryNodeResult result = (QueryNodeResult) qr;
        ItemViewAdapter localSource = result.createLocalSource();
        if (localSource != null)
          result.showQueryResult(explorer, false, true, localSource);
      }
    }
  }


  private static class MyRunReload implements Procedure2<ExplorerComponent, GenericNode> {
    public void invoke(ExplorerComponent explorer, GenericNode node) {
      QueryResult qr = node.getQueryResult();
      if (node instanceof AbstractQueryNode) {
        ((AbstractQueryNode) node).setSyncFlag(false, true);
      }
      if (qr instanceof QueryNodeResult) {
        QueryNodeResult result = (QueryNodeResult) qr;
        result.forceVersionIncrease();
        result.showQueryResult(explorer, false, false, result.new RemoteItemSource());
      }
    }
  }
}
