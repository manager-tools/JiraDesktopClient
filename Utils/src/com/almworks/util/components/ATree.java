package com.almworks.util.components;

import com.almworks.util.Getter;
import com.almworks.util.components.renderer.CanvasImpl;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.DefaultCanvasComponent;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.dnd.*;
import com.almworks.util.ui.actions.globals.GlobalData;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

/**
 * @author : Dyoma
 */
public class ATree<T extends ATreeNode> extends ScrollableWrapper<JTree>
  implements ACollectionComponent<T>, HighlighterTree, DndTarget, DropHintProvider<TreeDropHint, JTreeAdapter>
{

  private final TreeSelectionAccessor<T> mySelectionAccessor = new TreeSelectionAccessor<T>(getScrollable());
  private static final List<DataRole<?>> TREE_DATA_ROLES =
    Arrays.asList(new DataRole<?>[] {ATreeNode.ATREE_NODE, ATreeNode.TREE_NODE});

  private final Lifecycle mySwingLife = new Lifecycle(false);
  private final DndHelper<TreeDropHint, JTreeAdapter> myDndHelper;

  // temp variable to hold first explanation why drop is not possible
  private transient String myCantDropExplanation;

  public ATree() {
    super(new JTreeAdapter(new TreeModelBridge<Object>(null).becomeRoot()));
    myDndHelper = new DndHelper<TreeDropHint, JTreeAdapter>(this);
    getScrollable().setToolTipText("");
    SelectionDataProvider.installTo(this);
    SelectionDataProvider.setRoles(this, TREE_DATA_ROLES);
    JTree tree = (JTree) getSwingComponent();
    tree.addMouseListener(new ATreeMouseAdapter());
    ATreeExpandListener expandListener = new ATreeExpandListener();
    tree.addTreeWillExpandListener(expandListener);
    tree.addTreeExpansionListener(expandListener);
    DoubleClickAuthority.PROPERTY.putClientValue(tree, DoubleClickAuthority.Tree.INSTANCE);
  }

  public static <T extends ATreeNode<?>> ATree<T> create() {
    return new ATree<T>();
  }

  public void addHighlightedElement(final HighlighterTreeElement elem) {
    Getter<TreeNode> getter = new Getter<TreeNode>() {
      public TreeNode get() {
        Getter<TreeNode> delegate = elem.getRootGetter();
        if (delegate != null) {
          TreeNode node = delegate.get();
          node = normalize((ATreeNode) node, true);
          return node;
        }
        return null;
      }
    };
    ((JTreeAdapter) getScrollable()).addHighlightedElement(new HighlighterTreeElement(getter, elem.getColor(),
      elem.getFillAlpha(), elem.getIcon(), elem.getCaption(), elem.getCaptionBackground(), elem.getCaptionForeground(),
      elem.getCaptionFont()));
  }

  public void removeAllHighlighted() {
    ((JTreeAdapter) getScrollable()).removeAllHighlighted();
  }

  public void invalidateNode(ATreeNode<?> node) {
    node = normalize(node);
    if (node == null)
      return;
    //noinspection deprecation
    getModel().nodeChanged(node);
  }

  public void setCanvasRenderer(CanvasRenderer<? super T> renderer) {
    getScrollable().setCellRenderer(new CanvasRendererAdapter<T>(renderer));
  }

  public void setRootVisible(boolean visible) {
    getScrollable().setRootVisible(visible);
  }

  public boolean isRootVisible() {
    return getScrollable().isRootVisible();
  }

  @NotNull
  public SelectionAccessor<T> getSelectionAccessor() {
    return mySelectionAccessor;
  }

  public JComponent toComponent() {
    return this;
  }

  public void scrollSelectionToView() {
    int index = getSelectionAccessor().getSelectedIndex();
    if (index == -1)
      return;
    UIUtil.ensureRectVisiblePartially(this, getScrollable().getRowBounds(index));
  }

  public void setDataRoles(DataRole... roles) {
    List<DataRole<?>> allRoles = Collections15.arrayList(roles.length + TREE_DATA_ROLES.size());
    allRoles.addAll(Arrays.<DataRole<?>>asList(roles));
    allRoles.addAll(TREE_DATA_ROLES);
    SelectionDataProvider.setRoles(this, allRoles);
    GlobalData.KEY.removeAll(toComponent());
  }

  public void addGlobalRoles(DataRole<?>... roles) {
    SelectionDataProvider.addRoles(this, roles);
    GlobalData.KEY.addClientValue(this.toComponent(), roles);
  }

  public Rectangle getElementRect(int elementIndex) {
    return getScrollable().getRowBounds(elementIndex);
  }

  /**
   * @deprecated replaced with {@link #getRoot()}
   */
  public DefaultTreeModel getModel() {
    return (DefaultTreeModel) getScrollable().getModel();
  }

  public ATreeNode getRoot() {
    return (ATreeNode<?>) getScrollable().getModel().getRoot();
  }

  public void setEditable(boolean editable) {
    getScrollable().setEditable(editable);
  }

  public void startEditing(TreeNode node) {
    getScrollable().startEditingAtPath(UIUtil.getPathToRoot(node));
  }

  public void setRoot(ATreeNode<?> root) {
    root.replaceRoot((DefaultTreeModel) getScrollable().getModel());
    mySelectionAccessor.setNodeManager(root.getNodeManager());
  }

  public void expand(ATreeNode<?> node) {
    expand(node, 0);
  }

  public void expand(ATreeNode<?> node, long timeout) {
    ATreeNode<?> normalized = normalize(node, true);
    if (normalized != null)
      doExpand(normalized);
  }

  private void doExpand(@NotNull ATreeNode<?> normalized) {
    getScrollable().expandPath(normalized.getPathFromRoot());
  }

  @Nullable
  private ATreeNode normalize(ATreeNode<?> node) {
    return normalize(node, false);
  }

  @Nullable
  private ATreeNode normalize(ATreeNode node, boolean strict) {
    if (node == null)
      return null;
    ATreeNodeManager nodeManager = mySelectionAccessor.getNodeManager();
    ATreeNode result = nodeManager == null ? node : nodeManager.findCorresponding(node);
    if (result == null)
      return null;
    if (strict) {
      return result.getNodeManager() == nodeManager ? result : null;
    } else {
      return result;
    }
  }

  public void expandVisibleRoots() {
    ATreeNode<?> root = getRoot();
    if (root == null)
      return;
    if (isVisibleRoot(root)) {
      expand(root);
    } else {
      ATreeNode[] children = root.childrenToArray();
      for (ATreeNode<?> child : children) {
        if (isVisibleRoot(child))
          expand(child);
      }
    }
  }

  @Deprecated
  public Detach addExpansionListener(final TreeExpansionListener listener) {
    getScrollable().addTreeExpansionListener(listener);
    return new Detach() {
      protected void doDetach() {
        getScrollable().removeTreeExpansionListener(listener);
      }
    };
  }

  public ATree<T> addExpansionListener(Lifespan life, TreeExpansionListener listener) {
    if (!life.isEnded()) {
      life.add(addExpansionListener(listener));
    }
    return this;
  }

  public boolean isExpanded(ATreeNode<?> node) {
    node = normalize(node);
    if (node == null)
      return false;
    return getScrollable().isExpanded(node.getPathFromRoot());
  }

  public boolean isVisibleRoot(ATreeNode<?> node) {
    node = normalize(node);
    if (node == null)
      return false;
    ATreeNode<?> parent = node.getParent();
    if (parent == null)
      return isRootVisible();
    return !isRootVisible() && parent.getParent() == null;
  }

  public void setTransfer(ContextTransfer transfer) {
    super.setTransfer(transfer);
    if (!GraphicsEnvironment.isHeadless())
      getScrollable().setDragEnabled(true);
    if (isDisplayable()) {
      registerInDndManager();
    }
  }

  private void registerInDndManager() {
    DndManager dndManager = DndManager.require();
    Lifespan lifespan = mySwingLife.lifespan();
    dndManager.registerSource(lifespan, this);
    dndManager.registerTarget(lifespan, this);
  }

  public void addNotify() {
    super.addNotify();
    if (mySwingLife.cycleStart()) {
      if (getTransfer() != null) {
        registerInDndManager();
      }
    }
  }

  public void removeNotify() {
    mySwingLife.cycleEnd();
    super.removeNotify();
  }

  public void setDoubleClickExpandEnabled(boolean b) {
    getScrollable().setToggleClickCount(b ? 2 : 0);
  }

  public void setShowsRootHandles(boolean b) {
    getScrollable().setShowsRootHandles(b);
  }

  public void addKeyListener(TreeKeyAdapter adapter) {
    getScrollable().addKeyListener(adapter);
  }

  public void expandAll() {
    expandAll(getRoot());
  }

  public void expandAll(ATreeNode root) {
    expand(root);
    for (int i = root.getChildCount() - 1; i >= 0; i--)
      expandAll(root.getChildAt(i));
  }

  public JTree getScrollable() {
    return super.getScrollable();
  }

  public ATree<T> addModelListener(Lifespan life, final TreeModelListener listener) {
    if (life.isEnded())
      return this;
    final DefaultTreeModel model = getModel();
    model.addTreeModelListener(listener);
    life.add(new Detach() {
      protected void doDetach() {
        model.removeTreeModelListener(listener);
      }
    });
    return this;
  }

  public void dragNotify(DndEvent event) {
    myDndHelper.dragNotify(event, getTransfer(), (JTreeAdapter) getSwingComponent());
  }

  public TreeDropHint createDropHint(JTreeAdapter component, DragContext context) {
    Integer row = context.getValue(DndUtil.DROP_HINT_ROW);
    TreeDropPoint dropPoint = context.getValue(DndUtil.TREE_DROP_POINT);
    if (row == null || dropPoint == null)
      return null;
    Dimension size = context.getValue(DndUtil.DRAG_SOURCE_SIZE);
    return new TreeDropHint(row, dropPoint, size);
  }

  public boolean prepareDropHint(JTreeAdapter tree, Point p, DragContext context, ContextTransfer transfer) {
    int hintRow = getHintRow(tree, p);
    boolean dropPlaceFound = false;
    Integer lastRow = context.getValue(DndUtil.DROP_HINT_ROW);
    TreeDropPoint lastDropPoint = context.getValue(DndUtil.TREE_DROP_POINT);
    if (hintRow != 0) {
      boolean preferConsumption = hintRow < 0;
      int insertRow = preferConsumption ? (-hintRow - 1) + 1 : hintRow;
      TreePath pathAbove = tree.getPathForRow(insertRow - 1);
      if (pathAbove == null) {
        assert false : tree + " " + p;
        return true;
      }
      Point offset = context.getValue(DndUtil.DRAG_SOURCE_OFFSET);
      int x = offset == null ? p.x : p.x - offset.x;
      x -= DndUtil.DND_SUBNODE_POINT_OFFSET;
      myCantDropExplanation = null;
      dropPlaceFound = findDropPlace(pathAbove, 0, tree, x, context, transfer, true, preferConsumption);
      if (dropPlaceFound) {
        TreeDropPoint dropPoint = context.getValue(DndUtil.TREE_DROP_POINT);
        if (dropPoint == null) {
          assert false : context + " " + tree;
        } else {
          context.putValue(DndUtil.DROP_HINT_ROW, dropPoint.isInsertNode() ? insertRow : insertRow - 1);
        }
      }
    }
    if (!dropPlaceFound) {
      cleanContext(context);
      if (myCantDropExplanation != null) {
        StringDragImageFactory.ensureContext(context, DndUtil.ACTION_IMAGE_FACTORY, myCantDropExplanation, tree,
          GlobalColors.DND_DESCRIPTION_DARK_FG_BG);
      }
    }
    myCantDropExplanation = null;
    context.putValue(DndUtil.DROP_ENABLED, dropPlaceFound);
    return !Util.equals(lastRow, context.getValue(DndUtil.DROP_HINT_ROW)) ||
      !Util.equals(lastDropPoint, context.getValue(DndUtil.TREE_DROP_POINT));
  }

  private boolean findDropPlace(TreePath path, int insertIndex, JTreeAdapter tree, int x, DragContext context,
    ContextTransfer transfer, boolean consumptionPossible, boolean consumptionPreferred)
  {
    if (path == null)
      return false;
    Object node = path.getLastPathComponent();
    if (node == null)
      return false;
    assert
      insertIndex >= 0 && insertIndex <= tree.getModel().getChildCount(node) :
      insertIndex + " " + tree.getModel().getChildCount(node);

    Rectangle bounds = tree.getPathBounds(path);
    if (bounds == null)
      return false;
    boolean underNode = x >= bounds.x;

    boolean dropPlaceFound = false;

    // 1. If we position transferable to be consumed, try it first
    if (consumptionPossible && consumptionPreferred) {
      dropPlaceFound = tryDropPoint(false, node, -1, context, transfer);
    }

    // 2. If mouse points "under" this node, check if drop can be made there
    if (!dropPlaceFound) {
      if (underNode) {
        dropPlaceFound = tryDropPoint(true, node, insertIndex, context, transfer);
      }
    }

    // 3. Check if parent may accept the drop
    if (!dropPlaceFound) {
      TreeModel model = tree.getModel();
      // if there are sub-items shown below the hint row, we can't show a hint
      boolean mayInsertToParent = tree.isCollapsed(path) || insertIndex == model.getChildCount(node);
      if (mayInsertToParent) {
        TreePath parentPath = path.getParentPath();
        if (parentPath != null) {
          Object parent = parentPath.getLastPathComponent();
          if (parent != null) {
            int parentInsertIndex = model.getIndexOfChild(parent, node) + 1;
            dropPlaceFound = findDropPlace(parentPath, parentInsertIndex, tree, x, context, transfer, false, false);
          }
        }
      }
    }

    // 4. If parent does not accept the drop and (1) has not been checked, try it anyway
    if (!dropPlaceFound) {
      if (!underNode) {
        dropPlaceFound = tryDropPoint(true, node, insertIndex, context, transfer);
      }
    }

    // 5. Lastly, if node cannot be inserted but consumption is possible (not preferred), check it
    if (!dropPlaceFound) {
      if (consumptionPossible && !consumptionPreferred) {
        dropPlaceFound = tryDropPoint(false, node, -1, context, transfer);
      }
    }

    return dropPlaceFound;
  }


  private boolean tryDropPoint(boolean insert, Object node, int insertIndex, DragContext context,
    ContextTransfer transfer)
  {
    Threads.assertAWTThread();
    assert node != null : this;
    TreeDropPoint trial = insert ? TreeDropPoint.insert(node, insertIndex) : TreeDropPoint.consume(node);
    TreeDropPoint oldDropPoint = context.putValue(DndUtil.TREE_DROP_POINT, trial);
    boolean canImport = false;
    try {
      canImport = transfer.canImportDataNow(context, getSwingComponent());
    } catch (CantPerformExceptionExplained e) {
      if (myCantDropExplanation == null)
        myCantDropExplanation = e.getMessage();
      canImport = false;
    } catch (CantPerformException e) {
      canImport = false;
    } finally {
      if (!canImport) {
        context.putValue(DndUtil.TREE_DROP_POINT, oldDropPoint);
      }
    }
    return canImport;
  }

  public void cleanContext(DragContext context) {
    if (context != null) {
      context.putValue(DndUtil.TREE_DROP_POINT, null);
      context.putValue(DndUtil.DROP_HINT_ROW, null);
      context.putValue(DndUtil.ACTION_IMAGE_FACTORY, null);
    }
  }

  @Nullable
  public static TreeDropPoint getDropPoint(ActionContext context) {
    try {
      TreeDropPoint dropPoint = context.getSourceObject(DndUtil.TREE_DROP_POINT);
      if (dropPoint != null && dropPoint.getNode() instanceof ATreeNode) {
        return dropPoint;
      }
    } catch (CantPerformException e) {
      // fall through
    }
    if ((context instanceof DragContext) && (((DragContext) context).isKeyboardTransfer())) {
      // copy-paste
      Component c = context.getComponent();
      if (c instanceof JTree) {
        Container container = c.getParent();
        if (container instanceof ATree) {
          ATree tree = ((ATree) container);
          SelectionAccessor<ATreeNode> accessor = tree.getSelectionAccessor();
          if (accessor.getSelectedCount() == 1) {
            ATreeNode node = accessor.getSelection();
            if (node != null) {
              ContextTransfer transfer = tree.getTransfer();
              if (transfer != null) {
                // try consuming
                boolean found = tree.tryDropPoint(false, node, -1, (DragContext) context, transfer);
                if (!found) {
                  // try inserting
                  found = tree.tryDropPoint(true, node, node.getChildCount(), (DragContext) context, transfer);
                }
                if (found) {
                  TreeDropPoint dropPoint = ((DragContext) context).getValue(DndUtil.TREE_DROP_POINT);
                  assert dropPoint != null;
                  return dropPoint;
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns table row where insertion or drop should happen.
   *
   * @param tree tree component
   * @param p    mouse point
   * @return 0 if no place for drop is available
   *         >0 row is found, insertion is preferred; insertion=row
   *         <0 row is found, consumption is preferred; consumption=-row-1
   */
  private int getHintRow(JTreeAdapter tree, Point p) {
    Rectangle rect = tree.getInsertTargetHintRect();
    int hintRow = 0;
    if (rect != null && rect.contains(p)) {
      hintRow = tree.getInsertTargetHintRow();
      assert hintRow > 0;
    } else {
      int row = tree.getClosestRowForLocation(p.x, p.y);
      if (row >= 0) {
        Rectangle bounds = tree.getRowBounds(row);
        if (p.y < bounds.y) {
          // insert before
          hintRow = row;
        } else if (p.y < bounds.y + bounds.height * 0.5F) {
          // consume in
          hintRow = -row - 1;
        } else {
          // insert after
          hintRow = row + 1;
        }
      }
    }
    return hintRow;
  }

  protected boolean acceptDrag(DragContext context) {
    ContextTransfer transfer = getTransfer();
    if (transfer == null)
      return false;
    Transferable transferable = context.getTransferable();
    if (transferable == null)
      return false;
    try {
      if (!transfer.canImportData(context))
        return false;
    } catch (CantPerformException e) {
      return false;
    }
    DataFlavor[] flavors = transferable.getTransferDataFlavors();
    for (DataFlavor flavor : flavors) {
      boolean can = transfer.canImportFlavor(flavor);
      if (can)
        return true;
    }
    return false;
  }

  public JComponent getTargetComponent() {
    return getSwingComponent();
  }

  /**
   * Override {@link javax.swing.Scrollable#getPreferredScrollableViewportSize()} with {@link javax.swing.JComponent#getPreferredSize()}
   * @param b
   */
  public void setScrollSizeIsPref(boolean b) {
    ((JTreeAdapter) getScrollable()).setScrollSizeIsPref(b);
  }


  public static class TreeKeyAdapter implements KeyListener {
    private JTree myTree;

    public TreeKeyAdapter(ATree<?> tree) {
      myTree = tree.getScrollable();
    }

    private Object[] getSelection() {
      final TreePath[] selectionPaths = myTree.getSelectionPaths();
      if (selectionPaths == null || selectionPaths.length == 0)
        return Const.EMPTY_OBJECTS;

      Object[] selectedObjects = new Object[selectionPaths.length];
      for (int i = 0; i < selectionPaths.length; i++)
        selectedObjects[i] = selectionPaths[i].getLastPathComponent();
      return selectedObjects;
    }

    public final void keyPressed(KeyEvent e) {
      keyPressed(e, getSelection());
    }

    protected void keyPressed(KeyEvent e, Object[] selection) {
    }

    public final void keyReleased(KeyEvent e) {
      keyReleased(e, getSelection());
    }

    protected void keyReleased(KeyEvent e, Object[] selection) {
    }

    public final void keyTyped(KeyEvent e) {
      keyTyped(e, getSelection());
    }

    protected void keyTyped(KeyEvent e, Object[] selection) {
    }
  }


  private class ATreeMouseAdapter extends MouseAdapter {
    public void mouseClicked(MouseEvent e) {
      processMouseEvent(e);
    }

    public void mousePressed(MouseEvent e) {
      processMouseEvent(e);
    }

    public void mouseReleased(MouseEvent e) {
      processMouseEvent(e);
    }

    private void processMouseEvent(MouseEvent e) {
      if (!e.isPopupTrigger())
        return;
      UIUtil.adjustSelection(getScrollable().getRowForLocation(e.getX(), e.getY()), getSelectionAccessor());
    }
  }


  private static class ATreeExpandListener implements TreeWillExpandListener, TreeExpansionListener {
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
      getSink(event).treeWillExpand(event);
    }

    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
      getSink(event).treeWillCollapse(event);
    }

    public void treeExpanded(TreeExpansionEvent event) {
      getSink(event).treeExpanded(event);
    }

    public void treeCollapsed(TreeExpansionEvent event) {
      getSink(event).treeCollapsed(event);
    }

    private TreeExpansionAdapter getSink(TreeExpansionEvent event) {
      TreeExpansionAdapter result = TreeExpansionAdapter.STUB;
      TreePath path = event.getPath();
      if (path != null) {
        Object node = path.getLastPathComponent();
        if (node instanceof ATreeNode) {
          Object userObject = ((ATreeNode) node).getUserObject();
          if (userObject instanceof TreeExpansionAware) {
            TreeExpansionAdapter sink = ((TreeExpansionAware) userObject).getExpansionEventSink();
            if (sink != null) {
              result = sink;
            }
          }
        }
      }
      return result;
    }
  }

  private static class CanvasRendererAdapter<T> implements TreeCellRenderer {
    private final DefaultCanvasComponent myCanvas = new DefaultCanvasComponent();
    private final CanvasRenderer<? super T> myRenderer;

    public CanvasRendererAdapter(CanvasRenderer<? super T> renderer) {
      myRenderer = renderer;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
      boolean leaf, int row, boolean hasFocus)
    {
      T item = (T) value;
      hasFocus = ListSpeedSearch.fixFocusedState(tree, hasFocus, row, 0);
      CellState state = new TreeCellState(tree, selected, hasFocus, expanded, leaf, row);
      CanvasImpl canvas = myCanvas.prepareCanvas(state);
      canvas.setCanvasBackground(null);
      canvas.setCanvasBorder(null);
      canvas.getCurrentSection().setBorder(state.getBorder());
      myRenderer.renderStateOn(state, canvas, item);
      return myCanvas;
    }
  }
}
