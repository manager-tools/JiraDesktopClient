package com.almworks.util.components.tables;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;

/**
 * @author dyoma
 */
public class TreeInputHandler {
  private final MyJTree myTree = new MyJTree(new DefaultTreeModel(new DefaultMutableTreeNode(null)));
  private final MyTreeListener myTreeListener;
  private final Collection<Object> myKnownActions;

  public TreeInputHandler(Collection<Object> knownActions, Callback callback) {
    myKnownActions = knownActions;
    myTreeListener = new MyTreeListener(callback);
  }

  public void processMouseEvent(MouseEvent event, TreeNodeState state) {
    DefaultTreeModel model = getModel();
    TreePath path = state.setupPath(model, myTree);
    myTree.setCellSize(state.getCellSize());
    Rectangle bounds = myTree.getPathBounds(path);
    int dy = bounds.y - state.getY();
    dispatchMouseEvent(event, state.getX(), dy, state, path);
  }

  public void processKeyStroke(KeyStroke stroke, KeyEvent event, TreeNodeState nodeState) {
    Object name = myTree.getInputMap(JComponent.WHEN_FOCUSED).get(stroke);
    if (name == null || !myKnownActions.contains(name))
      return;
    Action action = myTree.getActionMap().get(name);
    if (action == null)
      return;
    TreePath path = nodeState.setupPath(getModel(), myTree);
    myTreeListener.listen(myTree, nodeState, path);
    SwingUtilities.notifyAction(action, stroke, event, myTree, event.getModifiers());
    myTreeListener.stopListen(myTree);
    event.consume();
  }

  private void dispatchMouseEvent(MouseEvent event, int dx, int dy, TreeNodeState state, TreePath path) {
    Object savedSource = event.getSource();
    event.setSource(myTree);
    event.translatePoint(dx, dy);
    myTreeListener.listen(myTree, state, path);
    myTree.processEvent(event);
    myTreeListener.stopListen(myTree);
    event.translatePoint(-dx, -dy);
    event.setSource(savedSource);
  }

  private DefaultTreeModel getModel() {
    return (DefaultTreeModel) myTree.getModel();
  }

  public void setRootVisible(boolean visible) {
    myTree.setRootVisible(visible);
  }

  public interface Callback {
    void treeExpanded(TreePath actualPath);

    void treeCollapsed(TreePath actualPath);

    void pathSelected(TreeSelectionEvent e, TreeNodeState targetNode, TreePath rootPath);
  }

  private static class MyJTree extends JTree {
    private final JComponent myRendererComponent = new JComponent(){};

    public MyJTree(TreeModel newModel) {
      super(newModel);
      setLargeModel(true);
      setCellRenderer(new TreeCellRenderer() {
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
          boolean leaf, int row, boolean hasFocus)
        {
          return myRendererComponent;
        }
      });
    }

    public void setCellSize(Dimension cellSize) {
      myRendererComponent.setPreferredSize(cellSize);
    }

    public void processEvent(AWTEvent e) {
      super.processEvent(e);
    }
  }


  private static class MyTreeListener implements TreeExpansionListener, TreeSelectionListener {
    private final Callback myCallback;
    @Nullable
    private TreeNodeState myTargetNode;
    private TreePath myPath;

    public MyTreeListener(@NotNull Callback callback) {
      myCallback = callback;
    }

    public void treeExpanded(TreeExpansionEvent event) {
      TreeNodeState targetNode = myTargetNode;
      assert targetNode != null;
      TreePath actualPath = targetNode.restorePath(event.getPath(), myPath);
      if (actualPath != null)
        myCallback.treeExpanded(actualPath);
    }

    public void treeCollapsed(TreeExpansionEvent event) {
      TreeNodeState targetNode = myTargetNode;
      assert targetNode != null;
      TreePath actualPath = targetNode.restorePath(event.getPath(), myPath);
      if (actualPath != null)
        myCallback.treeCollapsed(actualPath);
    }

    public void valueChanged(TreeSelectionEvent e) {
      TreeNodeState targetNode = myTargetNode;
      assert targetNode != null;
      myCallback.pathSelected(e, targetNode, myPath);
    }

    public void listen(JTree tree, TreeNodeState targetNode, TreePath path) {
      assert myTargetNode == null;
      tree.addTreeExpansionListener(this);
      tree.addTreeSelectionListener(this);
      myTargetNode = targetNode;
      myPath = path;
    }

    public void stopListen(JTree tree) {
      assert myTargetNode != null;
      tree.removeTreeExpansionListener(this);
      tree.removeTreeSelectionListener(this);
      myTargetNode = null;
    }
  }
}
