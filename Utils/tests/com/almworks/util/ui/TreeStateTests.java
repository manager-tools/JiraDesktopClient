package com.almworks.util.ui;

import com.almworks.util.components.ATree;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.MapMedium;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;

/**
 * @author : Dyoma
 */
public class TreeStateTests extends BaseTestCase {
  private Configuration myConfig;
  private TreeState myState;

  public TreeStateTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    TreeModelBridge<Object> root = createNode("root");
    addChild(addChild(root, "1"), "leaf");
    TreeModelBridge child = addChild(root, "2");
    addChild(child, "leaf");
    myConfig = MapMedium.createConfig();
    ATree tree = createTree(root);
    tree.expand(root);
    tree.expand(child);
  }

  public void testModelPrebuilt() {
    TreeModelBridge<Object> root = createNode("root");
    TreeModelBridge collapsed = addChild(root, "1");
    addChild(collapsed, "leaf");
    TreeModelBridge child = addChild(root, "2");
    addChild(child, "leaf");
    ATree tree = createTree(root);
    assertFalse(tree.isExpanded(collapsed));
    assertTrue(tree.isExpanded(root));
    assertTrue(tree.isExpanded(child));
  }

  public void testBuildingModel() {
    TreeModelBridge<Object> root = createNode("root");
    addChild(addChild(root, "1"), "leaf");
    ATree tree = createTree(root);
    assertTrue(tree.isExpanded(root));
    TreeModelBridge child = addChild(root, "2");
    addChild(child, "otherLeaf");
    assertTrue(tree.isExpanded(child));
  }

  public void testExpandNotEmpty() {
    TreeModelBridge<Object> root = createNode("root");
    TreeModelBridge child = addChild(root, "1");
    ATree tree = createTree(root);
    assertTrue(tree.isExpanded(root));
    myState.expand(child);
    TreeModelBridge leaf = addChild(child, "any");
    assertTrue(tree.isExpanded(child));
    leaf.removeFromParent();
    myState.updateExpansionNow();
    addChild(child, "other");
    assertTrue(tree.isExpanded(child));
  }

  private ATree createTree(TreeModelBridge<Object> root) {
    ATree tree = new ATree();
    tree.setRoot(root);
    myState = new TreeState(tree, myConfig);
    return tree;
  }

  private TreeModelBridge addChild(TreeModelBridge parent, String childId) {
    TreeModelBridge<Object> child = createNode(childId);
    parent.add(child);
    if (myState != null)
      myState.updateExpansionNow();
    return child;
  }

  private TreeModelBridge<Object> createNode(String id) {
    return new TreeModelBridge<Object>(new MyNode(id));
  }


  private static class MyNode implements IdentifiableNode {
    private final String myId;

    public MyNode(String id) {
      myId = id;
    }

    public String getNodeId() {
      return myId;
    }
  }
}
