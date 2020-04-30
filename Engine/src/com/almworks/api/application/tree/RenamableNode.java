package com.almworks.api.application.tree;

import com.almworks.util.components.EditableText;
import com.almworks.util.ui.actions.DataRole;

/**
 * @author dyoma
 */
public interface RenamableNode extends GenericNode {
  DataRole<RenamableNode> RENAMEABLE = DataRole.createRole(RenamableNode.class);

  EditableText getPresentation();

  boolean isRenamable();
}
