package com.almworks.util.ui.actions.dnd;

import com.almworks.util.components.ATreeNode;
import com.almworks.util.text.parser.ParseException;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author dyoma
 */
public interface TreeStringTransferService {
  boolean canExport(Collection<ATreeNode> nodes);

  boolean canImportUnder(ATreeNode parent, int insertIndex, String string, DragContext context);

  boolean canRemove(ATreeNode node);

  @Nullable
  ATreeNode createDefaultRoot();

  String exportString(ATreeNode node);

  void moveNode(ATreeNode child, ATreeNode parent, int index);

  ATreeNode parseAndCreateNode(String string, ATreeNode parent) throws ParseException;

  boolean isParseable(String string);

  int removeNode(ATreeNode node);

  boolean shouldFlattenUnder(ATreeNode parent, ATreeNode node);

  boolean shouldReplaceOnPaste(ATreeNode oldNode, ATreeNode newNode);
}
