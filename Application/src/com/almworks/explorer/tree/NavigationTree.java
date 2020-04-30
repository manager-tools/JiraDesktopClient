package com.almworks.explorer.tree;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.tree.*;
import com.almworks.api.config.ConfigNames;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.container.EventRouter;
import com.almworks.api.engine.*;
import com.almworks.api.syncreg.SyncRegistry;
import com.almworks.items.api.*;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.UtilConfigNames;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.TreeState;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.almworks.util.Collections15.*;

/**
 * @author : Dyoma
 */
public class NavigationTree {
  public static final DataRole<JComponent> NAVIGATION_JTREE = DataRole.createRole(JComponent.class);
  public static final ComponentProperty<ScalarModel<String>> TREE_FILTER_MODEL =
    ComponentProperty.createProperty("treeFilterModel");

  private static final DataRole[] DATA_ROLES = new DataRole[] {RenamableNode.RENAMEABLE};
  private static final DataRole[] GLOBAL_ROLES =
    new DataRole[] {GenericNodeImpl.NAVIGATION_NODE, UserQueryNode.USER_QUERY_NODE, ConnectionNode.CONNECTION_NODE};

  private final ComponentContainer myContainer;
  private final Configuration myConfig;
  private final Lifecycle myConnectionsLife = new Lifecycle();
  private final EventRouter myEventRouter;
  private final Map<String, ConnectionNodeImpl> myConnectionNodes = hashMap();
  /**
   * Contains requests for creation of default queries that were added before connection node was created. Confined to AWT thread.
   */
  private final Map<String, Procedure<ConnectionNodeImpl>> myWaitingDefaultQueries = hashMap();
  private final SyncRegistry mySyncRegistry;
  private final Database myDb;

  private final Bottleneck mySyncUpdater = new Bottleneck(500, ThreadGate.AWT, new Runnable() {
    public void run() {
      checkQueriesSyncState();
    }
  });
  private boolean mySyncUpdateMore = false;
  private boolean mySyncUpdateLess = false;

  private RootNode myRoot;
  private TreeNodeFactoryImpl myNodeFactory;

  private static final long ONE_MINUTE = 60 * 1000;
  private static final long THIRTY_MINUTES = 30 * ONE_MINUTE;
  private Set<ATreeNode<GenericNode>> mySelectionPaths = hashSet();
  private Set<ATreeNode<GenericNode>> myNewPaths = hashSet();

  private final Map<ATreeNode<GenericNode>, Boolean> myUserFilter = hashMap();
  private boolean myUserFilterActive = false;
  private Bottleneck myUserFilterResyncBottleneck;

  private final BasicScalarModel<Boolean> myReady = BasicScalarModel.createWithValue(false, true);
  private final Set<Connection> myConnectionsStartCount = hashSet();

  private final Bottleneck myRecountBottleneck = new Bottleneck(50, ThreadGate.AWT, new Runnable() {
    public void run() {
      Log.debug("NavTree invalidate start");
      long start = System.currentTimeMillis();
      myRoot.invalidatePreview();
      long duration = System.currentTimeMillis() - start;
      Log.debug("NavTree invalidate done: " + duration + "ms");
    }
  });
  private ATree<ATreeNode<GenericNode>> myTree;

  private final Map<GenericNode, Integer> myOrderMap = linkedHashMap();

  public NavigationTree(ComponentContainer container, Configuration config, EventRouter router, SyncRegistry syncRegistry, Database db) {
    myConfig = config;
    myContainer = container;
    myEventRouter = router;
    mySyncRegistry = syncRegistry;
    myDb = db;
  }

  public void createDefaultQueries(Connection connection, final Runnable whenCreated) {
    ConnectionNodeImpl connectionNode = myConnectionNodes.get(connection.getConnectionID());
    if (connectionNode != null) {
      doCreateDefaultQueries(whenCreated, connectionNode);
    } else {
      // schedule creation until later
      myWaitingDefaultQueries.put(connection.getConnectionID(), new Procedure<ConnectionNodeImpl>() {
        @Override
        public void invoke(ConnectionNodeImpl node) {
          doCreateDefaultQueries(whenCreated, node);
        }
      });
    }
  }

