package com.almworks.util.components;

import com.almworks.util.Pair;
import com.almworks.util.commons.Condition;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;

public class TreeModelFilterTests extends BaseTestCase {
  private TreeModelFilter<String> myModel;
  private TreeModelFilter<String> myModelChecking;
//  private Condition2<ATreeNode<String>, ATreeNode<String>> myFilter;


  protected void setUp() throws Exception {
    super.setUp();
    myModel = TreeModelFilter.create();
    myModelChecking = TreeModelFilter.create();
    myModelChecking.setSourceRoot(myModel.getFilteredRoot());
  }

  protected void tearDown() throws Exception {
    myModel = null;
    super.tearDown();
  }


  public TreeModelFilterTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  public void testSimple() {
    checkStatic("", "");
    checkStatic("a", "a");
    checkStatic("a,b,c", "a,b,c");
    checkStatic("a(b,c,d)", "a(b,c,d)");
    checkStatic("a(b(1,2,3),c(4,5,6),d(7,8,9))", "a(b(1,2,3),c(4,5,6),d(7,8,9))");
  }

  public void testInsert() {
    setSource("");
    add("a", "a");
    add("b", "a,b");
    add("c", "a,b,c");
    add("a.1", "a(1),b,c");
    add("a.1.2", "a(1(2)),b,c");
    add("a.1.4", "a(1(2,4)),b,c");
    add("b.x", "a(1(2,4)),b(x),c");
    add("c.y", "a(1(2,4)),b(x),c(y)");
    add("d", "a(1(2,4)),b(x),c(y),d");
    insert("v", 0, "v,a(1(2,4)),b(x),c(y),d");
    insert("vv", 0, "vv,v,a(1(2,4)),b(x),c(y),d");
    insert("a.2", 1, "vv,v,a(1(2,4),2),b(x),c(y),d");
    insert("a.1.5", 1, "vv,v,a(1(2,5,4),2),b(x),c(y),d");
    insert("a.1.6", 2, "vv,v,a(1(2,5,6,4),2),b(x),c(y),d");
    insert("z", 4, "vv,v,a(1(2,5,6,4),2),b(x),z,c(y),d");
    insert("zz", 7, "vv,v,a(1(2,5,6,4),2),b(x),z,c(y),d,zz");
  }

  public void testRemove() {
    setSource("vv,v,a(1(2,5,6,4),2),b(x),z,c(y),d,zz");
    remove("zz", "vv,v,a(1(2,5,6,4),2),b(x),z,c(y),d");
    remove("vv", "v,a(1(2,5,6,4),2),b(x),z,c(y),d");
    remove("a.1.5", "v,a(1(2,6,4),2),b(x),z,c(y),d");
    remove("a.1.4", "v,a(1(2,6),2),b(x),z,c(y),d");
    remove("a.1", "v,a(2),b(x),z,c(y),d");
    remove("b", "v,a(2),z,c(y),d");
    remove("c.y", "v,a(2),z,c,d");
    remove("v", "a(2),z,c,d");
    remove("a", "z,c,d");
    remove("c", "z,d");
    remove("d", "z");
    remove("z", "");
  }

  public void testChange() {
    setSource("vv,v,a(1(2,5,6,4),2),b(x),z,c(y),d,zz");
    change("a.1.2", "22", "vv,v,a(1(22,5,6,4),2),b(x),z,c(y),d,zz");
    change("a.1", "3", "vv,v,a(3(22,5,6,4),2),b(x),z,c(y),d,zz");
    change("zz", "xc", "vv,v,a(3(22,5,6,4),2),b(x),z,c(y),d,xc");
    change("vv", "w", "w,v,a(3(22,5,6,4),2),b(x),z,c(y),d,xc");
    change("a", "vv", "w,v,vv(3(22,5,6,4),2),b(x),z,c(y),d,xc");
    remove("vv.3", "w,v,vv(2),b(x),z,c(y),d,xc");
  }

  // todo filtering tests
  // todo changing node (seeded bug)

