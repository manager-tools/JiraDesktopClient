package com.almworks.actions;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.TreeNodeFactory;
import com.almworks.util.components.ACollectionComponent;
import com.almworks.util.ui.actions.*;

import javax.swing.*;

/**
 * @author dyoma
 */
class CreateChildNodeAction extends SimpleAction {
  private final TreeNodeFactory.NodeType myNodeType;

  private CreateChildNodeAction(Icon icon, TreeNodeFactory.NodeType type) {
    super((String)null, icon);
    myNodeType = type;
    watchRole(GenericNode.NAVIGATION_NODE);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    ComponentContext<ACollectionComponent> componentContext =
      context.getComponentContext(ACollectionComponent.class, GenericNode.NAVIGATION_NODE);
    TreeNodeFactory nodeFactory = context.getSourceObject(TreeNodeFactory.TREE_NODE_FACTORY);
    GenericNode parent = getGenericNodeImpl(componentContext);
    nodeFactory.createAndEditNode(parent, myNodeType, context);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.getSourceObject(TreeNodeFactory.TREE_NODE_FACTORY);
    ComponentContext<ACollectionComponent> componentContext =
      context.getComponentContext(ACollectionComponent.class, GenericNode.NAVIGATION_NODE);
    GenericNode node = getGenericNodeImpl(componentContext);
    if (node.allowsChildren(myNodeType))
      context.setEnabled(EnableState.ENABLED);
  }

  public static CreateChildNodeAction create(TreeNodeFactory.NodeType type) {
    return new CreateChildNodeAction(null, type);
  }

  public static CreateChildNodeAction create(Icon icon, TreeNodeFactory.NodeType type) {
    return new CreateChildNodeAction(icon, type);
  }

  public static GenericNode getGenericNodeImpl(ComponentContext<?> context) throws CantPerformException {
    return context.getSourceObject(GenericNode.NAVIGATION_NODE);
  }
}