  private void doCreateDefaultQueries(Runnable whenCreated, ConnectionNodeImpl connectionNode) {
    CreateDefaultQueries.perform(connectionNode, myNodeFactory, whenCreated);
    GenericNode temporaryFolder = connectionNode.findTemporaryFolder();
    if (temporaryFolder != null) {
      NoteNode note = myNodeFactory.createNote(temporaryFolder);
      note.setName(
        "Contents of this folder will be cleared each time " + Local.text(Terms.key_Deskzilla) + " starts");
    }
    GenericNode userQueries = myNodeFactory.createFolder(connectionNode);
    ((EditableText) userQueries.getPresentation()).setText("User Queries");
//      connectionNode.addChildNode(detachedNote(1, "Double-click on a query to run it."));
//      connectionNode.addChildNode(detachedNote(2, "Open \"Sample Queries\" and browse the project."));
//      connectionNode.addChildNode(detachedNote(3, "Click on \"User Queries\" and press F3 to create a new query."));
//      connectionNode.addChildNode(detachedNote(4, "Use drag-and-drop to move and copy queries."));
    connectionNode.sortChildren();
  }

  public void expandConnectionNode(Connection connection, boolean expandAll) {
    assert myNodeFactory != null;
    ATree<?> tree = myNodeFactory.getTree();
    ConnectionNodeImpl connectionNode = myConnectionNodes.get(connection.getConnectionID());
    if (connectionNode != null) {
      if (expandAll)
        tree.expandAll(connectionNode.getTreeNode());
      else
        tree.expand(connectionNode.getTreeNode());
    }
  }

  @Nullable
  public ConnectionNode getConnectionNode(@NotNull Connection connection) {
    return myConnectionNodes.get(connection.getConnectionID());
  }

