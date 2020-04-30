package com.almworks.jira.provider3.gui.edit.editors;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.util.Pair;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class JiraEditUtils {
  public static Object getContextKey(EditFeature feature, ActionContext context) throws CantPerformException {
    List<ItemWrapper> items =
      CantPerformException.ensureNotEmpty(context.getSourceCollection(ItemWrapper.ITEM_WRAPPER));
    return getContextKey(feature, items.get(0));
  }

  public static Pair<EditFeature, Long> getContextKey(EditFeature feature, ItemWrapper wrapper) {
    return Pair.create(feature, wrapper.getItem());
  }

  @NotNull
  public static Pair<JiraConnection3, List<LoadedItem>> selectLoadedIssues(ActionContext context) throws CantPerformException {
    return selectIssues(context, LoadedItem.LOADED_ITEM);
  }

  @NotNull
  public static Pair<JiraConnection3, List<ItemWrapper>> selectIssuesWrappers(ActionContext context) throws CantPerformException {
    return selectIssues(context, ItemWrapper.ITEM_WRAPPER);
  }

  @NotNull
  private static <T extends ItemWrapper> Pair<JiraConnection3, List<T>> selectIssues(ActionContext context, DataRole<T> role)
    throws CantPerformException
  {
    JiraConnection3 connection = null;
    List<T> selected = Collections15.arrayList();
    for (T item : context.getSourceCollection(role)) {
      JiraConnection3 c = Util.castNullable(JiraConnection3.class, item.getConnection());
      if (item.services().isRemoteDeleted()) throw new CantPerformException();
      if (c == null || c.getState().getValue().isDegrading()) continue;
      if (connection == null) connection = c;
      else if (connection != c) throw new CantPerformException();
      selected.add(item);
    }
    CantPerformException.ensureNotNull(connection);
    CantPerformException.ensureNotEmpty(selected);
    return Pair.create(connection, selected);
  }

  public static JiraConnection3 getIssueConnection(ItemWrapper issue) throws CantPerformException {
    return CantPerformException.cast(JiraConnection3.class, issue.getConnection());
  }

  public static void checkAnyConnectionAllowsEdit(ActionContext context, EditDescriptor.Impl descriptor) throws CantPerformException {
    descriptor.putPresentationProperty(PresentationKey.ENABLE, EnableState.INVISIBLE);
    Engine engine = context.getSourceObject(Engine.ROLE);
    ConnectionManager manager = engine.getConnectionManager();
    CollectionModel<Connection> connections = manager.getConnections();
    boolean canEdit = false;
    for (Connection c : connections.copyCurrent()) {
      JiraConnection3 connection = Util.castNullable(JiraConnection3.class, c);
      if (connection != null && connection.isUploadAllowed()) {
        canEdit = true;
        break;
      }
    }
    CantPerformException.ensure(canEdit);
    descriptor.putPresentationProperty(PresentationKey.ENABLE, EnableState.ENABLED);
  }

  @Nullable
  public static JiraConnection3 findConnection(ActionContext context, @NotNull List<GenericNode> nodes) throws CantPerformException {
    JiraConnection3 connection = null;
    for (GenericNode node : nodes) {
      ConnectionNode connectionNode = node.getAncestorOfType(ConnectionNode.class);
      if (connectionNode == null) continue;
      JiraConnection3 c = CantPerformException.ensureNotNull(Util.castNullable(JiraConnection3.class, connectionNode.getConnection()));
      if (connection == null) connection = c;
      else CantPerformException.ensure(connection == c);
    }
    if (connection == null) {
      Engine engine = context.getSourceObject(Engine.ROLE);
      Collection<Connection> connections = engine.getConnectionManager().getReadyConnectionsModel().copyCurrent();
      if (connections.size() == 1) connection = Util.castNullable(JiraConnection3.class, connections.iterator().next());
    }
    return connection;
  }
}
