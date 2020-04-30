package com.almworks.util.components.tables;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.detach.Lifespan;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TreeToListAdapterTests extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final TreeModelBridge<String> myRoot = TreeModelBridge.create("root");
  private TreeToListAdapter<String> myAdapter;

  public TreeToListAdapterTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  public void testNotExpandingInvisibleRoot() {
    TreeModel model = myRoot.becomeRoot();
    myRoot.add(new TreeModelBridge<String>("1"));
    createAdapter(model);
    myAdapter.setRootVisible(false);
    AListModel<String> view = myAdapter.getListModel();
    assertEquals(1, view.getSize());
    myRoot.add(new TreeModelBridge<String>("2"));
    assertEquals(2, view.getSize());
    myAdapter.expand(new TreePath(myRoot));
    assertEquals(2, view.getSize());
  }

  private void createAdapter(TreeModel model) {
    myAdapter = TreeToListAdapter.create(Lifespan.FOREVER, model);
  }

  public void testNotCollapsingInvisibleRoot() {
    DefaultTreeModel model = myRoot.becomeRoot();
    myRoot.add(TreeModelBridge.create("1"));
    createAdapter(model);
    myAdapter.setRootVisible(false);
    AListModel<String> view = myAdapter.getListModel();
    assertEquals(1, view.getSize());
    myAdapter.collapse(new TreePath(myRoot));
    assertEquals(1, view.getSize());
  }

  public void testRemoveNodes() {
    myRoot.add(TreeModelBridge.create("1"));
    myRoot.add(TreeModelBridge.create("2"));
    myRoot.add(TreeModelBridge.create("3"));
    DefaultTreeModel model = myRoot.becomeRoot();
    createAdapter(model);
    checkOrder("root", "1", "2", "3");
    myRoot.getChildAt(0).removeFromParent();
    checkOrder("root", "2", "3");
  }

  public void testRemoveParent() {
    TreeModelBridge<String> parent = TreeModelBridge.create("1");
    myRoot.add(parent);
    parent.add(TreeModelBridge.create("1.1"));
    myRoot.add(TreeModelBridge.create("2"));
    DefaultTreeModel model = myRoot.becomeRoot();
    createAdapter(model);
    checkOrder("root", "1", "2");
    myAdapter.expand(parent.getPathFromRoot());
    checkOrder("root", "1", "1.1", "2");
    parent.removeFromParent();
    checkOrder("root", "2");
  }

  public void testSortingOnUpdate() {
    TreeModelBridge<String> child = TreeModelBridge.create("1");
    myRoot.add(child);
    myRoot.add(TreeModelBridge.create("3"));
    myRoot.add(TreeModelBridge.create("2"));
    DefaultTreeModel model = myRoot.becomeRoot();
    createAdapter(model);
    checkOrder("root", "1", "3", "2");
    myAdapter.sort(String.CASE_INSENSITIVE_ORDER);
    checkOrder("root", "1", "2", "3");
    child.setUserObject("4");
    checkOrder("root", "2", "3", "4");
  }

  public void testInsertionUnsorted() {
    DefaultTreeModel model = myRoot.becomeRoot();
    createAdapter(model);
    myAdapter.setRootVisible(false);
    TreeModelBridge<String> child1 = TreeModelBridge.create("1");
    myRoot.add(child1);
    child1.add(TreeModelBridge.create("1_1"));
    checkOrder("1", "1_1");
    myRoot.add(TreeModelBridge.create("2"));
    checkOrder("1", "1_1", "2");
  }

  public void testInsertUnsortedCollapsed() {
    DefaultTreeModel model = myRoot.becomeRoot();
    createAdapter(model);
    TreeModelBridge<String> parent = TreeModelBridge.create("1");
    TreeModelBridge<String> child = TreeModelBridge.create("1.1");
    parent.add(child);
    myRoot.add(parent);
    checkOrder("root", "1");
    parent.add(TreeModelBridge.create("1.2"));
    myAdapter.expand(parent.getPathFromRoot());
    checkOrder("root", "1", "1.1", "1.2");
  }

  private void checkOrder(String ... nodes) {
    CHECK.order(nodes, myAdapter.getListModel().toList());
  }
}