  /**
   * @noinspection FeatureEnvy
   */
  public void setupTree() {
    assert myNodeFactory != null : "Should be started";

    myTree = myNodeFactory.getTree();
    TreeModelBridge<GenericNode> fullModel = myRoot.getTreeNode();
    TreeModelFilter<GenericNode> filtered = TreeModelFilter.create();
    filtered.setSourceRoot(fullModel);

    filtered.setFilter(new Condition<ATreeNode<GenericNode>>() {
      public boolean isAccepted(ATreeNode<GenericNode> node) {
        if (myUserFilterActive) {
          if (isNodeFilteredOutByUser(node)) {
            return false;
          }
        }
        return isNodeShown(node.getUserObject());
      }
    });
    myTree.setRoot(filtered.getFilteredRoot());
    myTree.setDataRoles(DATA_ROLES);
    ConstProvider.addGlobalValue(myTree.getTargetComponent(), NAVIGATION_JTREE, myTree.getTargetComponent());
    myTree.addGlobalRoles(GLOBAL_ROLES);
    listenSelectionPath(myTree);
    myTree.setCanvasRenderer(createNodeRenderer());
    myTree.setTransfer(new NavigationTreeStringTransfer(myDb, myTree));
    setupLargeModel();
    setupShowHideEmptyQueriesAction(myTree.getScrollable());
    setupListenFilterModel(myTree, filtered);

    NameResolver nameResolver = myContainer.getActor(NameResolver.ROLE);
    assert nameResolver != null;
    //nameResolver.
//    final DetachComposite detach = new DetachComposite(true);
//    nameResolver.getEnumDescriptorsReady().getEventSource().addAWTListener(detach, new ScalarModel.Adapter<Boolean>() {
//      public void onScalarChanged(ScalarModelEvent<Boolean> event) {
//        Boolean value = event.getNewValue();
//        if (value != null && value) {
//          detach.detach();
//          rebuildConnections();
//        }
//      }
//    });
    rebuildConnections();

    ConstProvider.addGlobalValue(myTree, TreeNodeFactory.TREE_NODE_FACTORY, myNodeFactory);
    TreeState state = new TreeState(myTree, myConfig.getOrCreateSubset(UtilConfigNames.TREE_EXPANSION_KEY)) {
      protected void updateStateFrom(TreePath path) {
        ATreeNode<GenericNode> node = (ATreeNode<GenericNode>) path.getLastPathComponent();
        if (node.getUserObject() instanceof ConnectionNode && !node.isLeaf())
          expand(node);
        super.updateStateFrom(path);
      }
    };
    state.expand(myRoot.getTreeNode());

    listenExpansion(myTree);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        checkQueriesSyncState();
        myRoot.getItemsPreviewManager().setActive(true);
      }
    });
  }

  private void setupLargeModel() {
    JTree tree = myTree.getScrollable();
    tree.setLargeModel(true);
    TreeModelBridge<GenericNode> sampleNode =
      new TreeModelBridge<GenericNode>(new FolderNode(myDb, "XXXXXXX", Configuration.EMPTY_CONFIGURATION));
    Component component =
      tree.getCellRenderer().getTreeCellRendererComponent(tree, sampleNode, true, true, false, 0, true);
    Dimension size = component.getPreferredSize();
    tree.setRowHeight(size.height);
  }

  private void listenExpansion(final ATree<ATreeNode<GenericNode>> tree) {
    final JTree jtree = tree.getScrollable();
    jtree.addTreeExpansionListener(new TreeExpansionListener() {
      public void treeExpanded(TreeExpansionEvent event) {
        TreePath path = event.getPath();
        if (!jtree.isVisible(path)) {
          return;
        }
        ATreeNode tnode = (ATreeNode) path.getLastPathComponent();
        GenericNode node = (GenericNode) tnode.getUserObject();
        requestRecountVisible(node, tree);
      }

      public void treeCollapsed(TreeExpansionEvent event) {
      }
    });
    requestRecountVisible(myRoot, tree);
  }

  private void requestRecountVisible(GenericNode node, ATree tree) {
    // recount grand-children if expanded
    boolean expanded = tree.isExpanded(node.getTreeNode());
    for (int i = 0; i < node.getChildrenCount(); i++) {
      GenericNode child = node.getChildAt(i);
      child.maybeSchedulePreview();
      if (expanded) {
        requestRecountVisible(child, tree);
      }
    }
  }


  private void setupListenFilterModel(ATree<ATreeNode<GenericNode>> tree, final TreeModelFilter<GenericNode> filtered) {
    final ScalarModel<String> model = TREE_FILTER_MODEL.getClientValue(tree);
    if (model == null)
      return;
    myUserFilterResyncBottleneck = new Bottleneck(350, ThreadGate.AWT, new Runnable() {
      public void run() {
        resyncUserFilter(model.getValue(), filtered);
      }
    });
    model.getEventSource().addAWTListener(Lifespan.FOREVER, new ScalarModel.Adapter<String>() {
      public void onScalarChanged(ScalarModelEvent<String> event) {
        if (!Util.equals(event.getOldValue(), event.getNewValue())) {
          myUserFilterResyncBottleneck.requestDelayed();
        }
      }
    });
  }

  private void resyncUserFilter(String filter, TreeModelFilter<GenericNode> filtered) {
    if (filter == null || filter.trim().length() == 0) {
      boolean wasActive = myUserFilterActive;
      myUserFilterActive = false;
      if (wasActive) {
        myUserFilter.clear();
        filtered.resyncAll();
      }
    } else {
      myUserFilter.clear();
      myUserFilterActive = true;
      filter = Util.lower(filter.trim());
      PlainTextCanvas canvas = new PlainTextCanvas();
      doResyncUserFilterOnNode(myRoot, filter, canvas);
      filtered.resyncAll();
    }
  }

  private boolean doResyncUserFilterOnNode(GenericNode node, String filter, PlainTextCanvas canvas) {
    boolean show = false;
    for (int i = 0; i < node.getChildrenCount(); i++) {
      GenericNode child = node.getChildAt(i);
      boolean c = doResyncUserFilterOnNode(child, filter, canvas);
      show |= c;
    }
    if (!show) {
      if (node instanceof ConnectionNode || node instanceof TagsFolderNode) {
        show = true;
      } else {
        canvas.clear();
        node.getPresentation().renderOn(canvas, CellState.LABEL);
        String text = Util.lower(canvas.getText());
        show = text.startsWith(filter);
      }
    }
    myUserFilter.put(node.getTreeNode(), show);
    return show;
  }


  private boolean isNodeFilteredOutByUser(ATreeNode<GenericNode> node) {
    Boolean b = myUserFilter.get(node);
    return b != null && !b;
  }

  private void listenSelectionPath(final ATree<ATreeNode<GenericNode>> tree) {
    tree.getSelectionAccessor().addAWTChangeListener(Lifespan.FOREVER, new ChangeListener() {
      public void onChange() {
        Threads.assertAWTThread();
        List<ATreeNode<GenericNode>> selected = tree.getSelectionAccessor().getSelectedItems();
        assert myNewPaths.size() == 0;
        for (ATreeNode<GenericNode> selection : selected) {
          for (ATreeNode<GenericNode> n = selection.getParent(); n != null; n = n.getParent()) {
            myNewPaths.add(n);
          }
        }
        myNewPaths.removeAll(selected);
        Set<ATreeNode<GenericNode>> oldPaths = mySelectionPaths;
        mySelectionPaths = myNewPaths;
        myNewPaths = oldPaths;
        List<ATreeNode<GenericNode>> notifications = null;
        for (ATreeNode<GenericNode> node : mySelectionPaths) {
          if (!oldPaths.remove(node)) {
            // a new node
            if (notifications == null)
              notifications = arrayList();
            notifications.add(node);
          }
        }
        if (oldPaths.size() > 0) {
          if (notifications == null)
            notifications = arrayList();
          notifications.addAll(oldPaths);
        }
        myNewPaths.clear();
        if (notifications != null) {
          for (ATreeNode<GenericNode> node : notifications) {
            tree.invalidateNode(node);
          }
        }
      }
    });
  }

  private void setupShowHideEmptyQueriesAction(final JTree tree) {
    tree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1 && e.isAltDown()) {
          // alt-click
          Point point = e.getPoint();
          TreePath path = tree.getPathForLocation(point.x, point.y);
          if (path == null)
            return;
          Object treeNode = path.getLastPathComponent();
          if (!(treeNode instanceof ATreeNode))
            return;
          GenericNode node = ((ATreeNode<GenericNode>) treeNode).getUserObject();
          e.consume();
          if(node.canHideEmptyChildren()) {
            // reversing logic
            boolean current = node.getHideEmptyChildren();
            node.setHideEmptyChildren(!current);
          }
        }
      }
    });
  }

  private boolean isOnSelectionPath(ATreeNode<GenericNode> item) {
    return mySelectionPaths.contains(item);
  }

  private CanvasRenderer<ATreeNode<GenericNode>> createNodeRenderer() {
    return new Renderers.DefaultCanvasRenderer<ATreeNode<GenericNode>>(CanvasRenderable.EMPTY) {
      private Color myCachedStateForegroundColor;

      public void renderStateOn(CellState state, Canvas canvas, ATreeNode<GenericNode> item) {
        if (isOnSelectionPath(item))
          canvas.setFontStyle(Font.BOLD);
        super.renderStateOn(state, canvas, item);
//        GenericNode node = item.getUserObject();
//        if (node != null) {
//          Boolean hide = node.getHideEmptyChildren();
//          if (hide != null) {
//            CanvasSection section = canvas.newSection();
//            section.setBackground(state.getDefaultBackground());
//            if (myCachedStateForegroundColor == null) {
//              myCachedStateForegroundColor =
//                ColorUtil.between(state.getDefaultForeground(), state.getDefaultBackground(), 0.5F);
//            }
//            section.setFontStyle(Font.PLAIN);
//            section.setForeground(myCachedStateForegroundColor);
//            section.setBorder(AwtUtil.EMPTY_BORDER);
//            section.appendText(" (hide empty queries: ").appendText(hide ? "ON" : "OFF").appendText(")");
//          }
//        }
      }
    };
  }

  private boolean isNodeShown(GenericNode node) {
    if (node instanceof DistributionGroupNode) {
      for (int i = 0; i < node.getChildrenCount(); i++)
        if (isNodeShown(node.getChildAt(i)))
          return true;
      return false;
    }
    if (!(node instanceof QueryNode))
      return true;
    if (!node.isSynchronized())
      return true;
    if (!node.getQueryResult().canRunNow())
      return true;
    int count = node.getCusionedPreviewCount();
    if (count != 0)
      return true;
    return !isNodeHiddenWhenEmpty(node);
  }

  private boolean isNodeHiddenWhenEmpty(GenericNode node) {
    if (node == null)
      return false;
    node = node.getParent();
    while (node != null) {
      Boolean hide = node.getHideEmptyChildren();
      if (hide != null)
        return hide;
      node = node.getParent();
    }
    return false;
  }

  private void rebuildConnections() {
    Threads.assertAWTThread();
    Log.debug("rebuilding connection nodes");
    myConnectionsLife.cycle();
    Collection<Connection> connections = getEngine().getConnectionManager().getConnections().copyCurrent();
    myConnectionsStartCount.clear();
    if (connections.isEmpty()) {
      myReady.commitValue(false, true);
    } else {
      if (!myReady.getValue()) {
        myConnectionsStartCount.addAll(connections);
        Log.debug("NT: waiting for start of " + connections.size() + " connections");
      }
      for (final Connection connection : connections) {
        ScalarModel<ConnectionState> state = connection.getState();
        state.getEventSource().addAWTListener(myConnectionsLife.lifespan(), new ScalarModel.Adapter<ConnectionState>() {
          public void onScalarChanged(ScalarModelEvent<ConnectionState> event) {
            ConnectionState state = event.getNewValue();
            if (state == ConnectionState.STARTING) {
              Log.debug("NT: " + connection + " starting");
              removeConnectionNode(connection.getConnectionID());
              maybeCreateConnectionLoadingNode(connection);
            } else if (state == ConnectionState.READY) {
              Log.debug("NT: " + connection + " started");
              maybeCreateConnectionNode(connection);
            } else {
              if (state != ConnectionState.INITIAL) {
                countConnection(connection, "state=" + state);
              }
              removeConnectionNode(connection.getConnectionID());
              Log.debug("NT: " + connection + " removed");
            }
          }
        });
      }
    }
  }

  @ThreadAWT
  private void countConnection(Connection connection, String reason) {
    Threads.assertAWTThread();
    if (myConnectionsStartCount.remove(connection)) {
      Log.debug("NT: got start from " + connection + " (" + reason + ")");
    }
    if (myConnectionsStartCount.isEmpty()) {
      Log.debug("NT: all connections started");
      myReady.commitValue(false, true);
    }
  }

  private ConnectionNodeImpl maybeCreateConnectionNode(final Connection connection) {
    final String connectionID = connection.getConnectionID();
    ConnectionNodeImpl node = myConnectionNodes.get(connectionID);
    if (node == null) {
      EngineUtils.runWhenConnectionDescriptorsAreReady(connection, ThreadGate.AWT_QUEUED, new Runnable() {
        public void run() {
          Log.debug("NT: " + connection + " descriptors ready");
          try {
            maybeRemoveConnectionLoadingNode(connection);
            ConnectionNodeImpl node = myConnectionNodes.get(connectionID);
            if (node == null) {
              node = createConnectionNode(connection);
              node.updateConnectionName();
            }
          } finally {
            try {
              countConnection(connection, "descriptors ready");
            } catch (Exception e) {
              Log.error(e);
            }
          }
        }
      });
    } else {
      countConnection(connection, "node exists");
      maybeRemoveConnectionLoadingNode(connection);
      node.updateConnectionName();
    }
    return node;
  }

  private void maybeCreateConnectionLoadingNode(Connection connection) {
    if (getConnectionLoadingNodeIndex(connection) != null)
      return;
    Log.debug("NT: creating -loading- node for " + connection);
    myRoot.addChildNode(new ConnectionLoadingNodeImpl(myDb, myRoot.getConfiguration(), connection));
  }

  private ConnectionLoadingNode getConnectionLoadingNodeIndex(Connection connection) {
    for (int i = 0; i < myRoot.getChildrenCount(); i++) {
      GenericNode child = myRoot.getChildAt(i);
      if (child instanceof ConnectionLoadingNode) {
        ConnectionLoadingNode node = (ConnectionLoadingNode) child;
        if (node.getConnection() == connection) {
          return node;
        }
      }
    }
    return null;
  }

  private void maybeRemoveConnectionLoadingNode(Connection connection) {
    ConnectionLoadingNode node = getConnectionLoadingNodeIndex(connection);
    if (node != null) {
      Log.debug("NT: removing -loading- node for " + connection);
      node.removeFromTree();
    }
  }

  private ConnectionNodeImpl createConnectionNode(Connection connection) {
    Log.debug("NT: creating node: " + connection);

    String connectionID = connection.getConnectionID();
    Configuration connectionConfig = getConnectionNodeConfig(connectionID);
    ConnectionNodeImpl node = new ConnectionNodeImpl(connection, getEngine(), connectionConfig, myOrderMap);

    myConnectionNodes.put(connectionID, node);
    Procedure<ConnectionNodeImpl> createDefaultQueries = myWaitingDefaultQueries.get(connectionID);
    if (createDefaultQueries != null) {
      createDefaultQueries.invoke(node);
    }
    updateOrderMap();

    // sortChildren should come after order map is updated
    myRoot.addChildNode(node);
    long start = System.currentTimeMillis();
    Log.debug("Sorting tree " + connection);
    myRoot.sortChildren();
    long duration = System.currentTimeMillis() - start;
    Log.debug("Sorting tree done " + duration + "ms " + connection);
    return node;
  }

  @ThreadAWT
  private void updateOrderMap() {
    myOrderMap.clear();
    Collection<Connection> connections = getEngine().getConnectionManager().getConnections().copyCurrent();
    int position = 0;
    for (Connection connection : connections) {
      ++position;
      ConnectionNodeImpl node = myConnectionNodes.get(connection.getConnectionID());
      if (node != null) {
        myOrderMap.put(node, position);
      }
    }
  }

  // TODO remove config migration
  private Configuration getConnectionNodeConfig(String connectionID) {
    Configuration connectionNodes = myConfig.getOrCreateSubset("connectionNodes");
    if (connectionNodes.isSet(connectionID))
      return connectionNodes.getSubset(connectionID);
    Configuration movedConfig = connectionNodes.getOrCreateSubset(connectionID);
    movedConfig.setSetting(ConfigNames.HIDE_EMPTY_CHILDREN, true);
    if (!myConfig.isSet(connectionID))
      return movedConfig;
    Configuration oldConfig = myConfig.getSubset(connectionID);
    ConfigurationUtil.copyTo(oldConfig, movedConfig);
    oldConfig.removeMe();
    return movedConfig;
  }

  @NotNull
  private Engine getEngine() {
    Engine engine = myContainer.getActor(Engine.ROLE);
    assert engine != null;
    return engine;
  }

  private int removeConnectionNode(String connectionID) {
    ConnectionNodeImpl node;
    node = myConnectionNodes.get(connectionID);
    int index;
    if (node != null) {
      index = node.removeFromTree();
      myConnectionNodes.remove(connectionID);
    } else {
      index = -1;
    }
    return index;
  }

  @ThreadAWT
  public void start(final ATree<ATreeNode<GenericNode>> tree) {
    assert myNodeFactory == null : "Should be called once";
    myNodeFactory = new TreeNodeFactoryImpl(myDb, tree);
    NodeIdUtil.setConfig(myConfig.getOrCreateSubset("nodeidfactory"));
    myRoot = GenericNodeImpl.createRootNode(myContainer, myDb, "Root", myConfig.getOrCreateSubset("Root"), mySyncRegistry,
      myNodeFactory);
    myEventRouter.addListener(Lifespan.FOREVER, ThreadGate.AWT_QUEUED, new EngineListener.Adapter() {
      public void onConnectionsChanged() {
        rebuildConnections();
      }
    });

    /// here aggregate, avoid awt gating
    myDb.addListener(Lifespan.FOREVER, new DBListener() {
      public void onDatabaseChanged(DBEvent event, DBReader reader) {
        myRecountBottleneck.request();
      }
    });

    mySyncRegistry.getEventSource().addAWTListener(Lifespan.FOREVER, new SyncRegistry.Listener() {
      public void onSyncRegistryChanged(boolean moreSynchronized, boolean lessSynchronized) {
        mySyncUpdateMore |= moreSynchronized;
        mySyncUpdateLess |= lessSynchronized;
        mySyncUpdater.requestDelayed();
      }
    });
  }

  @ThreadAWT
  private void checkQueriesSyncState() {
    final boolean more = mySyncUpdateMore;
    final boolean less = mySyncUpdateLess;
    mySyncUpdateMore = false;
    mySyncUpdateLess = false;
    if (!more && !less)
      return;
    NavigationTreeUtil.visitTree(myRoot, AbstractQueryNode.class, false,
      new ElementVisitor<AbstractQueryNode>() {
        public boolean visit(AbstractQueryNode node) {
          node.checkSyncStateAndFire(more, less);
          return true;
        }
      });
  }

  public Map<DBIdentifiedObject, TagNode> getTags() {
    return myRoot.getTags();
  }

  public RootNode getRootNode() {
    return myRoot;
  }

  public ScalarModel<Boolean> isReady() {
    return myReady;
  }

  public void selectNode(GenericNode node) {
    TreeModelBridge<GenericNode> parentNode = node.getTreeNode().getParent();
    if (!myTree.isExpanded(parentNode))
      myTree.expand(parentNode);
    myTree.getSelectionAccessor().setSelected(node.getTreeNode());
    myTree.scrollSelectionToView();
  }
}

