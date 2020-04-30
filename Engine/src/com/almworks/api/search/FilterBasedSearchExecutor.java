package com.almworks.api.search;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
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
import com.almworks.api.engine.ConnectionState;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.SyncCubeRegistry;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DP;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.TreeUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.concurrent.SynchronizedInt;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class FilterBasedSearchExecutor implements TextSearchExecutor {
  private final Engine myEngine;
  private final SyncCubeRegistry myCubeRegistry;

  protected FilterBasedSearchExecutor(Engine engine, SyncCubeRegistry cubeRegistry) {
    myCubeRegistry = cubeRegistry;
    myEngine = engine;
  }

  @Nullable
  protected abstract BoolExpr<DP> getFilter(final Connection connection);

  @Nullable
  protected abstract Constraint getConstraint(Connection connection);

  public ItemSource executeSearch(Collection<? extends GenericNode> scope) {
    Threads.assertAWTThread();
    scope = expandUnsearchableNodes(scope);
    Set<GenericNode> disjunctiveNodes = TreeUtil.excludeDescendants(scope, GenericNode.GET_PARENT_NODE);
    final List<NodeInfo> nodeInfos = Collections15.arrayList();
    Collection<Connection> connections = myEngine.getConnectionManager().getReadyConnectionsModel().copyCurrent();
    for (GenericNode node : disjunctiveNodes) {
      Connection connection = node.getConnection();
      if (connection != null) {
        nodeInfos.add(new NodeInfo(connection, node));
      } else {
        for (Connection c : connections) {
          nodeInfos.add(new NodeInfo(c, node));
        }
      }
    }
    return new FilterBasedItemSource(nodeInfos);
  }

  private Collection<? extends GenericNode> expandUnsearchableNodes(Collection<? extends GenericNode> scope) {
    for (GenericNode tnode : scope) {
      if (isUnsearchable(tnode)) {
        List<GenericNode> result = Collections15.arrayList();
        for (GenericNode node : scope) {
          if (isUnsearchable(node)) {
            result.addAll(node.getChildren());
          } else {
            result.add(node);
          }
        }
        // process children now
        return expandUnsearchableNodes(result);
      }
    }
    return scope;
  }

  private boolean isUnsearchable(GenericNode node) {
    if (node instanceof RootNode) {
      // root node returns trash
      return true;
    }
    if (node.getParent() instanceof RootNode && node.getConnection() == null) {
      // collections folder node
      return true;
    }
    return false;
  }

  protected static class NodeInfo {
    private final GenericNode myNode;
    private final Connection myConnection;
    private final DBFilter myView;
    private final Constraint myConstraint;
    private final boolean mySynchronized;

    @ThreadAWT
    public NodeInfo(Connection connection, GenericNode node) {
      Threads.assertAWTThread();
      myConnection = connection;
      myNode = node;
      QueryResult result = node.getQueryResult();
      myView = result.getDbFilter();
      myConstraint = result.getValidConstraint();
      mySynchronized = node.isSynchronized();
    }

    public Connection getConnection() {
      return myConnection;
    }

    public DBFilter getView() {
      return myView;
    }

    public boolean isSynchronized() {
      return mySynchronized;
    }

    public Constraint getConstraint() {
      return myConstraint;
    }

    public GenericNode getNode() {
      return myNode;
    }
  }


  protected class FilterBasedItemSource extends CompositeItemSource {
    private final List<NodeInfo> myScope;

    public FilterBasedItemSource(@NotNull List<NodeInfo> nodes) {
      super(FilterBasedItemSource.class.getName(), "Searching the database");
      myScope = nodes;
    }

    protected void reloadingPrepare(ItemsCollector collector) {
      clear(collector);

      final BasicScalarModel<Long> finalWCN = BasicScalarModel.createWithValue(null, true);
      final BasicScalarModel<Long> tempWCN = BasicScalarModel.createWithValue(null, true);
      final SynchronizedInt countWCNSources = new SynchronizedInt(0);

      for (NodeInfo nodeInfo : myScope) {
        final Connection connection = nodeInfo.getConnection();
        assert connection != null;

        ConnectionState state = connection.getState().getValue();
        if (state == null || !state.isReady())
          continue;

        BoolExpr<DP> filter = getFilter(connection);
        Constraint constraint = getConstraint(connection);
        if (filter == null || constraint == null)
          continue;

        Constraint nodeConstraint = nodeInfo.getConstraint();
        if (nodeConstraint != null) {
          constraint = CompositeConstraint.Simple.and(nodeConstraint, constraint);
        } else {
          // node constraint may be null for out-of-connection nodes
        }

        DBFilter view = QueryUtil.maybeGetHintedView(nodeInfo.getNode());
        if (view == null)
          view = nodeInfo.getView();
        if (view == null)
          continue;
        view = view.filter(filter);

        BasicScalarModel<LongList> localResultModel = BasicScalarModel.create(true);
        ItemViewAdapter adapter = ItemViewAdapter.create(view, localResultModel);
        adapter.setRequiredCNModel(collector, finalWCN);
        add(collector, adapter, 1000);

        if (!isSynchronized(nodeInfo)) {
          LoadingUserQueryItemSource source = new LoadingUserQueryItemSource(
            connection, getSearchDescription(), constraint, view, localResultModel,
            new Procedure<SyncTask>() {
              public void invoke(SyncTask syncTask) {
                long wcn = syncTask.getLastCommittedCN();
                synchronized (tempWCN.getLock()) {
                  final Long old = tempWCN.getValue();
                  if (old == null || old < wcn) {
                    tempWCN.setValue(wcn);
                  }
                }
                int sourcesLeft = countWCNSources.decrement();
                if (sourcesLeft == 0) {
                  finalWCN.setValue(tempWCN.getValue());
                }
              }
            }
          );
          add(collector, source, 10000);
          countWCNSources.increment();
        }
      }
      if (countWCNSources.get() == 0) {
        finalWCN.setValue(0l);
      }
    }

    private boolean isSynchronized(NodeInfo nodeInfo) {
      if (nodeInfo.isSynchronized())
        return true;
      Connection connection = nodeInfo.getConnection();
      if (connection != null && myCubeRegistry != null) {
        ItemHypercubeImpl cube = new ItemHypercubeImpl();
        long connectionItem = connection.getConnectionItem();
        cube.addValue(SyncAttributes.CONNECTION, connectionItem, true);
        return myCubeRegistry.isSynced(cube);
      }
      return false;
    }
  }
}
