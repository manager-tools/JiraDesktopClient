package com.almworks.explorer.qbuilder.editor;

import com.almworks.api.application.qb.*;
import com.almworks.explorer.qbuilder.EditorGroupNode;
import com.almworks.explorer.qbuilder.filter.BinaryCommutative;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.CollectionViewer;
import com.almworks.util.ui.DialogEditor;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.dnd.TransferAction;
import com.almworks.util.ui.actions.dnd.TreeStringTransfer;
import com.almworks.util.ui.actions.dnd.TreeStringTransferService;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * @author : Dyoma
 */
class FilterEditorImpl implements DialogEditor, FilterEditor, Modifiable {
  static final AnAction OR_ACTION = new OrOperationAction();
  static final AnAction AND_ACTION = new AndOperationAction();

  private static final NegateAction NOT_ACTION = new NegateAction();
  private static final MenuBuilder POPUP_ACTIONS =
    new MenuBuilder().addActions(AND_ACTION, OR_ACTION, NOT_ACTION).
    addSeparator().addAllActions(TransferAction.CUT_COPY_PASTE).addAction(TransferAction.REMOVE);

  private final JComponent myComponent;
  private final ATree<TreeModelBridge<EditorNode>> myTree;
  private final QueryEditorContext myContext;
  private FilterNode myAppliedFilter;

  private final AnActionDelegator myRemoveAction = new AnActionDelegator(TransferAction.REMOVE) {
    public void perform(ActionContext context) throws CantPerformException {
      super.perform(context);
      myTree.expandVisibleRoots();
    }
  };

  public FilterEditorImpl(Configuration configuration, QueryEditorContext context, FilterNode filter) {
    myAppliedFilter = filter;
    myContext = context;
    myTree = createQueryTree(new QBTransferService(context));
    PlaceHolder editorPlace = new PlaceHolder();
    JScrollPane scrollPane = new JScrollPane(myTree);
    scrollPane.setMinimumSize(new Dimension(150, 200));
    JPanel filterPanel = new JPanel(new BorderLayout(0, 0));
    filterPanel.add(scrollPane, BorderLayout.CENTER);
    filterPanel.add(UIUtil.createLabelFor(myTree.getSwingComponent(), "F&ilter:"), BorderLayout.NORTH);
    JSplitPane splitPane = UIUtil.createSplitPane(filterPanel, editorPlace, true, configuration, "splitter", 150);
    splitPane.setOneTouchExpandable(false);
    splitPane.setResizeWeight(0.2);
    JPanel treePanel = new JPanel(new BorderLayout(0, 0));
    createToolbar().addToNorth(treePanel);
    treePanel.add(splitPane, BorderLayout.CENTER);
    myComponent = treePanel;
    POPUP_ACTIONS.addToComponent(Lifespan.FOREVER, myTree.getSwingComponent());
    new CollectionViewer(
      myTree.getSelectionAccessor(), editorPlace, configuration.getOrCreateSubset("editors"),
      createStub("No constraint selected"), createStub("No additional options available"));
    myTree.setCanvasRenderer(Renderers.defaultCanvasRenderer());

    myTree.addKeyListener(new ATree.TreeKeyAdapter(myTree) {
      @Override
      protected void keyPressed(KeyEvent e, Object[] selection) {
        if(e.getKeyCode() != KeyEvent.VK_DELETE) {
          return;
        }
        final ActionBridge actionBridge = new ActionBridge(myRemoveAction, (JComponent) e.getComponent());
        try {
          actionBridge.startUpdate();
          if(!actionBridge.isEnabled()) {
            return;
          }
        } finally {
          actionBridge.stopUpdate();
        }
        actionBridge.performIfEnabled();
      }
    });
  }

