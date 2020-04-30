package com.almworks.explorer.tree;

import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.tree.*;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.FieldEqualsConstraint;
import com.almworks.api.engine.*;
import com.almworks.api.syncreg.*;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBReader;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasSection;
import com.almworks.util.components.EditableText;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.images.AlphaIcon;
import com.almworks.util.images.AnimatedIconWatcher;
import com.almworks.util.images.IconHandle;
import com.almworks.util.images.Icons;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.concurrent.Synchronized;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Map;

/**
 * @author : Dyoma
 */
class ConnectionNodeImpl extends GenericNodeImpl implements ConnectionNode {
  private static final IconHandle DEFAULT_CONNECTION_ICON = Icons.NODE_CONNECTION;
  private static final Icon OFFLINE_CONNECTION_ICON = new AlphaIcon(DEFAULT_CONNECTION_ICON, 0.5f);
  private static final String DEFAULT_CONNECTION_NAME = "---";

  private final Connection myConnection;
  private final Engine myEngine;

  private final Lifecycle myLife = new Lifecycle();
  private final Lifecycle myIconAnimationCycle = new Lifecycle();

  private final ConnectionNodeState myState = new ConnectionNodeState();
  private boolean myReady = false;
  private boolean myFullyReady = false;
  private boolean mySystemNodesCreated = false;
  private final long myConnectionItem;

  private static final String REMOTE_QUERIES_2_CONFIG = "remoteQueries2";

  private final Map<GenericNode, Integer> myOrderMap;
  private volatile OutboxNode myOutboxNode;

  public boolean isNarrowing() {
    return true;
  }

  public ConnectionNodeImpl(Connection connection, Engine engine, Configuration config, Map<GenericNode, Integer> orderMap) {
    super(engine.getDatabase(),
      createPresentation(
        DEFAULT_CONNECTION_NAME, DEFAULT_CONNECTION_ICON,
        connection, engine.getConnectionManager()), config);
    myConnection = connection;
    myConnectionItem = connection.getConnectionItem();
    myEngine = engine;
    myOrderMap = orderMap;
    updateConnectionName();
    addAllowedChildType(TreeNodeFactory.NodeType.FOLDER);
    addAllowedChildType(TreeNodeFactory.NodeType.QUERY);
    addAllowedChildType(TreeNodeFactory.NodeType.DISTRIBUTION_FOLDER);
    addAllowedChildType(TreeNodeFactory.NodeType.LAZY_DISTRIBUTION);
    addChildNode(new TemporaryQueriesNode(engine.getDatabase()));
  }

  public MyPresentation getPresentation() {
    return (MyPresentation) super.getPresentation();
  }

  private void createSystemNodes() {
    if (mySystemNodesCreated)
      return;
    mySystemNodesCreated = true;
    CollectionModel<RemoteQuery> remoteQueries = myConnection.getRemoteQueries();
    if (remoteQueries != null) {
      addChildNode(new RemoteQueriesNode(myEngine.getDatabase(), this, remoteQueries));
    }
    ScalarModel<Collection<RemoteQuery2>> remoteQueries2 = myConnection.getRemoteQueries2();
    if (remoteQueries2 != null) {
      addChildNode(
        new RemoteQueries2Node(myEngine.getDatabase(), remoteQueries2, getConfiguration().getOrCreateSubset(REMOTE_QUERIES_2_CONFIG)));
    }
  }

  public Connection getConnection() {
    return myConnection;
  }

  void updateConnectionName() {
    getPresentation().updateName(getConnectionName());
  }

  @Override
  public void onInsertToModel() {
    super.onInsertToModel();
    listenSyncState();
    listenConnectionState();
    listenConnectionArtifacts();
    updateConnectionName();
  }

  public void onRemoveFromModel() {
    myLife.cycle();
    myIconAnimationCycle.cycle();
    super.onRemoveFromModel();
  }

