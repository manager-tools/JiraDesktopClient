package com.almworks.explorer.qbuilder.editor;

import com.almworks.api.application.qb.ConstraintEditorNodeImpl;
import com.almworks.api.application.qb.EditorNode;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.explorer.qbuilder.EditorGroupNode;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.text.parser.FormulaWriter;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.text.parser.TokenRegistry;
import com.almworks.util.ui.actions.dnd.DragContext;
import com.almworks.util.ui.actions.dnd.TreeStringTransferService;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author dyoma
 */
class QBTransferService implements TreeStringTransferService {
  private final QueryEditorContext myContext;

  public QBTransferService(QueryEditorContext context) {
    myContext = context;
  }

  public ATreeNode parseAndCreateNode(String string, ATreeNode parent) throws ParseException {
    TokenRegistry<FilterNode> parser = myContext.getParser();
    FilterNode newNode = parser.tokenize(string).parseNode();
    newNode.normalizeNames(myContext.getResolver(), myContext.getParentHypercube());
    return newNode.createEditorNode(myContext).getTreeNode();
  }

  public boolean isParseable(String string) {
    try {
      TokenRegistry<FilterNode> parser = myContext.getParser();
      FilterNode newNode = parser.tokenize(string).parseNode();
      return newNode != null;
    } catch (ParseException e) {
      return false;
    }
  }

  public String exportString(ATreeNode object) {
    EditorNode userObject = (EditorNode) object.getUserObject();
    return FormulaWriter.write(userObject.createFilterNodeTree());
  }

  public boolean canExport(Collection<ATreeNode> nodes) {
    for (ATreeNode node : nodes) {
      Object userObject = node.getUserObject();
      boolean allowed;
      if (userObject instanceof ConstraintEditorNodeImpl)
        allowed = !((ConstraintEditorNodeImpl) userObject).isNoConstraint();
      else
        allowed = userObject instanceof EditorNode;
      if (!allowed)
        return false;
    }
    return true;
  }

  public boolean shouldReplaceOnPaste(ATreeNode oldObject, ATreeNode newObject) {
    Object userObject = oldObject.getUserObject();
    return userObject instanceof ConstraintEditorNodeImpl && ((ConstraintEditorNodeImpl) userObject).isNoConstraint();
  }

  public boolean canImportUnder(ATreeNode parent, int insertIndex, String string, DragContext context) {
    Object groupId = EditorGroupNode.getGroupId(parent);
    return groupId != null && groupId != EditorGroupNode.ROOT_GROUP;
  }

  public boolean canRemove(ATreeNode node) {
    ATreeNode parent = node.getParent();
    if (parent == null)
      return false;
    if (parent.getParent() != null)
      return true;
    Object userObject = node.getUserObject();
    if (!(userObject instanceof ConstraintEditorNodeImpl))
      return true;
    return !((ConstraintEditorNodeImpl) userObject).isNoConstraint();
  }

  public ATreeNode createDefaultRoot() {
    return new ConstraintEditorNodeImpl(myContext).getTreeNode();
  }

  public void moveNode(ATreeNode child, ATreeNode parent, int index) {
    child.removeFromParent();
    parent.insert(child, index);
  }

  public int removeNode(ATreeNode node) {
    return node.removeFromParent();
  }

  public boolean shouldFlattenUnder(ATreeNode parent, ATreeNode node) {
    Object groupId = getGroupType(node);
    if (groupId == null || groupId == EditorGroupNode.ROOT_GROUP)
      return false;
    int childCount = node.getChildCount();
    if (groupId == EditorGroupNode.NEITHER_GROUP)
      return childCount == 0;
    if (childCount == 1)
      return true;
    Object parentType = getGroupType(parent);
    return groupId.equals(parentType);
  }

  @Nullable
  private Object getGroupType(ATreeNode node) {
    if (node == null)
      return null;
    return EditorGroupNode.getGroupId(node);
  }
}
