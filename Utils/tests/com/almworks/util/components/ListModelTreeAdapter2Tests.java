package com.almworks.util.components;

import com.almworks.util.Pair;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.FactoryWithParameter;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.List;

/**
 * @author dyoma
 */
public class ListModelTreeAdapter2Tests extends GUITestCase {
  private final FactoryWithParameter<TreeModelBridge<TreeElement>, TreeElement> myNodeFactory =
    new FactoryWithParameter<TreeModelBridge<TreeElement>, TreeElement>() {
      public TreeModelBridge<TreeElement> create(TreeElement parameter) {
        return new TreeModelBridge<TreeElement>(parameter);
      }
  };
  private final Convertor<TreeElement, Pair<Object, Object>> myTreeFunction =
    new Convertor<TreeElement, Pair<Object, Object>>() {
      public Pair<Object, Object> convert(TreeElement value) {
        return Pair.<Object, Object>create(value.getId(), value.getParentId());
      }
    };
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final OrderListModel<TreeElement> myList = OrderListModel.create();
  private final ListModelTreeAdapter<TreeElement, TreeModelBridge<TreeElement>> myAdapter = ListModelTreeAdapter.create(myList, myNodeFactory, myTreeFunction, Containers.<TreeElement>comparablesComparator());
  private final TreeElement myRoot = new TreeElement(null);
  private TreeModelBridge<TreeElement> myRootNode;

  protected void setUp() throws Exception {
    super.setUp();
    myAdapter.attach(Lifespan.FOREVER);
    myRootNode = myAdapter.getRootNode();
//    assertEquals(1, treeRootNode.getChildCount());
//    myRootNode = (TreeModelBridge<TreeElement>) treeRootNode.getChildAt(0);
  }

  public void testBuildingTree() {
    TreeElement child1 = new TreeElement(myRoot);
    myList.addElement(child1);
    CHECK.singleElement(child1, collectChildren(myRootNode));
    TreeElement child2 = new TreeElement(myRoot);
    myList.addElement(child2);
    CHECK.order(new TreeElement[]{child1, child2}, collectChildren(myRootNode));
  }

  public void testBuildingSubTreeAndAddWhole() {
    TreeElement node = new TreeElement(myRoot);
    TreeElement child1 = new TreeElement(node);
    myList.addElement(child1);
    CHECK.singleElement(child1, collectChildren(myRootNode));
    myList.addElement(node);
    printTree(myRootNode);
    TreeModelBridge<TreeElement> nodeNode = myRootNode.getChildAt(0);
    CHECK.singleElement(node, collectChildren(myRootNode));
    CHECK.singleElement(child1, collectChildren(nodeNode));
  }

  public void testUpdateElementInList() {
    TreeElement node = new TreeElement(myRoot);
    TreeElement child = new TreeElement(node);
    myList.addElement(node);
    myList.addElement(child);
    TreeModelBridge<TreeElement> nodeNode = myRootNode.getChildAt(0);
    CHECK.singleElement(node, collectChildren(myRootNode));
    CHECK.singleElement(child, collectChildren(nodeNode));

    /**
     * Limitations not implemented yet
     * @see TreeBuilder
     */
//    node.setId("newId");
//    myList.updateElement(node);
//    printTree(myRootNode);
//    CHECK.order(new TreeElement[]{node, child}, collectChildren(myRootNode));
//    child.setParentId("newId");
//    myList.updateElement(child);
//    CHECK.singleElement(node, collectChildren(myRootNode));
//    CHECK.singleElement(child, collectChildren(nodeNode));
  }

  private List<TreeElement> collectChildren(TreeModelBridge<TreeElement> node) {
    List<ATreeNode<TreeElement>> children = node.childrenToList();
    List<TreeElement> result = Collections15.arrayList(children.size());
    for (ATreeNode<TreeElement> child : children) {
      result.add(child.getUserObject());
    }
    return result;
  }

  private void printTree(TreeModelBridge<? extends TreeElement> node) {
    printSubTree(0, node);
  }

  private void printSubTree(int offset, TreeModelBridge node) {
    for (int i = 0; i < offset; i++)
      System.out.print("  ");
    System.out.println(node.getUserObject());
    List<TreeModelBridge> children = node.childrenToList();
    for (int i = 0; i < children.size(); i++) {
      TreeModelBridge child = children.get(i);
      printSubTree(offset + 1, child);
    }
  }

  private static class TreeElement implements Comparable<TreeElement> {
    private static int ourCount = 1;
    private int myOrder = ourCount++;
    private TreeElement myParent;
    private Object myId;
    private Object myParentId;

    public TreeElement(TreeElement parent) {
      myParent = parent;
      myId = this;
      myParentId = myParent != null ? myParent.getId() : null;
    }

    public int compareTo(TreeElement o) {
      return Containers.compareInts(myOrder, o.myOrder);
    }

    public String toString() {
      return "(Node[" + myOrder +
        (myId != this ? ", '" + myId + "'" : "") +
        "] Parent: " + (myParentId != null ? myParentId.toString() : "<null>") + ")";
    }

    public Object getId() {
      return myId;
    }

    public void setId(Object id) {
      myId = id;
    }

    public Object getParentId() {
      return myParentId;
    }

    public void setParentId(Object parentId) {
      myParentId = parentId;
    }
  }
}