  public boolean isSynchronized() {
    RootNode root = getRoot();
    if (root == null)
      return false;
    SyncRegistry syncRegistry = root.getSyncRegistry();
    SyncCubeRegistry cubeRegistry = syncRegistry.getSyncCubeRegistry();
    if (cubeRegistry == null)
      return false;
    return cubeRegistry.isSynced(getHypercube(false));
  }

  private void listenSyncState() {
    final ScalarModel<SyncTask.State> syncState = myConnection.getConnectionSynchronizer().getState();
    syncState.getEventSource().addAWTListener(myLife.lifespan(), new ScalarModel.Adapter<SyncTask.State>() {
      public void onScalarChanged(ScalarModelEvent<SyncTask.State> event) {
        updateIcon();
        updateOutbox();
      }
    });
    final ScalarModel<Connection.AutoSyncMode> autoSyncMode = myConnection.getAutoSyncMode();
    autoSyncMode.getEventSource().addAWTListener(myLife.lifespan(), new ScalarModel.Adapter<Connection.AutoSyncMode>() {
      @Override
      public void onScalarChanged(ScalarModelEvent<Connection.AutoSyncMode> event) {
        updateIcon();
      }
    });
  }

  private void listenConnectionState() {
    ScalarModel.Adapter listener = new ScalarModel.Adapter() {
      public void onScalarChanged(ScalarModelEvent event) {
        checkConnectionState();
      }
    };
    myConnection.getState().getEventSource().addAWTListener(myLife.lifespan(), listener);
    myConnection.getInitializationState().getEventSource().addAWTListener(myLife.lifespan(), listener);
  }

  private void listenConnectionArtifacts() {
    myConnection.getViews().connectionItemsChange().addAWTChangeListener(myLife.lifespan(), new ChangeListener() {
      public void onChange() {
        updateArtifactView();
      }
    });
  }

  private void onConnectionState(final boolean ready) {
    NavigationTreeUtil.visitTree(this, ConnectionAwareNode.class, false, new ElementVisitor<ConnectionAwareNode>() {
      public boolean visit(ConnectionAwareNode element) {
        element.onConnectionState(ConnectionNodeImpl.this, ready);
        return true;
      }
    });
  }

  @ThreadAWT
  private void checkConnectionState() {
    ConnectionState state = myConnection.getState().getValue();
    if (state == ConnectionState.READY)
      updateConnectionName();
    updateIcon();
    InitializationState initializationState = myConnection.getInitializationState().getValue();
    boolean oldReady = myReady;
    myReady = state != null && state.isReady();
    myFullyReady = myReady && initializationState != null && initializationState.isInitialized();
    if (myFullyReady)
      createSystemNodes();
    updateArtifactView();
    if (oldReady != myReady)
      onConnectionState(myReady);
  }

  private void updateOutbox() {
    ConnectionViews views = myConnection.getViews();
    if ((myConnection.isUploadAllowed()))
    {
      if (myOutboxNode == null) {
        myOutboxNode = new OutboxNode(myEngine.getDatabase(), getNodeId(), views.getOutbox());
        addChildNode(myOutboxNode);
      }
    } else if (myOutboxNode != null) {
      myOutboxNode.removeFromTree();
      myOutboxNode = null;
    }
  }

  private void updateIcon() {
    InitializationState initializationState = myConnection.getInitializationState().getValue();
    Icon icon;
    String message;
    boolean animated = false;
    if (initializationState != null && initializationState.isInitializing()) {
      icon = Icons.NODE_CONNECTION_INITIALIZING.getIcon();
      message = "Connection is initializing";
      animated = true;
    } else if(initializationState == null || !initializationState.isInitialized()) {
      icon = Icons.NODE_CONNECTION_WITH_ALERT;
      message = "Connection is not initialized";
    } else {
      SyncTask.State syncState = myConnection.getConnectionSynchronizer().getState().getValue();
      if (syncState == SyncTask.State.NEVER_HAPPENED) {
        icon = Icons.NODE_CONNECTION_WITH_ALERT;
        message = "Connection was never synchronized";
      }
      else if (syncState == SyncTask.State.FAILED) {
        icon = Icons.NODE_CONNECTION_WITH_ALERT;
        message = "Connection synchronization has failed";
      }
      else if (syncState == SyncTask.State.SUSPENDED) {
        icon = Icons.NODE_CONNECTION_WITH_ALERT;
        message = "Connection synchronization has been suspended";
      }
      else {
        icon = Icons.NODE_CONNECTION;
        message = null;

      }
    }
    if(myConnection.getAutoSyncMode().getValue() != Connection.AutoSyncMode.AUTOMATIC) {
      if (!animated) icon = new AlphaIcon(icon, 0.5f);
      if (message == null) message = "Automatic synchronization is disabled";
    }
    setIcon(icon, message, animated);
  }