  private static UIComponentWrapper createStub(String text) {
    final JLabel label = new JLabel(text, SwingConstants.CENTER);
    label.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

    final JComponent panel = new JPanel(new BorderLayout());
    panel.add(new JLabel(" "), BorderLayout.NORTH);
    panel.add(label, BorderLayout.CENTER);

    return new UIComponentWrapper.Simple(panel);
  }

  public QueryEditorContext getContext() {
    return myContext;
  }

  static ATree<TreeModelBridge<EditorNode>> createQueryTree(TreeStringTransferService transfer) {
    ATree<TreeModelBridge<EditorNode>> aTree = new ATree<TreeModelBridge<EditorNode>>();
    aTree.setDataRoles(EditorNode.EDITOR_NODE);
    aTree.setRootVisible(false);
    aTree.setTransfer(new TreeStringTransfer(transfer));
    return aTree;
  }

  private AToolbar createToolbar() {
    ToolbarBuilder builder = ToolbarBuilder.buttonsWithText();
    builder.addCommonPresentation(Action.MNEMONIC_KEY, PresentationMapping.GET_MNEMONIC);
    builder.setContextComponent(myTree);
    builder.addActions(AND_ACTION, OR_ACTION, NOT_ACTION);
    builder.addAction(myRemoveAction);
    return builder.createHorizontalToolbar();
  }

  public boolean isModified() {
    return getRoot().isModified();
  }

  public void apply() throws CantPerformExceptionExplained {
    myAppliedFilter = getUpToDateFilter();
    SelectionAccessor<TreeModelBridge<EditorNode>> selectionAccessor = myTree.getSelectionAccessor();
    int[] savedSelection = selectionAccessor.getSelectedIndexes();
    reset();
    if (savedSelection.length > 0)
      selectionAccessor.setSelectedIndexes(savedSelection);
  }

  public FilterNode getCurrentFilter() {
    return myAppliedFilter;
  }

  public FilterNode getUpToDateFilter() {
    return getRoot().createFilterNodeTree();
  }

  @Override
  public Modifiable getModifiable() {
    return this;
  }

  @NotNull
  private EditorNode getRoot() {
    EditorNode root = getTreeRoot().getUserObject();
    assert root != null : getTreeRoot();
    return root;
  }

  private TreeModelBridge<EditorNode> getTreeRoot() {
    return (TreeModelBridge<EditorNode>) myTree.getRoot();
  }

  public void reset() {
    resetQueryTree(myTree, myAppliedFilter.createEditorNode(myContext));
  }

  static TreeModelBridge<EditorNode> resetQueryTree(ATree<TreeModelBridge<EditorNode>> tree, EditorNode editorNode) {
    TreeModelBridge<EditorNode> root = QueryTree.createRoot(editorNode);
    tree.setRoot(root);
    TreeModelBridge<EditorNode> queryNode = root.getChildAt(0);
    tree.getSelectionAccessor().setSelected(queryNode);
//    tree.expand(queryNode, 1500);
    tree.expandAll();
    return root;
  }

  public void addChangeListener(Lifespan life, ThreadGate gate, final ChangeListener listener) {
    final DefaultTreeModel treeModel = myTree.getModel();
    final TreeModelListener treeModelListener = new TreeModelAdapter() {
      public void treeModelEvent(TreeModelEvent e) {
        listener.onChange();
      }
    };
    treeModel.addTreeModelListener(treeModelListener);
    life.add(new Detach() {
      protected void doDetach() {
        treeModel.removeTreeModelListener(treeModelListener);
      }
    });
  }

  public void addChangeListener(Lifespan life, ChangeListener listener) {
    addChangeListener(life, ThreadGate.STRAIGHT, listener);
  }

  public Detach addAWTChangeListener(ChangeListener listener) {
    DetachComposite life = new DetachComposite();
    addChangeListener(life, ThreadGate.AWT, listener);
    return life;
  }

