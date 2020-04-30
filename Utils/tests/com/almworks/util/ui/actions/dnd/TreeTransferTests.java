package com.almworks.util.ui.actions.dnd;

import com.almworks.util.components.ATree;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.ui.TreeUtil;
import com.almworks.util.ui.actions.CantPerformException;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
public class TreeTransferTests extends BaseTestCase {
  private final MockTransferService myTransferService = new MockTransferService();
  private final TreeStringTransfer myTransfer = new TreeStringTransfer(myTransferService);
  private ATree myTree;
  private final TreeModelBridge<String> myRoot = TreeModelBridge.create("root");

  public TreeTransferTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    myTree = new ATree();
    myTree.setRoot(myRoot);
    myTree.setTransfer(myTransfer);
  }

  public void testCopy() throws CantPerformException, UnsupportedFlavorException, IOException {
    TreeModelBridge<String> child1 = TreeModelBridge.create("abc");
    myRoot.add(child1);
    DragContext context = createContext(child1);
    assertTrue(myTransfer.canCopy(context));
    assertEquals("abc", myTransfer.transfer(context).getTransferData(DataFlavor.stringFlavor));

    TreeModelBridge<String> child2 = TreeModelBridge.create("-abc");
    myRoot.add(child2);
    context = createContext(child2);
    assertFalse(myTransfer.canCopy(context));
  }

  public void testPasteOnReplacable() throws CantPerformException, UnsupportedFlavorException, IOException {
    TreeModelBridge<String> child1 = TreeModelBridge.create("child1");
    TreeModelBridge<String> child2 = TreeModelBridge.create("child2");
    TreeModelBridge<String> replacable = TreeModelBridge.create("=abc");
    myRoot.add(child1);
    myRoot.add(replacable);
    myRoot.add(child2);

    DragContext context = createContext(replacable);
    StringSelection transferable = new StringSelection("newNode");
    context.putValue(DndUtil.TRANSFERABLE, transferable);
    assertTrue(myTransfer.canImportDataNow(context, null));
    myTransfer.acceptTransfer(context, transferable);
    assertEquals(3, myRoot.getChildCount());
    assertEquals("newNode", myRoot.getChildAt(1).getUserObject());
    assertSame(child1, myRoot.getChildAt(0));
    assertSame(child2, myRoot.getChildAt(2));
  }

  public void testPasteAfter() throws CantPerformException, UnsupportedFlavorException, IOException {
    TreeModelBridge<String> child = TreeModelBridge.create("child");
    myRoot.add(child);

    DragContext context = createContext(myRoot);
    StringSelection transfer = new StringSelection("new");
    context.putValue(DndUtil.TRANSFERABLE, transfer);
    assertTrue(myTransfer.canImportDataNow(context, null));
    myTransfer.acceptTransfer(context, transfer);
    assertEquals(2, myRoot.getChildCount());
    assertSame(child, myRoot.getChildAt(0));
    assertEquals("new", myRoot.getChildAt(1).getUserObject());
  }

  public void testPasteFlatten() throws CantPerformException, UnsupportedFlavorException, IOException {
    TreeModelBridge<String> child1 = TreeModelBridge.create("child1");
    TreeModelBridge<String> child2 = TreeModelBridge.create("child2");
    myRoot.add(child1);
    myRoot.add(child2);

    DragContext context = createContext(myRoot);
    StringSelection transfer = new StringSelection("+group(sub1,sub2,+sub3)");
    context.putValue(DndUtil.TRANSFERABLE, transfer);
    assertTrue(myTransfer.canImportDataNow(context, null));
    myTransfer.acceptTransfer(context, transfer);
    assertEquals(4, myRoot.getChildCount());
    assertSame(child1, myRoot.getChildAt(0));
    assertSame(child2, myRoot.getChildAt(1));
    assertEquals("sub1", myRoot.getChildAt(2).getUserObject());
    assertEquals("sub2", myRoot.getChildAt(3).getUserObject());
  }

  public void testPasteForebidden() throws CantPerformException {
    TreeModelBridge<String> child1 = TreeModelBridge.create("child1");
    myRoot.add(child1);
    TreeModelBridge<String> sub1 = TreeModelBridge.create("sub1");
    child1.add(sub1);
    TreeModelBridge<String> child2 = TreeModelBridge.create("#child2");
    myRoot.add(child2);
    TreeModelBridge<String> sub2 = TreeModelBridge.create("sub2");
    child2.add(sub2);

    DragContext context = createContext(sub1);
    context.putValue(DndUtil.TRANSFERABLE, new StringSelection("whatever"));
    assertTrue(myTransfer.canImportFlavor(DataFlavor.stringFlavor));
//    assertFalse(myTransfer.canImport(createContext(sub2)));
  }

  public void testRemoveForebidden() throws CantPerformException {
    TreeModelBridge<String> child1 = TreeModelBridge.create("!child1");
    myRoot.add(child1);
    TreeModelBridge<String> child2 = TreeModelBridge.create("child2");
    myRoot.add(child2);

    assertFalse(myTransfer.canRemove(createContext(child1)));
    assertTrue(myTransfer.canRemove(createContext(child2)));
  }

  public void testSimpleRemove() throws CantPerformException {
    TreeModelBridge<String> child1 = TreeModelBridge.create("child1");
    myRoot.add(child1);

    myTransfer.remove(createContext(child1));
    assertEquals(0, myRoot.getChildCount());
    assertTrue(myTree.getSelectionAccessor().isSelected(myRoot));
  }

  public void testSimpleRemove2() throws CantPerformException {
    TreeModelBridge<String> child1 = TreeModelBridge.create("child1");
    myRoot.add(child1);
    TreeModelBridge<String> child2 = TreeModelBridge.create("child1");
    myRoot.add(child2);

    myTransfer.remove(createContext(child2));
    assertEquals(1, myRoot.getChildCount());
    assertTrue(myTree.isExpanded(myRoot));
    assertTrue(myTree.getSelectionAccessor().isSelected(child1));
  }

  public void testRemoveVisibleRoot() throws CantPerformException {
    myTree.setRootVisible(true);

    myTransfer.remove(createContext(myRoot));
    assertEquals(MockTransferService.DEFAULT_ROOT, myTree.getRoot().getUserObject());

    myTree.setRootVisible(false);
    TreeModelBridge<String> visibleRoot = TreeModelBridge.create("visibleRoot");
    myTree.setRoot(myRoot);
    myRoot.add(visibleRoot);
    myTransfer.remove(createContext(visibleRoot));
    TreeModelBridge treeRoot = (TreeModelBridge) myTree.getRoot();
    assertEquals(1, treeRoot.getChildCount());
    TreeModelBridge newVisibleRoot = treeRoot.getChildAt(0);
    assertEquals(MockTransferService.DEFAULT_ROOT, newVisibleRoot.getUserObject());
    assertTrue(myTree.isExpanded(newVisibleRoot.getParent()));
    assertTrue(myTree.getSelectionAccessor().isSelected(newVisibleRoot));
    assertFalse(myTree.getSelectionAccessor().isSelected(myTree.getRoot()));
  }

  public void testFlatteningWhenRemove() throws CantPerformException {
    TreeModelBridge<String> child1 = TreeModelBridge.create("child1");
    myRoot.add(child1);
    TreeModelBridge<String> child2 = TreeModelBridge.create("+child2");
    myRoot.add(child2);
    TreeModelBridge<String> sub1 = TreeModelBridge.create("sub1");
    child2.add(sub1);
    TreeModelBridge<String> sub2 = TreeModelBridge.create("+sub2");
    child2.add(sub2);
    TreeModelBridge<String> toRemove = TreeModelBridge.create("toRemove");
    sub2.add(toRemove);
    myTree.setRootVisible(true);
    myTree.expand(myRoot);
    myTree.expand(child2);
    myTree.expand(sub2);

    myTransfer.remove(createContext(toRemove));
    assertEquals(2, myRoot.getChildCount());
    assertSame(child1, myRoot.getChildAt(0));
    assertEquals(0, child1.getChildCount());
    assertSame(sub1, myRoot.getChildAt(1));
    assertEquals(0, sub1.getChildCount());
    assertTrue(myTree.isExpanded(myRoot));
    assertTrue(myTree.getSelectionAccessor().isSelected(sub1));
  }

  public void testFlatteningWhenRemove2() throws CantPerformException {
    TreeModelBridge<String> toFlatten = TreeModelBridge.create("+toFlatten");
    TreeModelBridge<String> expanded = TreeModelBridge.create("expanded");
    expanded.add(TreeModelBridge.create("child1"));
    TreeModelBridge<String> collapsed = TreeModelBridge.create("collapsed");
    collapsed.add(TreeModelBridge.create("child2"));
    TreeModelBridge<String> toRemove = TreeModelBridge.create("toRemove");
    myRoot.add(toFlatten);
    toFlatten.add(expanded);
    toFlatten.add(collapsed);
    toFlatten.add(toRemove);
    myTree.setRootVisible(true);
    myTree.expand(myRoot);
    myTree.expand(toFlatten);
    myTree.expand(expanded);

    myTransfer.remove(createContext(toRemove));
    assertTrue(myTree.getSelectionAccessor().isSelected(collapsed));
    assertTrue(myTree.isExpanded(myRoot));
    assertTrue(myTree.isExpanded(expanded));
  }

  public void testFlatteningWhenRemove3() throws CantPerformException {
    TreeModelBridge<String> toFlatten = TreeModelBridge.create("+toFlatten");
    TreeModelBridge<String> parent = TreeModelBridge.create("parent");
    parent.add(TreeModelBridge.create("child"));
    TreeModelBridge<String> toRemove = TreeModelBridge.create("toRemove");
    myRoot.add(toFlatten);
    toFlatten.add(parent);
    toFlatten.add(toRemove);
    myTree.expand(myRoot);
    myTree.expand(parent);
    myTree.setRootVisible(false);

    myTransfer.remove(createContext(toRemove));
    assertTrue(myTree.isExpanded(myRoot));
    assertTrue(myTree.isExpanded(parent));
    assertTrue(myTree.getSelectionAccessor().isSelected(parent));
  }

  private DragContext createContext(TreeModelBridge<String> node) {
    assertSame(myRoot, TreeUtil.getRoot(node));
    myTree.getSelectionAccessor().setSelected(node);
    DragContext context = new DragContext(myTree);

    context.putValue(DndUtil.FROM_CLIPBOARD, true);
    return context;
  }

  private static class MockTransferService implements TreeStringTransferService {
    private static final String DEFAULT_ROOT = "DefaultRoot";

    public String exportString(ATreeNode node) {
      return node.getUserObject().toString();
    }

    public ATreeNode parseAndCreateNode(String string, ATreeNode treeParentNode) throws ParseException {
      Pattern pattern = Pattern.compile("([\\w+-=]*)(\\(.*\\))?");
      Matcher matcher = pattern.matcher(string);
      assertTrue(string, matcher.matches());
      String parent = matcher.group(1);
      TreeModelBridge<String> parentNode = new TreeModelBridge<String>(parent);
      String childrenGroup = matcher.group(2);
      if (childrenGroup == null)
        return parentNode;
      String[] children = childrenGroup.substring(1, childrenGroup.length() - 1).split(",");
      for (int i = 0; i < children.length; i++) {
        String child = children[i];
        parentNode.add(TreeModelBridge.create(child));
      }
      return parentNode;
    }

    public boolean isParseable(String string) {
      return true;
    }

    public boolean canExport(Collection<ATreeNode> nodes) {
      for (ATreeNode node : nodes) {
        if (node.getUserObject().toString().startsWith("-"))
          return false;
      }
      return true;
    }

    public boolean shouldReplaceOnPaste(ATreeNode oldNode, ATreeNode newNode) {
      return isReplace(oldNode);
    }

    private boolean isReplace(ATreeNode oldNode) {
      return oldNode.getUserObject().toString().startsWith("=");
    }

    public boolean shouldFlattenUnder(ATreeNode parent, ATreeNode node) {
      return node.getUserObject().toString().startsWith("+");
    }

    public boolean canImportUnder(ATreeNode parent, int insertIndex, String string, DragContext context) {
      return isReplace(parent) || (!parent.isLeaf() && !parent.getUserObject().toString().startsWith("#"));
    }

    public boolean canRemove(ATreeNode node) {
      return !node.getUserObject().toString().startsWith("!");
    }

    public ATreeNode createDefaultRoot() {
      return TreeModelBridge.create(DEFAULT_ROOT);
    }

    public void moveNode(ATreeNode child, ATreeNode parent, int index) {
      child.removeFromParent();
      parent.insert(child, index);
    }

    public int removeNode(ATreeNode node) {
      return  node.removeFromParent();
    }
  }
}
