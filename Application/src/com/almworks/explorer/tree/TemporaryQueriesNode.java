package com.almworks.explorer.tree;

import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.application.tree.TreeNodeFactory;
import com.almworks.items.api.Database;
import com.almworks.util.config.MapMedium;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.ThreadSafe;
import org.jetbrains.annotations.NotNull;

public class TemporaryQueriesNode extends GenericNodeImpl {
  private final ParentResult myResult = new ParentResult(this);

  public TemporaryQueriesNode(Database db) {
    super(db, new FixedText("Temporary Queries", Icons.NODE_FOLDER_OPEN, Icons.NODE_FOLDER_CLOSED),
      MapMedium.createConfig());
    addAllowedChildType(TreeNodeFactory.NodeType.QUERY);
    addAllowedChildType(TreeNodeFactory.NodeType.DISTRIBUTION_FOLDER);
  }

  @NotNull
  @ThreadSafe
  public QueryResult getQueryResult() {
    return myResult;
  }

  public boolean isCopiable() {
    return false;
  }

  public String getPositionId() {
    return null;
  }
}