  public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    addChangeListener(life, ThreadGate.AWT, listener);
  }

  public JComponent getComponent() {
    return myComponent;
  }

  public void dispose() {
    myContext.dispose();
    getRoot().dispose();
  }

  private static void checkSingleSelectedNotRoot(UpdateContext context, boolean allowNC) throws CantPerformException {
    context.getSourceObject(EditorContext.DATA_ROLE);
    ComponentContext<ACollectionComponent> componentContext =
      context.getComponentContext(ACollectionComponent.class, EditorNode.EDITOR_NODE);
    EditorNode node = componentContext.getSourceObject(EditorNode.EDITOR_NODE);
    if ((!allowNC && node.isNoConstraint()) || node.getTreeNode().getParent() == null)
      context.setEnabled(EnableState.DISABLED);
    else
      context.setEnabled(EnableState.ENABLED);
  }

  private static abstract class OperationAction extends SimpleAction {
    public OperationAction(String name, Icon icon) {
      super(name, icon);
      watchRole(EditorNode.EDITOR_NODE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      checkSingleSelectedNotRoot(context, false);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      EditorContext editorContext = context.getSourceObject(EditorContext.DATA_ROLE);
      TreeModelBridge<EditorNode> newNode = new ConstraintEditorNodeImpl(editorContext).getTreeNode();
      ComponentContext<ACollectionComponent> cc =
        context.getComponentContext(ACollectionComponent.class, EditorNode.EDITOR_NODE);
      EditorNode node = context.getSourceObject(EditorNode.EDITOR_NODE);
      TreeModelBridge<EditorNode> parent = chooseParent(editorContext, node);
      parent.add(newNode);
      cc.getComponent().getSelectionAccessor().setSelected(newNode);
    }

    @NotNull
    private TreeModelBridge<EditorNode> chooseParent(EditorContext context, EditorNode node) {
      TreeModelBridge<EditorNode> treeNode = node.getTreeNode();
      Object groupId = EditorGroupNode.getGroupId(node);
      if (isMyGroup(groupId))
        return treeNode;
      TreeModelBridge<EditorNode> parent = treeNode.getParent();
      assert parent != null : treeNode;
      if (!canInsertInto(EditorGroupNode.getGroupId(parent))) {
        TreeModelBridge<EditorNode> operationNode = createEditorNode(context);
        treeNode.wedgeInParent(operationNode);
        parent = operationNode;
      }
      return parent;
    }

    @NotNull
    protected abstract TreeModelBridge<EditorNode> createEditorNode(EditorContext context);

    protected abstract boolean canInsertInto(Object groupId);

    protected abstract boolean isMyGroup(Object groupId);
  }

  private static class OrOperationAction extends OperationAction{
    public OrOperationAction() {
      super("&OR", Icons.QUERY_OR_ACTION);
    }

    public void customUpdate(UpdateContext context) throws CantPerformException {
      super.customUpdate(context);
      if (isNor(context)) {
        context.putPresentationProperty(PresentationKey.NAME, "N&OR");
        context.putPresentationProperty(PresentationKey.SMALL_ICON, Icons.QUERY_NOR_ACTION);
      }
    }

    private boolean isNor(ActionContext context) throws CantPerformException {
      TreeModelBridge<EditorNode> selectedNode = (TreeModelBridge<EditorNode>) context
        .getComponentContext(ACollectionComponent.class, EditorNode.EDITOR_NODE)
        .getComponent()
        .getSelectionAccessor()
        .getSelection();
      assert selectedNode != null;
      return EditorGroupNode.getGroupId(selectedNode.getParent()) == EditorGroupNode.NEITHER_GROUP;
    }

    @NotNull
    protected TreeModelBridge<EditorNode> createEditorNode(EditorContext context) {
      return createOrTreeNode(context);
    }

    @NotNull
    private TreeModelBridge<EditorNode> createOrTreeNode(EditorContext context) {
      return BinaryCommutative.OR_SERIALIZER.create(Collections15.<FilterNode>emptyList()).createEditorNode(context).getTreeNode();
    }

    protected boolean canInsertInto(Object groupId) {
      return isMyGroup(groupId) || groupId == EditorGroupNode.NEITHER_GROUP;
    }

    protected boolean isMyGroup(Object groupId) {
      return groupId == EditorGroupNode.OR_GROUP;
    }
  }

  static class NegateAction extends SimpleAction {
    public NegateAction() {
      super("&NOT", Icons.QUERY_NOT_ACTION);
      watchRole(EditorNode.EDITOR_NODE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      checkSingleSelectedNotRoot(context, true);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      ComponentContext<ACollectionComponent> cc =
        context.getComponentContext(ACollectionComponent.class, EditorNode.EDITOR_NODE);
      SelectionAccessor<TreeModelBridge<EditorNode>> selection = cc.getComponent().getSelectionAccessor();
      assert selection.getSelectedCount() == 1 : selection.getSelectedCount();
      TreeModelBridge<EditorNode> selectedNode = selection.getSelection();
      assert selectedNode != null;
      EditorNode node = selectedNode.getUserObject();
      assert node != null : selection.getSelection();
      Object selectedGroupId = EditorGroupNode.getGroupId(node);
      TreeModelBridge<EditorNode> treeNode = node.getTreeNode();
      TreeModelBridge<EditorNode> toSelect;
      EditorContext editorContext = context.getSourceObject(EditorContext.DATA_ROLE);
      if (selectedGroupId == null || selectedGroupId == EditorGroupNode.AND_GROUP) {
        TreeModelBridge<EditorNode> parent = treeNode.getParent();
        assert parent != null : treeNode;
        toSelect = parent.getChildCount() == 1 && EditorGroupNode.getGroupId(parent) == EditorGroupNode.NEITHER_GROUP ?
          removeNot(parent) : wedgeInNot(treeNode, editorContext);
      } else if (selectedGroupId == EditorGroupNode.NEITHER_GROUP) {
        int childCount = treeNode.getChildCount();
        if (childCount == 0) {
          assert false;
          toSelect = treeNode;
        } else if (childCount == 1)
          toSelect = removeNot(treeNode);
        else {
          toSelect = EditorGroupNode.createOr(editorContext, treeNode).getTreeNode();
        }
      } else if (selectedGroupId == EditorGroupNode.OR_GROUP) {
        toSelect = createNotNode(editorContext, treeNode);
      } else {
        assert false : "Group:" + selectedGroupId + " Node:" + node;
        return;
      }
      selection.setSelected(toSelect);
    }

    private TreeModelBridge<EditorNode> removeNot(TreeModelBridge<EditorNode> notNode) {
      TreeModelBridge<EditorNode> child = notNode.getChildAt(0);
      notNode.pullOut();
      return child;
    }

    private TreeModelBridge<EditorNode> wedgeInNot(TreeModelBridge<EditorNode> toNegate, EditorContext context) {
      toNegate.wedgeInParent(createNotNode(context, null));
      return toNegate;
    }

    private TreeModelBridge<EditorNode> createNotNode(EditorContext context, @Nullable TreeModelBridge<EditorNode> treeNode) {
      return EditorGroupNode.createNegation(context, treeNode).getTreeNode();
    }
  }


  private static class AndOperationAction extends OperationAction {
    public AndOperationAction() {
      super("&AND", Icons.QUERY_AND_ACTION);
    }

    @NotNull
    protected TreeModelBridge<EditorNode> createEditorNode(EditorContext context) {
      return BinaryCommutative.AND_SERIALIZER.create(Collections15.<FilterNode>emptyList()).createEditorNode(context).getTreeNode();
    }

    protected boolean canInsertInto(Object groupId) {
      return isMyGroup(groupId);
    }

    protected boolean isMyGroup(Object groupId) {
      return groupId == EditorGroupNode.AND_GROUP;
    }
  }
}