  public void testSetFilter() {
    setSource("vv,v,a(1(2,5,6,4),2),b(x),z,c(y),d,zz");
    myModel.setFilter(always());
    checkFiltered("vv,v,a(1(2,5,6,4),2),b(x),z,c(y),d,zz");
    myModel.setFilter(never());
    checkFiltered("");
    myModel.setFilter(new LeafNotNumbers());
    checkFiltered("vv,v,a(1),b(x),z,c(y),d,zz");
    myModel.setFilter(null);
    checkFiltered("vv,v,a(1(2,5,6,4),2),b(x),z,c(y),d,zz");

    myModel.setSourceRoot(null);
    myModel.setFilter(new EvenNumbersFilter());
    setSource("a(2(1,2,3,4,5),3(1,2,3,4,5)),b,c,d(1,2,3),e(e(e(e(e(2(1))))))");

    checkFiltered("a(2(2,4)),b,c,d(2),e(e(e(e(e(2)))))");
  }

  private Condition<ATreeNode<String>> always() {
    return Condition.<ATreeNode<String>>always();
  }

  private Condition<ATreeNode<String>> never() {
    return Condition.<ATreeNode<String>>never();
  }

  public void testFilteredOperations() {
    myModel.setFilter(new EvenNumbersFilter());
    setSource("a(2(1,2,3,4,5),3(1,2,3,4,5)),b,c,d(1,2,3),e(e(e(e(e(2(1))))))");
    checkFiltered("a(2(2,4)),b,c,d(2),e(e(e(e(e(2)))))");

    add("a.2.1.2", "a(2(2,4)),b,c,d(2),e(e(e(e(e(2)))))"); // no change - a.2.1 filtered out
    add("a.2.100", "a(2(2,4,100)),b,c,d(2),e(e(e(e(e(2)))))");
    add("a.3.100", "a(2(2,4,100)),b,c,d(2),e(e(e(e(e(2)))))");
    add("a.2.101", "a(2(2,4,100)),b,c,d(2),e(e(e(e(e(2)))))");
    add("a.3.101", "a(2(2,4,100)),b,c,d(2),e(e(e(e(e(2)))))");
    add("1", "a(2(2,4,100)),b,c,d(2),e(e(e(e(e(2)))))");
    add("2", "a(2(2,4,100)),b,c,d(2),e(e(e(e(e(2))))),2");

    myModel.setFilter(null);
    checkFiltered("a(2(1(2),2,3,4,5,100,101),3(1,2,3,4,5,100,101)),b,c,d(1,2,3),e(e(e(e(e(2(1)))))),1,2");
    myModel.setFilter(new EvenNumbersFilter());
    checkFiltered("a(2(2,4,100)),b,c,d(2),e(e(e(e(e(2))))),2");

    insert("3", 0, "a(2(2,4,100)),b,c,d(2),e(e(e(e(e(2))))),2");
    insert("4", 0, "4,a(2(2,4,100)),b,c,d(2),e(e(e(e(e(2))))),2");
    insert("a.2.200", 4, "4,a(2(2,4,200,100)),b,c,d(2),e(e(e(e(e(2))))),2");
    insert("a.2.201", 6, "4,a(2(2,4,200,100)),b,c,d(2),e(e(e(e(e(2))))),2");
    insert("a.2.202", 6, "4,a(2(2,4,200,202,100)),b,c,d(2),e(e(e(e(e(2))))),2");
    insert("e.e.e.e.e.1", 1, "4,a(2(2,4,200,202,100)),b,c,d(2),e(e(e(e(e(2))))),2");
    insert("e.e.e.e.e.4", 2, "4,a(2(2,4,200,202,100)),b,c,d(2),e(e(e(e(e(2,4))))),2");

    myModel.setFilter(null);
    checkFiltered("4,3,a(2(1(2),2,3,4,200,5,202,201,100,101),3(1,2,3,4,5,100,101)),b,c,d(1,2,3),e(e(e(e(e(2(1),1,4))))),1,2");
    myModel.setFilter(new EvenNumbersFilter());
    checkFiltered("4,a(2(2,4,200,202,100)),b,c,d(2),e(e(e(e(e(2,4))))),2");

    change("a", "x", "4,x(2(2,4,200,202,100)),b,c,d(2),e(e(e(e(e(2,4))))),2");
    change("4", "5", "x(2(2,4,200,202,100)),b,c,d(2),e(e(e(e(e(2,4))))),2");
    change("1", "32", "x(2(2,4,200,202,100)),b,c,d(2),e(e(e(e(e(2,4))))),32,2");
    change("x.2.1", "0", "x(2(0(2),2,4,200,202,100)),b,c,d(2),e(e(e(e(e(2,4))))),32,2");
    change("e.e", "33", "x(2(0(2),2,4,200,202,100)),b,c,d(2),e,32,2");
    change("x.2.0.2", "1", "x(2(0,2,4,200,202,100)),b,c,d(2),e,32,2");
    change("x.2.0", "1", "x(2(2,4,200,202,100)),b,c,d(2),e,32,2");
    change("x.2.1.1", "2", "x(2(2,4,200,202,100)),b,c,d(2),e,32,2");
    change("x.2.1", "0", "x(2(0(2),2,4,200,202,100)),b,c,d(2),e,32,2");
    change("x.2.0.2", "1", "x(2(0,2,4,200,202,100)),b,c,d(2),e,32,2");

    myModel.setFilter(null);
    checkFiltered("5,3,x(2(0(1),2,3,4,200,5,202,201,100,101),3(1,2,3,4,5,100,101)),b,c,d(1,2,3),e(33(e(e(e(2(1),1,4))))),32,2");
    myModel.setFilter(new EvenNumbersFilter());
    checkFiltered("x(2(0,2,4,200,202,100)),b,c,d(2),e,32,2");

    remove("3", "x(2(0,2,4,200,202,100)),b,c,d(2),e,32,2");
    remove("x.3", "x(2(0,2,4,200,202,100)),b,c,d(2),e,32,2");
    remove("b", "x(2(0,2,4,200,202,100)),c,d(2),e,32,2");
    remove("e.33.e.e", "x(2(0,2,4,200,202,100)),c,d(2),e,32,2");
    change("e.33", "e", "x(2(0,2,4,200,202,100)),c,d(2),e(e(e)),32,2");
    remove("2", "x(2(0,2,4,200,202,100)),c,d(2),e(e(e)),32");
    remove("5", "x(2(0,2,4,200,202,100)),c,d(2),e(e(e)),32");
    remove("c", "x(2(0,2,4,200,202,100)),d(2),e(e(e)),32");
    remove("d", "x(2(0,2,4,200,202,100)),e(e(e)),32");
    remove("x.2.201", "x(2(0,2,4,200,202,100)),e(e(e)),32");
    remove("x.2.202", "x(2(0,2,4,200,100)),e(e(e)),32");
    remove("x.2.101", "x(2(0,2,4,200,100)),e(e(e)),32");
    remove("x.2.100", "x(2(0,2,4,200)),e(e(e)),32");

    myModel.setFilter(null);
    checkFiltered("x(2(0(1),2,3,4,200,5)),e(e(e)),32");
    myModel.setFilter(new EvenNumbersFilter());
    checkFiltered("x(2(0,2,4,200)),e(e(e)),32");

    remove("x", "e(e(e)),32");
    remove("e", "32");
    remove("32", "");

    myModel.setFilter(null);
    checkFiltered("");
  }

