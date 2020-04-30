package com.almworks.explorer.tree;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.RemoteQuery;
import com.almworks.items.api.Database;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.model.CollectionModelEvent;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author dyoma
 */
class RemoteQueriesNode extends GenericNodeImpl implements CollectionModel.Consumer<RemoteQuery> {
  private final ConnectionNodeImpl myConnectionNode;
  private static final String NAME = "Remote Queries";
  private final ParentResult myResult = new ParentResult(this);

  public RemoteQueriesNode(Database db, ConnectionNodeImpl connectionNode, @NotNull CollectionModel<RemoteQuery> remoteQueriesModel) {
    super(db, FixedText.folder(NAME, Icons.NODE_FOLDER_OPEN, Icons.NODE_FOLDER_CLOSED),
      Configuration.EMPTY_CONFIGURATION);
    myConnectionNode = connectionNode;
    CollectionModel<RemoteQuery> queries = remoteQueriesModel;
    queries.getEventSource().addAWTListener(Lifespan.FOREVER, this);
  }

  public void onScalarsAdded(CollectionModelEvent<RemoteQuery> event) {
    Engine engine = myConnectionNode.getEngine();
    for (int i = 0; i < event.size(); i++) {
      RemoteQuery remoteQuery = event.get(i);
      addChildNode(new RemoteQueryNode(myConnectionNode.getEngine().getDatabase(), myConnectionNode.getNodeId(), remoteQuery));
    }
  }

  public void onScalarsRemoved(CollectionModelEvent<RemoteQuery> event) {
    for (int i = 0; i < event.size(); i++) {
      RemoteQuery remoteQuery = event.get(i);
      List<? extends GenericNode> children = getChildren();
      for (int j = 0; j < children.size(); j++) {
        GenericNode child = children.get(j);
        if (child instanceof RemoteQueryNode) {
          if (((RemoteQueryNode) child).getRemoteQuery().getQueryName().equals(remoteQuery.getQueryName())) {
            child.removeFromTree();
            break;
          }
        }
      }
    }
  }

  public void onContentKnown(CollectionModelEvent<RemoteQuery> event) {
  }

  public boolean isCopiable() {
    return false;
  }

  public String getNodeId() {
    return NAME + "@" + myConnectionNode.getNodeId();
  }

  public String getPositionId() {
    return null;
  }

  public QueryResult getQueryResult() {
    return myResult;
  }
}
