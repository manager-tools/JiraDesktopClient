package com.almworks.api.application.tree;

import com.almworks.api.application.qb.FilterNode;
import com.almworks.util.components.EditableText;
import com.almworks.util.ui.actions.DataRole;

public interface QueryNode extends RenamableNode, GenericNode {
  DataRole<QueryNode> QUERY_NODE = DataRole.createRole(QueryNode.class);

  void setFilter(FilterNode filter);

  EditableText getPresentation();

  FilterNode getFilterStructure();
}
