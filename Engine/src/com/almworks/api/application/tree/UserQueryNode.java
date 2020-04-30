package com.almworks.api.application.tree;

import com.almworks.api.application.qb.QueryBuilderComponent;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.TypedKey;

/**
 * @author dyoma
 */
public interface UserQueryNode extends QueryNode {
  TypedKey<Boolean> SINGLE_ENUM_PLEASE = TypedKey.create("singleEnumOrNull");
  TypedKey<Integer> MAX_NAME_LENGTH = TypedKey.create("maxNameLength");

  DataRole<UserQueryNode> USER_QUERY_NODE = DataRole.createRole(UserQueryNode.class);

  DialogEditorBuilder openQueryEditor(String title, QueryBuilderComponent qb);
}