  public void testAddTree() {
    setSource("a,b,c(1,2,3)");
    addTree("c.1", "x,y,z(5,6,7)", "a,b,c(1(root(x,y,z(5,6,7))),2,3)");
    myModel.setFilter(new EvenNumbersFilter());
    checkFiltered("a,b,c(2)");
    addTree("c.1.root.x", "1,3,5,4", "a,b,c(2)");
    addTree("b", "1,3,5,4", "a,b(root(4)),c(2)");
    addTree("b.root.4", "1(1(1))", "a,b(root(4(root))),c(2)");
    addTree("a", "2(2(2))", "a(root(2(2(2)))),b(root(4(root))),c(2)");
    myModel.setFilter(null);
    checkFiltered("a(root(2(2(2)))),b(root(1,3,5,4(root(1(1(1)))))),c(1(root(x(root(1,3,5,4)),y,z(5,6,7))),2,3)");
  }

  public void testOperationsOnFilteredTree() {
    setSource("a,b,c(1,2,3)");
    myModel.setFilter(new EvenNumbersFilter());
    getFilteredNode("c.2").removeFromParent();
    checkModels("a,b,c", "a,b,c(1,3)");
    getFilteredNode("").add(getFilteredNode("").remove(0));
    checkModels("b,c,a", "b,c(1,3),a");
    getFilteredNode("").insert(buildTree("5,6,7,8"), 0);
    checkModels("root(6,8),b,c,a", "root(5,6,7,8),b,c(1,3),a");
  }

