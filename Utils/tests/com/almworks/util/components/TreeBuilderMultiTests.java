package com.almworks.util.components;

import com.almworks.util.collections.Containers;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.Collections15;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class TreeBuilderMultiTests extends GUITestCase {
  private static final String a = "a", b = "b", c = "c", d = "d", e = "e", x = "x", y = "y", z = "z";
  
  private final TreeModelBridge<TreeElement> myRoot = new TreeModelBridge<TreeElement>(new TreeElement(null));

  private final TreeStructure.MultiParent<TreeElement, String, TreeModelBridge<TreeElement>> myTreeStructure =
    new TreeStructure.MultiParent<TreeElement, String, TreeModelBridge<TreeElement>>() {
      public String getNodeKey(TreeElement element) {
        return element.getId();
      }

      public String getNodeParentKey(TreeElement element) {
        assertTrue("TreeBuilder should call getNodeParentKeys() instead.", false);
        return null;
      }

      @Override
      public Set<String> getNodeParentKeys(TreeElement element) {
        return Collections15.hashSet(element.getParentIds());
      }

      public TreeModelBridge<TreeElement> createTreeNode(TreeElement element) {
        return new TreeModelBridge<TreeElement>(element);
      }
    };

  private final TreeBuilder<TreeElement, TreeModelBridge<TreeElement>> myBuilder =
    new TreeBuilder<TreeElement, TreeModelBridge<TreeElement>>(
      myTreeStructure,
      new Comparator<TreeElement>() {
        public int compare(TreeElement o1, TreeElement o2) {
          return Containers.compareInts(o1.getOrder(), o2.getOrder());
        }
      },
      myRoot);

  private final CollectionsCompare CHECK = new CollectionsCompare();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myRoot.becomeRoot();
  }

  public void testAddRemove() throws Exception {
    myBuilder.clear();
    chk().atTop();

    add(a).atTop();
    chk(a).atTop();

    add(b).under(c, d);
    chk(a, b).atTop();

    add(c).atTop();
    chk(a, c).atTop();
    chk(b).under(c);

    add(d).under(a);
    chk(a, c).atTop();
    chk(d).under(a);
    chk(b).under(d);
    chk(b).under(c);

    add(e).under(b);
    add(x).under(b);
    chk(a, c).atTop();
    chk(d).under(a);
    chk(b).under(d);
    chk(b).under(c);
    chk(e, x).under(b);

    rem(d);
    chk(a, c).atTop();
    chk().under(a);
    chk(b).under(c);

    rem(y);
    chk(a, c).atTop();
    chk().under(a);
    chk(b).under(c);
    chk(e, x).under(b);

    add(d).under(a);
    chk(d).under(a);
    chk(b).under(d);
    chk(b).under(c);
    chk(e, x).under(b);

    rem(b);
    chk(a, c, e, x).atTop();
    chk(d).under(a);
    chk().under(d);
    chk().under(c);

    add(b).under(c, d);
    chk(a, c).atTop();
    chk(d).under(a);
    chk(b).under(d);
    chk(b).under(c);
    chk(e, x).under(b);

    rem(c);
    rem(d);
    chk(a, b).atTop();
    chk().under(a);
    chk(e, x).under(b);
  }

  public void testUpdateOrder() throws Exception {
    myBuilder.clear();
    chk().atTop();

    add(a).atTop();
    add(b).atTop();
    chk(a, b).atTop();

    upd(a).atTop().go();
    chk(a, b).atTop();

    upd(a).order().go();
    chk(b, a).atTop();
    upd(b).order().go();
    chk(a, b).atTop();

    add(x).under(a, b);
    add(y).under(a, b);
    chk(a, b).atTop();
    chk(x, y).under(a);
    chk(x, y).under(b);

    upd(x).order().go();
    chk(y, x).under(a);
    chk(y, x).under(b);

    add(z).under(b, a);
    chk(y, x, z).under(a);
    chk(y, x, z).under(b);

    upd(y).order().under(b).go();
    chk(a, b).atTop();
    chk(x, z).under(a);
    chk(x, z, y).under(b);

    upd(x).order().atTop().go();
    chk(a, b, x).atTop();
    chk(z).under(a);
    chk(z, y).under(b);

    upd(x).under(a, b).go();
    chk(a, b).atTop();
    chk(z, x).under(a);
    chk(z, y, x).under(b);
  }

  public void testUpdateStructure() throws Exception {
    myBuilder.clear();
    chk().atTop();

    add(a).atTop();
    add(b).atTop();
    chk(a, b).atTop();

    add(c).under(b, a);
    chk(a, b).atTop();
    chk(c).under(a);
    chk(c).under(b);

    upd(c).atTop().go();
    chk(a, b, c).atTop();
    chk().under(a);
    chk().under(b);

    upd(c).under(a, d).go();
    chk(a, b).atTop();
    chk(c).under(a);
    chk().under(b);

    upd(c).under(d, e).go();
    chk(a, b, c).atTop();
    chk().under(a);
    chk().under(b);

    upd(c).under(e).go();
    chk(a, b, c).atTop();
    add(d).atTop();
    chk(a, b, c, d).atTop();

    upd(c).atTop().go();
    chk(a, b, c, d).atTop();
    add(e).atTop();
    chk(a, b, c, d, e).atTop();
  }

  public void testAddLoops() throws Exception {
    myBuilder.clear();
    chk().atTop();

    add(a).under(c);
    chk(a).atTop();
    chk().under(a);

    add(c).under(a);
    chk(a).atTop();
    chk(c).under(a);
    chk().under(c);

    myBuilder.clear();
    chk().atTop();

    add(a).under(b);
    chk(a).atTop();
    add(b).under(c);
    chk(b).atTop();
    chk(a).under(b);

    add(c).under(a);
    chk(b).atTop();
    chk(a).under(b);
    chk(c).under(a);
    chk().under(c);
  }

  public void testUpdateLoops() throws Exception {
    myBuilder.clear();
    chk().atTop();

    add(a).atTop();
    add(b).atTop();
    add(c).under(a, b);
    add(d).under(c, x, y);
    chk(a, b).atTop();
    chk(c).under(a);
    chk(c).under(b);
    chk(d).under(c);

    upd(b).under(d).go();
    chk(a, b).atTop();
    chk().under(d);

    upd(b).under(d, a).go();
    chk(a).atTop();
    chk(b, c).under(a);
    chk(c).under(b);
    chk(d).under(c);
    chk().under(d);
  }

  private Adder add(String id) {
    return new Adder(id);
  }

  private class Adder {
    private final String myId;

    public Adder(String id) {
      myId = id;
    }

    public TreeElement atTop() {
      return under();
    }

    public TreeElement under(String... parents) {
      final TreeElement e = new TreeElement(myId, parents);
      myBuilder.addElement(e);
      return e;
    }
  }

  private Checker chk(String... ids) {
    return new Checker(ids);
  }

  private class Checker {
    private final String[] myIds;

    public Checker(String... ids) {
      myIds = ids;
    }

    public void atTop() {
      CHECK.order(myIds, getRootChildren());
    }

    public void under(String parent) {
      final List<List<String>> kidLists = getChildrenOf(parent);
      for(final List<String> kids : kidLists) {
        CHECK.order(myIds, kids);
      }
    }
  }

  private void rem(String id) {
    myBuilder.removeElement(new TreeElement(id));
  }

  private Updater upd(String id) {
    return new Updater(findTreeElement(id));
  }

  private class Updater {
    private final TreeElement myElement;

    public Updater(TreeElement element) {
      myElement = element;
    }

    public Updater atTop() {
      return under();
    }

    public Updater under(String... parents) {
      myElement.myParentIds = Collections15.arrayList(parents);
      return this;
    }

    public Updater order() {
      myElement.myOrder = TreeElement.ourCounter++;
      return this;
    }

    public void go() {
      myBuilder.updateElement(myElement);
    }
  }

  private List<String> getRootChildren() {
    return chilrenOf(myRoot);
  }

  private List<List<String>> getChildrenOf(String id) {
    final List<TreeModelBridge<TreeElement>> nodes = findNodes(id);
    assertFalse("Node not found: " + id, nodes.isEmpty());
    final List<List<String>> result = Collections15.arrayList();
    for(final TreeModelBridge<TreeElement> node : nodes) {
      result.add(chilrenOf(node));
    }
    return result;
  }

  private TreeElement findTreeElement(String id) {
    final List<TreeModelBridge<TreeElement>> nodes = findNodes(id);
    assertFalse("Node not found: " + id, nodes.isEmpty());
    TreeElement result = null;
    for(final TreeModelBridge<TreeElement> node : nodes) {
      if(result == null) {
        result = node.getUserObject();
      } else {
        assertSame("Different user objects for id " + id, result, node.getUserObject());
      }
    }
    return result;
  }

  private List<TreeModelBridge<TreeElement>> findNodes(String nodeId) {
    final List<TreeModelBridge<TreeElement>> result = Collections15.arrayList();
    findNodesStartingAt(nodeId, myRoot, result);
    return result;
  }

  private void findNodesStartingAt(String nodeId, TreeModelBridge<TreeElement> parent, List<TreeModelBridge<TreeElement>> result) {
    if(nodeId.equals(parent.getUserObject().getId())) {
      result.add(parent);
    }
    for(int i = 0; i < parent.getChildCount(); i++) {
      findNodesStartingAt(nodeId, parent.getChildAt(i), result);
    }
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
    private List<String> myParentIds;

    public TreeElement(String id, String... parentIds) {
      myId = id;
      myParentIds = Collections15.arrayList(parentIds);
    }

    public int getOrder() {
      return myOrder;
    }

    public String getId() {
      return myId;
    }

    public List<String> getParentIds() {
      return myParentIds;
    }

    @Override
    public String toString() {
      return myId;
    }
  }
}