  @ThreadAWT
  private void updateArtifactView() {
    Procedure<DBFilter> doUpdate = new Procedure<DBFilter>() {
      @Override
      public void invoke(DBFilter viewableItems) {
        myState.setView(viewableItems);
        invalidatePreviewSafe();
      }
    };
    if (myReady) {
      DBFilter allConnectionItems = myConnection.getViews().getConnectionItems();
      myConnection.adjustView(allConnectionItems, myLife.lifespan(), doUpdate);
    } else {
      doUpdate.invoke(null);
    }
  }

  private void setIcon(Icon icon, String alert, boolean animated) {
    if(getPresentation().setConnectionState(icon, alert)) {
      fireTreeNodeChanged();
      myIconAnimationCycle.cycle();
      if(animated) {
        beginAnimation(icon);
      }
    }
  }

  private void beginAnimation(Icon icon) {
    AnimatedIconWatcher.INSTANCE.registerPainter(
      icon, myIconAnimationCycle.lifespan(),
      new ChangeListener() {
        @Override
        public void onChange() {
          getPresentation().fireChanged();
        }
    });
  }

  private static EditableText createPresentation(String connectionName, Icon icon, final Connection connection,
    final ConnectionManager connectionManager)
  {
    return new MyPresentation(connectionName, icon, connection, connectionManager);
  }

  public String getConnectionName() {
    return myEngine.getConnectionManager().getConnectionName(myConnection.getConnectionID());
  }

  public boolean isConnectionReady() {
    return myReady;
  }

  public GenericNode findTemporaryFolder() {
    for (int i = 0; i < getChildrenCount(); i++) {
      GenericNode child = getChildAt(i);
      if (child instanceof TemporaryQueriesNode)
        return child;
    }
    return null;
  }

  @Override
  public GenericNode getOutboxNode() {
    return myOutboxNode;
  }

  public ItemHypercube getHypercube(boolean strict) {
    ItemHypercube hypercube = super.getHypercube(strict);
    if (hypercube == null)
      hypercube = new ItemHypercubeImpl();
    ItemHypercubeUtils.adjustForConnection(hypercube, myConnection);
    return myConnection.adjustHypercube(hypercube);
  }

  public boolean isCopiable() {
    return false;
  }

  @ThreadAWT
  public int compareToSame(GenericNode that) {
    Integer thisOrder = myOrderMap.get(this);
    Integer thatOrder = myOrderMap.get(that);
    if (thisOrder == null) {
      if (thatOrder == null) {
        return super.compareToSame(that);
      } else {
        return 1;
      }
    } else {
      if (thatOrder == null) {
        return -1;
      } else {
        return Containers.compareInts(thisOrder, thatOrder);
      }
    }
  }

  public Engine getEngine() {
    return myEngine;
  }

  @NotNull
  public QueryResult getQueryResult() {
    return myState;
  }

  @ThreadAWT
  protected boolean isPreviewAvailable() {
    return myState.getDbFilter() != null;
  }

  @Nullable
  protected ItemsPreview calculatePreview(Lifespan lifespan, DBReader reader) {
    DBFilter view = myState.getDbFilter();
    if (view == null)
      return new ItemsPreview.Unavailable();
    if (lifespan.isEnded())
      return null;
    long start = System.currentTimeMillis();
    ItemsPreview preview = CountPreview.scanView(view, lifespan, reader);
    long duration = System.currentTimeMillis() - start;
    Log.debug("ConnectionNodePreview: " + duration + "ms/" + (preview != null ? preview.getItemsCount() : "<cancelled> ") + "count " + this);
    return preview;
  }