  public void testRegressionRemovedNodesRemainAttached() {
    // failed to isolate problem :(
    setSource("2,3(4,5(8,9),6)");
    myModel.setFilter(new EvenNumbersFilter());
    checkModels("2", "2,3(4,5(8,9),6)");
    change("3.5.9", "10", "2");
    myModel.setFilter(null);
    checkModels("2,3(4,5(8,10),6)", "2,3(4,5(8,10),6)");
  }

  private void checkModels(String filteredSpec, String sourceSpec) {
    checkFiltered(filteredSpec);
    checkSource(sourceSpec);
  }

  private TreeFilterNode<String> getFilteredNode(String path) {
    return this.<String, TreeFilterNode<String>>findNode(path, myModel.getFilteredRoot());
  }

  private void addTree(String parentPath, String subtreeSpec, String filteredSpec) {
    TreeModelBridge<String> parent = getSourceNode(parentPath);
    TreeModelBridge<String> subTree = buildTree(subtreeSpec);
    parent.add(subTree);
    checkFiltered(filteredSpec);
  }


  private void change(String path, String newName, String filteredSpec) {
    Pair<TreeModelBridge<String>, String> pair = getParentAndName(path);
    TreeModelBridge<String> node = pair.getFirst();
    String name = pair.getSecond();
    for (int i = 0; i < node.getChildCount(); i++) {
      TreeModelBridge<String> child = node.getChildAt(i);
      if (name.equals(child.getUserObject())) {
        child.setUserObject(newName);
        break;
      }
    }
    checkFiltered(filteredSpec);
  }

  private void add(String path, String filteredSpec) {
    insert(path, -1, filteredSpec);
  }

  private void insert(String path, int place, String filteredSpec) {
    Pair<TreeModelBridge<String>, String> pair = getParentAndName(path);
    TreeModelBridge<String> child = new TreeModelBridge<String>(pair.getSecond());
    if (place >= 0)
      pair.getFirst().insert(child, place);
    else
      pair.getFirst().add(child);
    checkFiltered(filteredSpec);
  }

  private Pair<TreeModelBridge<String>, String> getParentAndName(String path) {
    int k = path.lastIndexOf('.');
    String parentPath = k <= 0 ? null : path.substring(0, k);
    String name = path.substring(k + 1);
    TreeModelBridge<String> parent = getSourceNode(parentPath);
    if (parent == null)
      throw new IllegalArgumentException("bad path " + path);
    return Pair.create(parent, name);
  }

  private TreeModelBridge<String> getSourceNode(String parentPath) {
    return this.<String, TreeModelBridge<String>>findNode(parentPath, (TreeModelBridge<String>) myModel.getSourceRoot());
  }

  private <T, N extends ATreeNode<T>> N findNode(String path, N root) {
    N node = root;
    String iterpath = path;
    while (iterpath != null && iterpath.length() > 0) {
      int k = iterpath.indexOf('.');
      String name = k <= 0 ? iterpath : iterpath.substring(0, k);
      if (name.length() == 0)
        throw new IllegalArgumentException("bad path " + path);
      iterpath = k <= 0 ? null : iterpath.substring(k + 1);
      int i;
      int childCount = node.getChildCount();
      for (i = 0; i < childCount; i++) {
        N child = (N)node.getChildAt(i);
        if (name.equals(child.getUserObject())) {
          node = child;
          break;
        }
      }
      if (i == childCount)
        throw new IllegalArgumentException("bad path " + path);
    }
    return node;
  }

