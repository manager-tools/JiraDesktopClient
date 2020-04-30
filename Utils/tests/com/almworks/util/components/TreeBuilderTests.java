package com.almworks.util.components;

import com.almworks.util.collections.Containers;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.Collections15;

import javax.swing.tree.DefaultTreeModel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author dyoma
 */
public class TreeBuilderTests extends GUITestCase {
  private final TreeModelBridge<TreeElement> myRoot = new TreeModelBridge<TreeElement>(new TreeElement(null, null));
  private final TreeStructure<TreeElement,String, TreeModelBridge<TreeElement>> myTreeStructure = new TreeStructure<TreeElement, String, TreeModelBridge<TreeElement>>() {
    public String getNodeKey(TreeElement element) {
      return element.getId();
    }

    public String getNodeParentKey(TreeElement element) {
      return element.getParentId();
    }

    public TreeModelBridge<TreeElement> createTreeNode(TreeElement element) {
      return new TreeModelBridge<TreeElement>(element);
    }
  };
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final TreeBuilder<TreeElement, ?> myBuilder = new TreeBuilder(myTreeStructure, new Comparator<TreeElement>() {
    public int compare(TreeElement o1, TreeElement o2) {
      return Containers.compareInts(o1.getOrder(), o2.getOrder());
    }
  }, myRoot);
  private DefaultTreeModel myModel;

  protected void setUp() throws Exception {
    super.setUp();
    myModel = myRoot.becomeRoot();
  }

  public void testAddRemoveElement() {
    myBuilder.addElement(new TreeElement("a", null));
    CHECK.singleElement("a", getRootChildren());
    myBuilder.addElement(new TreeElement("c", "b"));
    CHECK.order(new String[]{"a", "c"}, getRootChildren());
    myBuilder.addElement(new TreeElement("b", "a"));
    CHECK.singleElement("a", getRootChildren());
    CHECK.singleElement("b", getChildrenOf("a"));
    CHECK.singleElement("c", getChildrenOf("b"));
    CHECK.empty(getChildrenOf("c"));

    myBuilder.removeElement(findTreeElement("b"));
    CHECK.order(new String[]{"a", "c"}, getRootChildren());
    CHECK.empty(getChildrenOf("a"));
    CHECK.empty(getChildrenOf("c"));
    assertNull(findNode("b"));

    myBuilder.clear();
    assertEquals(0, myRoot.getChildCount());
  }

  public void testAddingElementWithSameId() {
    myBuilder.addElement(new TreeElement("a", null));
    CHECK.singleElement("a", getRootChildren());
    myBuilder.addElement(new TreeElement("a", "b"));
    CHECK.singleElement("a", getRootChildren());
    assertEquals(1, myRoot.getChildCount());
    myBuilder.addElement(new TreeElement("b", null));
    CHECK.order(new String[]{"a", "b"}, getRootChildren());
  }

  public void testRemoveTwice() {
    TreeElement element = new TreeElement("a", null);
    myBuilder.addElement(element);
    CHECK.singleElement("a", getRootChildren());
    myBuilder.removeElement(element);
    CHECK.empty(getRootChildren());
    myBuilder.removeElement(element);
    CHECK.empty(getRootChildren());
  }

  public void testUpdateOrder() {
    myBuilder.addElement(new TreeElement("a", null));
    TreeElement b = new TreeElement("b", "a");
    myBuilder.addElement(b);
    TreeModelBridge<? extends TreeElement> bNode = findNode("b");
    TreeElement c = new TreeElement("c", "a");
    myBuilder.addElement(c);
    TreeModelBridge<? extends TreeElement> cNode = findNode("c");
    CHECK.singleElement("a", getRootChildren());
    CHECK.order(new String[]{"b", "c"}, getChildrenOf("a"));
    b.setOrder(c.getOrder() + 1);
    myBuilder.updateElement(b);
    CHECK.order(new String[]{"c", "b"}, getChildrenOf("a"));
    assertSame(bNode, findNode("b"));
    assertSame(cNode, findNode("c"));
  }

  public void testAddParentOfItSelf() {
    myBuilder.addElement(new TreeElement("d", "b"));
    CHECK.singleElement("d", getRootChildren());
    myBuilder.addElement(new TreeElement("b", "b"));
    CHECK.singleElement("b", getRootChildren());
    CHECK.singleElement("d", getChildrenOf("b"));
    myBuilder.addElement(new TreeElement("c", "b"));
    CHECK.singleElement("b", getRootChildren());
    CHECK.order(new String[]{"d", "c"}, getChildrenOf("b"));
    myBuilder.addElement(new TreeElement("a", null));
    CHECK.order(new String[] {"b", "a"}, getRootChildren());
  }
  
  public void testMultipleUpdate() {
    TreeElement root = new TreeElement("root", null);
    myBuilder.addElement(root);
    TreeElement child = new TreeElement("child", "root");
    myBuilder.addElement(child);
    TreeElement sub1 = new TreeElement("sub1", "child");
    myBuilder.addElement(sub1);
    TreeElement sub2 = new TreeElement("sub2", "child");
    myBuilder.addElement(sub2);
    CHECK.singleElement("root", getRootChildren());
    CHECK.singleElement("child", getChildrenOf("root"));
    CHECK.order(new Object[] {"sub1", "sub2"}, getChildrenOf("child"));

    sub1.myParentId = "root";
    myBuilder.updateAll(Arrays.asList(sub2, sub1));
    CHECK.singleElement("root", getRootChildren());
    CHECK.order(new Object[] {"child", "sub1"}, getChildrenOf("root"));
    CHECK.singleElement("sub2", getChildrenOf("child"));
  }

  private List<String> getRootChildren() {
    return chilrenOf(myRoot);
  }

  private List<String> getChildrenOf(String nodeId) {
    TreeModelBridge<? extends TreeElement> desendant = findNode(nodeId);
    assertNotNull("Node not found: " + nodeId, desendant);
    return chilrenOf(desendant);
  }

  private TreeElement findTreeElement(String id) {
    TreeModelBridge<? extends TreeElement> desendant = findNode(id);
    assertNotNull("Node not found: " + id, desendant);
    return desendant.getUserObject();
  }

  private TreeModelBridge<? extends TreeElement> findNode(String nodeId) {
    return findNodeStartingAt(nodeId, myRoot);
  }

  private TreeModelBridge<? extends TreeElement> findNodeStartingAt(String nodeId, TreeModelBridge<? extends TreeElement> parent) {
    if (nodeId.equals(parent.getUserObject().getId()))
      return parent;
    for (int i = 0; i < parent.getChildCount(); i++) {
      TreeModelBridge<? extends TreeElement> desendant = findNodeStartingAt(nodeId, parent.getChildAt(i));
      if (desendant != null)
        return desendant;
    }
    return null;
  }

  private List<String> chilrenOf(TreeModelBridge<? extends TreeElement> parent) {
    List<String> children = Collections15.arrayList();
    for (int i = 0; i < parent.getChildCount(); i++) {
      TreeModelBridge<? extends TreeElement> child = parent.getChildAt(i);
      children.add(child.getUserObject().getId());
    }
    return children;
  }

  private static class TreeElement {
    private static int ourCounter = 0;
    private int myOrder = ourCounter++;
    private String myId;
    private String myParentId;

    public TreeElement(String id, String parentId) {
      myId = id;
      myParentId = parentId;
    }

    public int getOrder() {
      return myOrder;
    }

    public String getId() {
      return myId;
    }

    public String getParentId() {
      return myParentId;
    }

    public void setOrder(int order) {
      myOrder = order;
    }
  }
}
