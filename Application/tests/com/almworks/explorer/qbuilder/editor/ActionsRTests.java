package com.almworks.explorer.qbuilder.editor;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.ConstraintDescriptorProxy;
import com.almworks.api.application.qb.ConstraintEditorNodeImpl;
import com.almworks.api.application.qb.EditorNode;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.explorer.loader.DBThreadConstraints;
import com.almworks.explorer.qbuilder.EditorGroupNode;
import com.almworks.explorer.qbuilder.filter.FilterGramma;
import com.almworks.explorer.qbuilder.filter.TextAttribute;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.AList;
import com.almworks.util.components.ATree;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.config.Configuration;
import com.almworks.util.i18n.Local;
import com.almworks.util.i18n.TestTextProvider;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.MockProxy;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.ActionBridge;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.dnd.TransferAction;
import com.almworks.util.ui.swing.DescendantsIterator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author : Dyoma
 */
@SuppressWarnings({"ConstantConditions"})
public class ActionsRTests extends BaseTestCase {
  private ActionBridge myAnd;
  private ActionBridge myOr;
  private ActionBridge myRemove;
  private TreeModelBridge<EditorNode> myRoot;
  private ATree<TreeModelBridge<EditorNode>> myTree;
  private CollectionsCompare CHECK = new CollectionsCompare();
  private ActionBridge myNegate;
  private static final TestTextProvider PROVIDER = new TestTextProvider();

  static {
    PROVIDER.put(Terms.key_Artifacts, "Artifacts");
  }

  public ActionsRTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    Local.getBook().installProvider(PROVIDER);
    MockProxy<NameResolver> resolver = MockProxy.create(NameResolver.class, "resolver");
    OrderListModel<ConstraintDescriptor> descriptors = OrderListModel.create();
    descriptors.addElement(ConstraintDescriptorProxy.stub("attribute", TextAttribute.INSTANCE));
    resolver.method("getConstraintDescriptorModel").returning(descriptors).ignore();
    QueryEditorContext context = new QueryEditorContext(resolver.getObject(), new ItemHypercubeImpl());
    myTree = FilterEditorImpl.createQueryTree(new QBTransferService(context));
    myAnd = createAction(FilterEditorImpl.AND_ACTION);
    myOr = createAction(FilterEditorImpl.OR_ACTION);
    myNegate = createAction(new FilterEditorImpl.NegateAction());
    myRemove = new ActionBridge(TransferAction.REMOVE, myTree);

