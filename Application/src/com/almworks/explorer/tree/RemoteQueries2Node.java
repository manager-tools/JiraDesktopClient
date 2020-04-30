package com.almworks.explorer.tree;

import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.application.tree.*;
import com.almworks.api.engine.RemoteQuery2;
import com.almworks.items.api.Database;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

import static com.almworks.util.images.Icons.NODE_SYSTEM_FOLDER_CLOSED;
import static com.almworks.util.images.Icons.NODE_SYSTEM_FOLDER_OPEN;

public class RemoteQueries2Node extends GenericNodeImpl {
  private static final String NAME = "Saved Filters";

  private final ScalarModel<Collection<RemoteQuery2>> myRemoteQueriesModel;
  private final ParentResult myResult = new ParentResult(this);
  private final Lifecycle myLife = new Lifecycle(false);
  private final ScalarModel.Adapter<Collection<RemoteQuery2>> myListener = new MyListener();

  public RemoteQueries2Node(Database db, ScalarModel<Collection<RemoteQuery2>> remoteQueriesModel, Configuration config) {
    super(db, FixedText.folder(NAME, NODE_SYSTEM_FOLDER_OPEN, NODE_SYSTEM_FOLDER_CLOSED), config);
    myRemoteQueriesModel = remoteQueriesModel;
  }

  @NotNull
  @ThreadSafe
  public QueryResult getQueryResult() {
    return myResult;
  }

  public boolean isCopiable() {
    return false;
  }

  public boolean isRemovable() {
    return false;
  }

  public String getNodeId() {
    ConnectionNode connection = getAncestorOfType(ConnectionNode.class);
    if (connection == null)
      return NAME;
    else
      return NAME + "@" + connection.getNodeId();
  }

  public String getPositionId() {
    return null;
  }

  public void onInsertToModel() {
    super.onInsertToModel();
    myLife.cycleStart();

    // if ThreadGate.AWT is used, the listener will be called immediately, while children has not yet been initialized
    myRemoteQueriesModel.getEventSource().addListener(myLife.lifespan(), ThreadGate.AWT_QUEUED, myListener);
  }

  public void onRemoveFromModel() {
    myLife.cycleEnd();
    super.onRemoveFromModel();
  }

  private void updateQueries(Collection<RemoteQuery2> queries) {
    if (!isNode())
      return;
    RootNode root = getRoot();
    if (root == null) {
      assert false : this;
      return;
    }
    TreeNodeFactory factory = root.getNodeFactory();

    Map<String, UserQueryNode> currentNodes = Collections15.hashMap();
    for (GenericNode child : getChildren()) {
      if (child instanceof UserQueryNode) {
        currentNodes.put(child.getName(), (UserQueryNode) child);
      }
    }

    boolean added = false;
    for (RemoteQuery2 query : queries) {
      String name = query.getDisplayableName();
      FilterNode filterNode = query.getFilterNode();
      UserQueryNode node = currentNodes.get(name);
      if (node == null) {
        node = factory.createUserQuery(this);
        node.setFilter(filterNode);
        node.getPresentation().setText(name);
        added = true;
      } else {
        node.setFilter(filterNode);
      }
    }

    if (added) {
      sortChildren();
    }
  }


  private class MyListener extends ScalarModel.Adapter<Collection<RemoteQuery2>> {
    public void onScalarChanged(ScalarModelEvent<Collection<RemoteQuery2>> event) {
      Collection<RemoteQuery2> queries = event.getNewValue();
      if (queries != null && queries.size() > 0) {
        updateQueries(queries);
      }
    }
  }
}
