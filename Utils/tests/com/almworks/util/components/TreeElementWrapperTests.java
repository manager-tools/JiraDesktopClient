package com.almworks.util.components;

import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;

/**
 * @author : Dyoma
 */
public class TreeElementWrapperTests extends GUITestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();

  public void test() {
    TreeModelBridge<Object> root = createNode();
    TreeModelBridge<Object> child = createNode();
    TreeModelBridge<Object> subChild = createNode();
    child.add(subChild);
    root.add(child);
    CHECK.singleElement(child, root.children());
    CHECK.singleElement(subChild, child.children());
    child.removeFromParent();
    assertNull(child.getParent());
    CHECK.empty(root.children());
  }

  public void testWedgeInParent() {
    TreeModelBridge<Object> root = createNode();
    root.add(createNode());
    TreeModelBridge<Object> leaf = createNode();
    root.add(leaf);
    root.add(createNode());
    TreeModelBridge<Object> newParent = createNode();
    leaf.wedgeInParent(newParent);
    assertSame(newParent, root.getChildAt(1));
    CHECK.singleElement(leaf, newParent.childrenToList());
  }

  public void testPullOut() {
    TreeModelBridge<Object> root = createNode();
    TreeModelBridge<Object> child1 = createNode();
    root.add(child1);
    TreeModelBridge<Object> node = createNode();
    root.add(node);
    TreeModelBridge<Object> child3 = createNode();
    root.add(child3);
    TreeModelBridge<Object> leaf = createNode();
    node.add(leaf);
    node.pullOut();
    assertNull(node.getParent());
    CHECK.order(new TreeModelBridge[] {child1, leaf, child3}, root.childrenToList());
    child1.pullOut();
    CHECK.order(new TreeModelBridge[] {leaf, child3}, root.childrenToList());
    leaf.removeFromParent();
    child3.add(leaf);
    child3.pullOut();
    CHECK.singleElement(leaf, root.childrenToList());
  }

  public void testReplaceWith() {
    TreeModelBridge<Object> root = createNode();
    TreeModelBridge<Object> parent = createNode();
    root.add(createNode());
    root.add(parent);
    root.add(createNode());
    TreeModelBridge<Object> leaf = createNode();
    parent.add(leaf);
    TreeModelBridge<Object> newParent = createNode();
    parent.replaceWith(newParent);
    CHECK.empty(parent.childObjectsToList());
    CHECK.singleElement(leaf, newParent.childrenToList());
    assertSame(newParent, root.getChildAt(1));
    assertEquals(3, root.getChildCount());
  }

  private TreeModelBridge<Object> createNode() {
    return new TreeModelBridge(null);
  }
}