    context.registerDataProvider(myTree);
    EditorNode editorNode = FilterGramma.createEmpty().createEditorNode(context);
    myRoot = FilterEditorImpl.resetQueryTree(myTree, editorNode);
    setAnyConstraint(editorNode);
    myAnd.startUpdate();
    myOr.startUpdate();
    myNegate.startUpdate();
    myRemove.startUpdate();
  }

  protected void tearDown() throws Exception {
    Local.getBook().installProvider(PROVIDER);
    super.tearDown();
  }

  public void testSameOperation() throws CantPerformException {
    TreeModelBridge<? extends EditorNode> leaf = myRoot.getChildAt(0);
    createGroup(myAnd);
    TreeModelBridge<? extends EditorNode> and = myRoot.getChildAt(0);
    checkGroupId(EditorGroupNode.AND_GROUP, and);
    assertSame(leaf, and.getChildAt(0));
    TreeModelBridge<? extends EditorNode> leaf2 = and.getChildAt(1);
    assertSame(leaf2, getSelectedItem());
    assertTrue(myTree.isExpanded(and));
    checkAllActionsEnabled();
    createGroup(myAnd);
    TreeModelBridge<? extends EditorNode> leaf3 = and.getChildAt(2);
    assertSame(leaf3, getSelectedItem());
    checkAllActionsEnabled();
    setSelectedItem(and);
    checkAllActionsEnabled();
    createGroup(myAnd);
    assertSame(and, myRoot.getChildAt(0));
    assertEquals(4, and.getChildCount());
    CHECK.order(new TreeModelBridge[]{leaf, leaf2, leaf3, getSelectedItem()}, and.childrenToList());
  }

  public void testDifferentOperations() {
    createGroup(myAnd);
    checkAllActionsEnabled();
    TreeModelBridge<EditorNode> leaf2 = getSelectedItem();
    TreeModelBridge<? extends EditorNode> and = leaf2.getParent();
    createGroup(myOr);
    checkAllActionsEnabled();
    TreeModelBridge<? extends EditorNode> or = and.getChildAt(1);
    assertSame(or, leaf2.getParent());
    TreeModelBridge<? extends EditorNode> leaf3 = or.getChildAt(1);
    assertSame(leaf3, getSelectedItem());
  }

  public void testRemoveSingleChild() {
    TreeModelBridge<EditorNode> leaf = getSelectedItem();
    createGroup(myAnd);
    TreeModelBridge<? extends EditorNode> and = leaf.getParent();
    checkAllActionsEnabled();
    assertSame(and.getChildAt(0), leaf);
    assertSame(and.getChildAt(1), getSelectedItem());
    perform(myRemove);
    assertFalse(and.isAttachedToModel());
    assertNull(leaf.getParent().getParent());
    assertEquals(1, leaf.getParent().getChildCount());
  }

  public void testRemoveSingleChild2() {
    TreeModelBridge<EditorNode> leaf = getSelectedItem();
    createGroup(myAnd);
    TreeModelBridge<EditorNode> toRemove = getSelectedItem();
    TreeModelBridge<? extends EditorNode> and = leaf.getParent();
    setSelectedItem(leaf);
    createGroup(myOr);
    TreeModelBridge<? extends EditorNode> or = getSelectedItem().getParent();
    assertEquals(2, or.getChildCount());
    checkGroupId(EditorGroupNode.OR_GROUP, or);
    setSelectedItem(toRemove);
    ActionBridge.updateActionsNow();
    assertTrue(myRemove.isEnabled());
    assertTrue(myTree.isExpanded(or));
    perform(myRemove);
    assertTrue(myTree.isExpanded(myTree.getRoot()));
    assertTrue(myTree.isExpanded(or));
    assertTrue(or.isAttachedToModel());
    assertFalse(and.isAttachedToModel());
    assertNull(or.getParent().getParent());
  }

  public void testRemoveTwoChildren() {
    createGroup(myAnd);
    TreeModelBridge<? extends EditorNode> and = getSelectedItem().getParent();
    setSelectedItem(and.getChildAt(0));
    createGroup(myOr);
    TreeModelBridge<EditorNode> leaf1r = getSelectedItem();
    setSelectedItem(and.getChildAt(1));
    createGroup(myOr);
    TreeModelBridge<EditorNode> leaf2r = getSelectedItem();
    assertNotSame(leaf1r.getParent(), leaf2r.getParent());
    assertSame(and, leaf1r.getParent().getParent());
    assertSame(and, leaf2r.getParent().getParent());

    myTree.getSelectionAccessor().setSelected(new TreeModelBridge[]{leaf1r, leaf2r});
    ActionBridge.updateActionsNow();
    assertTrue(myRemove.isEnabled());

    perform(myRemove);
    assertTrue(and.isAttachedToModel());
    assertFalse(leaf1r.isAttachedToModel());
    assertFalse(leaf2r.isAttachedToModel());
    assertEquals(2, and.getChildCount());
  }

  public void testSelectionAfterRemoveLast() {
    createGroup(myAnd);
    createGroup(myAnd);
    TreeModelBridge<? extends EditorNode> and = getSelectedItem().getParent();
    assertEquals(3, and.getChildCount());
    setSelectedItem(and.getChildAt(2));
    perform(myRemove);
    assertSame(and.getChildAt(1), getSelectedItem());
    TreeModelBridge<? extends EditorNode> last = and.getChildAt(0);
    perform(myRemove);
    assertSame(last, getSelectedItem());
  }

  public void testSimpleNegate() {
    TreeModelBridge<EditorNode> leaf = getSelectedItem();
    perform(myNegate);
    assertSame(leaf, getSelectedItem());
    TreeModelBridge<? extends EditorNode> not = leaf.getParent();
    checkGroupId(EditorGroupNode.NEITHER_GROUP, not);
    assertSame(myRoot, not.getParent());
    assertEquals(1, myRoot.getChildCount());
    assertEquals(1, not.getChildCount());
  }

  public void testNor() {
    TreeModelBridge<EditorNode> leaf = getSelectedItem();
    perform(myNegate);
    checkAllActionsEnabled();
    assertEquals("NOR", myOr.getPresentation().getValue(Action.NAME));
    perform(myOr);
    TreeModelBridge<EditorNode> newLeaf = getSelectedItem();
    TreeModelBridge<? extends EditorNode> neighter = leaf.getParent();
    assertSame(EditorGroupNode.NEITHER_GROUP, EditorGroupNode.getGroupId(neighter));
    CHECK.order(new TreeModelBridge[] {leaf, newLeaf}, neighter.childrenToList());
    setSelectedItem(neighter);
    ActionBridge.updateActionsNow();
    assertEquals("OR", myOr.getPresentation().getValue(Action.NAME));
    perform(myOr);
    TreeModelBridge<? extends EditorNode> or = getSelectedItem().getParent();
    CHECK.singleElement(or, myRoot.childrenToList());
    CHECK.order(new TreeModelBridge[] {neighter, getSelectedItem()}, or.childrenToList());
  }

  public void testNegateNot() {
    TreeModelBridge<EditorNode> leaf = getSelectedItem();
    perform(myNegate);
    setSelectedItem(leaf.getParent());
    checkAllActionsEnabled();
    perform(myNegate);
    assertSame(leaf, getSelectedItem());
    CHECK.singleElement(leaf, myRoot.childrenToList());
    CHECK.empty(leaf.childrenToList());
  }

  public void testNegatingTwice() {
    TreeModelBridge<EditorNode> leaf = getSelectedItem();
    perform(myNegate);
    assertSame(leaf, getSelectedItem());
    perform(myNegate);
    assertSame(leaf, getSelectedItem());
    CHECK.singleElement(leaf, myRoot.childrenToList());
  }

  public void testNegateAnd() {
    createGroup(myAnd);
    TreeModelBridge<? extends EditorNode> and = getSelectedItem().getParent();
    CHECK.singleElement(and, myRoot.childrenToList());
    setSelectedItem(and);
    checkAllActionsEnabled();
    perform(myNegate);
    assertSame(and, getSelectedItem());
    TreeModelBridge<? extends EditorNode> not = and.getParent();
    assertSame(myRoot, not.getParent());
    CHECK.singleElement(not, myRoot.childrenToList());
    CHECK.singleElement(and, not.childrenToList());
    setSelectedItem(not);
    perform(myNegate);
    CHECK.singleElement(and, myRoot.childrenToList());
  }

  public void testNegateOr() {
    TreeModelBridge<EditorNode> leaf = getSelectedItem();
    createGroup(myOr);
    TreeModelBridge<EditorNode> leaf2 = getSelectedItem();
    TreeModelBridge<? extends EditorNode> or = leaf2.getParent();
    CHECK.order(new TreeModelBridge[] {leaf, leaf2}, or.childrenToList());
    CHECK.singleElement(or, myRoot.childrenToList());
    setSelectedItem(or);
    perform(myNegate);
    TreeModelBridge<EditorNode> neither = getSelectedItem();
    checkGroupId(EditorGroupNode.NEITHER_GROUP, neither);
    CHECK.singleElement(neither, myRoot.childrenToList());
    CHECK.order(new TreeModelBridge[] {leaf, leaf2}, neither.childrenToList());
    checkGroupId(EditorGroupNode.NEITHER_GROUP, neither);
  }

  public void testNegateNeither() {
    createGroup(myOr);
    setSelectedItem(myRoot.getChildAt(0));
    perform(myNegate);
    perform(myNegate);
    TreeModelBridge<EditorNode> or = getSelectedItem();
    assertSame(myRoot.getChildAt(0), or);
    checkGroupId(EditorGroupNode.OR_GROUP, or);
  }

  public void testRemoveNeitherChildren() {
    createGroup(myOr);
    perform(myNegate);
    perform(myOr);
    TreeModelBridge<EditorNode> leaf = getSelectedItem();
    CHECK.size(2, leaf.getParent().childrenToList());
    perform(myRemove);
    leaf = getSelectedItem();
    TreeModelBridge<EditorNode> not = leaf.getParent();
    checkGroupId(EditorGroupNode.NEITHER_GROUP, not);
    TreeModelBridge<EditorNode> or = not.getParent();
    checkGroupId(EditorGroupNode.OR_GROUP, or);
    CHECK.singleElement(or, myRoot.childrenToList());

    perform(myRemove);
    leaf = myRoot.getChildAt(0);
    CHECK.singleElement(leaf, myRoot.childrenToList());
    CHECK.size(0, leaf.childrenToList());
  }

  public void testNegateNotSingleChild() {
    perform(myNegate);
    TreeModelBridge<EditorNode> neither = getSelectedItem().getParent();
    checkGroupId(EditorGroupNode.NEITHER_GROUP, neither);
    perform(myOr);
    CHECK.size(2, neither.childrenToList());
    assertTrue(neither.isAttachedToModel());
    perform(myNegate);
    assertTrue(neither.isAttachedToModel());
    TreeModelBridge<EditorNode> leaf = getSelectedItem();
    CHECK.empty(leaf.childrenToList());
    TreeModelBridge<EditorNode> not = leaf.getParent();
    checkGroupId(EditorGroupNode.NEITHER_GROUP, not);
    assertSame(neither, not.getParent());
    CHECK.size(2, neither.childrenToList());
  }

  public void testRemoveNegationSibling() {
    perform(myNegate);
    TreeModelBridge<EditorNode> toRemove = getSelectedItem();
    perform(myAnd);
    assertNotSame(toRemove, getSelectedItem());
    perform(myNegate);
    TreeModelBridge<EditorNode> leaf = getSelectedItem();
    setSelectedItem(toRemove);
    perform(myRemove);
    setSelectedItem(leaf);
    perform(myRemove);
    TreeModelBridge<EditorNode> selection = getSelectedItem();
    assertNotNull(selection);
    TreeModelBridge<EditorNode> root = selection.getParent();
    assertNull(root.getParent());
    CHECK.singleElement(selection, root.childrenToList());
    CHECK.empty(selection.childrenToList());
  }

  private void checkGroupId(Object groupId, TreeModelBridge<? extends EditorNode> treeNode) {
    assertSame(groupId, EditorGroupNode.getGroupId(treeNode));
  }

  private void checkOperationsEnabled() {
    ActionBridge.updateActionsNow();
    TreeModelBridge<EditorNode> selection = myTree.getSelectionAccessor().getSelection();
    assertNotNull(selection);
    if (selection.getChildCount() != 0)
      assertTrue(myTree.isExpanded(selection));
    assertTrue(myAnd.isEnabled());
    assertTrue(myOr.isEnabled());
    assertTrue(myNegate.isEnabled());
  }

  private void checkAllActionsEnabled() {
    checkOperationsEnabled();
    assertTrue(myRemove.isEnabled());
  }

  private void perform(ActionBridge action) {
    action.getPresentation().actionPerformed(new ActionEvent(myTree, 0, null));
    ActionBridge.updateActionsNow();
  }

  private void createGroup(ActionBridge action) {
    ActionBridge.updateActionsNow();
    assertTrue(action.isEnabled());
    perform(action);
    List<TreeModelBridge<EditorNode>> items = myTree.getSelectionAccessor().getSelectedItems();
    assert items.size() == 1 : "Otherwise don't know what to do";
    TreeModelBridge<EditorNode> newNode = items.get(0);
    EditorNode editorNode = newNode.getUserObject();
    if (editorNode instanceof ConstraintEditorNodeImpl)
      setAnyConstraint(editorNode);
  }

  private void setAnyConstraint(EditorNode editorNode) {
    UIComponentWrapper editor = editorNode.createEditor(Configuration.EMPTY_CONFIGURATION);
    assertNotNull(editor);
    Collection<JComponent> descendants = Containers.collectList(DescendantsIterator.create(editor.getComponent(), false,
      DescendantsIterator.IS_JCOMPONENT, JComponent.class));
    Set<AList<?>> lists = (Set) Condition.<Object>isInstance(AList.class).selectSet(descendants);
    assert lists.size() == 1 : "Try select visible only";
    AList<?> constraints = lists.iterator().next();
    assert constraints.getCollectionModel().getSize() > 0;
    constraints.getSelectionAccessor().ensureSelectionExists();
  }

  private TreeModelBridge<EditorNode> getSelectedItem() {
    return myTree.getSelectionAccessor().getSelection();
  }

  private void setSelectedItem(TreeModelBridge<? extends EditorNode> node) {
    myTree.getSelectionAccessor().setSelected((TreeModelBridge)node);
    assertSame(node, getSelectedItem());
    assertEquals(1, myTree.getSelectionAccessor().getSelectedItems().size());
  }

  private ActionBridge createAction(AnAction action) {
    return new ActionBridge(action, myTree);
  }

  static {
    DBThreadConstraints.register();
  }
}