  private class ConnectionNodeState extends AbstractQueryResult {
    private final Synchronized<DBFilter> myView = new Synchronized<DBFilter>(null);

    @Override
    public Constraint getValidConstraint() {
      if (getDbFilter() != null) {
        Constraint connectionConstraint =
          FieldEqualsConstraint.Simple.create(SyncAttributes.CONNECTION, myConnectionItem);
        return myConnection.adjustConstraint(connectionConstraint);
      } else {
        return null;
      }
    }

    @Override
    public boolean isRunnable() {
      return false;
    }

    @Override
    @Nullable
    public ItemSource getItemSource() {
      return null;
    }

    @Override
    @Nullable
    public ItemCollectionContext getCollectionContext() {
      return null;
    }

    private void setView(DBFilter view) {
      DBFilter oldView = myView.set(view);
      if (!Util.equals(oldView, view)) {
        fireChanged();
      }
    }

    @Override
    @Nullable
    public DBFilter getDbFilter() {
      return myView.get();
    }

    @Override
    public long getVersion() {
      return 0;
    }

    @Override
    @Nullable
    public ItemHypercube getHypercube(boolean precise) {
      return ConnectionNodeImpl.this.getHypercube(precise);
    }
  }

  public int compareTo(GenericNode genericNode) {
    int i = super.compareTo(genericNode);
    if (i != 0)
      return i;
    return getConnectionName().compareToIgnoreCase(((ConnectionNode) genericNode).getConnectionName());
  }

  public boolean isRenamable() {
    return true;
  }

  private static class MyPresentation extends EditableText {
    private final Connection myConnection;
    private final ConnectionManager myConnectionManager;
    private InitializationState myInitState = null;
    private String myTooltip;

    public MyPresentation(String connectionName, Icon icon, Connection connection,
      ConnectionManager connectionManager)
    {
      super(connectionName, icon);
      myConnection = connection;
      myConnectionManager = connectionManager;
    }

    public void renderOn(Canvas canvas, CellState state) {
      super.renderOn(canvas, state);
      if(myInitState != null && myInitState.isInitializing()) {
        append(canvas, state, "(initializing\u2026)", CounterPresentation.COUNT_COLOR);
      } else if(myInitState == null || !myInitState.isInitialized()) {
        append(canvas, state, "(not initialized)", CounterPresentation.getGrey(state));
      }
    }

    private void append(Canvas canvas, CellState state, String text, Color color) {
      final CanvasSection section = canvas.newSection();
      section.setBackground(state.getDefaultBackground());
      section.setForeground(color);
      section.setBorder(CounterPresentation.COUNT_BORDER);
      section.appendText(text);
    }

    public boolean setText(String text) {
      boolean changed = super.setText(text);
      if (!changed)
        return false;
      renameConnection(myConnection, text, myConnectionManager);
      return changed;
    }

    public boolean setConnectionState(Icon icon, String hint) {
      boolean changed = setIcon(icon);
      changed |= Util.equals(myTooltip, hint);
      myTooltip = hint;
      return changed;
    }

    public void storeName(Lifespan life, Configuration configuration, String settingName) {
    }

    public void updateName(String connectionName) {
      setText(connectionName);
      myInitState = myConnection.getInitializationState().getValue();
    }

    private static void renameConnection(Connection connection, String newName, ConnectionManager connectionManager) {
      ReadonlyConfiguration config = connection.getConfiguration();
      if (Util.equals(newName, config.getSetting(CommonConfigurationConstants.CONNECTION_NAME, null)))
        return;
      Configuration configCopy = ConfigurationUtil.copy(config);
      configCopy.setSetting(CommonConfigurationConstants.CONNECTION_NAME, newName);
      connectionManager.updateConnection(connection, configCopy);
    }
  }
}