  private void remove(String path, String filteredSpec) {
    Pair<TreeModelBridge<String>, String> pair = getParentAndName(path);
    TreeModelBridge<String> node = pair.getFirst();
    String name = pair.getSecond();
    for (int i = 0; i < node.getChildCount(); i++) {
      if (name.equals(node.getChildAt(i).getUserObject())) {
        node.remove(i);
        break;
      }
    }
    checkFiltered(filteredSpec);
  }

  private void checkStatic(String sourceSpec, String filteredSpec) {
    setSource(sourceSpec);
    checkFiltered(filteredSpec);
  }

  private void setSource(String sourceSpec) {
    TreeModelBridge<String> source = buildTree(sourceSpec);
    myModel.setSourceRoot(source);
  }

  private TreeModelBridge<String> buildTree(String spec) {
    TreeModelBridge<String> result = new TreeModelBridge<String>("root");
    buildChildren(result, spec);
    return result;
  }

  private void buildChildren(TreeModelBridge<String> parent, String spec) {
    if (spec == null || spec.length() == 0)
      return;
    int length = spec.length();
    int next = 0;
    for (int i = 0; i <= length; i++) {
      char c = i < length ? spec.charAt(i) : 0;
      if (c == ',' || c == '(' || c == 0) {
        if (i == next ) {
          if (c != 0)
            throw new IllegalArgumentException("bad spec " + spec);
          else
            break;
        }
        String name = spec.substring(next, i).trim();
        if (name.length() == 0)
          throw new IllegalArgumentException("bad spec " + spec);
        TreeModelBridge<String> node = new TreeModelBridge<String>(name);
        parent.add(node);
        if (c == '(') {
          String subspec = subSpec(spec, i);
          buildChildren(node, subspec);
          i += subspec.length() + 1; // subspec is without braces
          if (i + 1 < length && spec.charAt(i + 1) == ',')
            i++;
        }
        next = i + 1;
      }
    }
    assert next >= length : next + " " + length;
  }

  private String subSpec(String spec, int braceIndex) {
    assert spec.charAt(braceIndex) == '(';
    int count = 0;
    for (int i = braceIndex + 1; i < spec.length(); i++) {
      char c = spec.charAt(i);
      if (c == '(')
        count++;
      if (c == ')')
        count--;
      if (count < 0)
        return spec.substring(braceIndex + 1, i);
    }
    throw new IllegalArgumentException("bad spec " + spec + " " + braceIndex);
  }

  private void checkSource(String sourceSpec) {
    TreeModelBridge<String> expected = buildTree(sourceSpec);
    String expectedLines = buildLines(expected);
    assertEquals(expectedLines, buildLines(myModel.getSourceRoot()));
  }

  private void checkFiltered(String filteredSpec) {
    TreeModelBridge<String> expected = buildTree(filteredSpec);
    String expectedLines = buildLines(expected);
    assertEquals(expectedLines, buildLines(myModel.getFilteredRoot()));
    assertEquals(expectedLines, buildLines(myModelChecking.getFilteredRoot()));
  }

  private String buildLines(ATreeNode<String> root) {
    StringBuffer result = new StringBuffer();
    buildLines(result, root, null);
    return result.toString();
  }
  private void buildLines(StringBuffer result, ATreeNode<String> root, String prefix) {
    String name = root.getUserObject();
    prefix = prefix == null ? name : prefix + "." + name;
    result.append(prefix).append('\n');
    for (int i = 0; i < root.getChildCount(); i++) {
      buildLines(result, root.getChildAt(i), prefix);
    }
  }

  private static class LeafNotNumbers extends Condition<ATreeNode<String>> {
    public boolean isAccepted(ATreeNode<String> value) {
      if (!value.isLeaf())
        return true;
      try {
        Integer.parseInt(value.getUserObject());
      } catch(NumberFormatException e) {
        return true;
      }
      return false;
    }
  }


  private static class EvenNumbersFilter extends Condition<ATreeNode<String>> {
    public boolean isAccepted(ATreeNode<String> value) {
      try {
        int n = Integer.parseInt(value.getUserObject());
        return n % 2 == 0;
      } catch (NumberFormatException e) {
        return true;
      }
    }
  }
}
