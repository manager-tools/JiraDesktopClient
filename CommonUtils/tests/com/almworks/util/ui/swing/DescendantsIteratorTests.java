package com.almworks.util.ui.swing;

import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Condition;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DescendantsIteratorTests extends GUITestCase {
  private final CollectionsCompare CHECK = CollectionsCompare.createForComponents();
  private final JPanel myRoot = createNode("root");
  private final JPanel myChild1 = createNode("child1");
  private final JLabel myLeaf1 = createLeaf("leaf1");
  private final JLabel myLeaf2 = createLeaf("leaf2");
  private final JPanel myChild2 = createNode("child2");
  private static final Condition<Object> JPANEL_ONLY = Condition.<Object>isInstance(JPanel.class);

  public void testEmptyContainer() {
    JPanel ancestor = createNode("root");
    DescendantsIterator it = DescendantsIterator.all(ancestor, true);
    checkAtEnd(it);
    
    it = DescendantsIterator.all(ancestor, false);
    assertTrue(it.hasNext());
    assertSame(ancestor, it.next());
    checkAtEnd(it);
    it.reset();
    assertTrue(it.hasNext());
    assertSame(ancestor, it.next());
    checkAtEnd(it);
  }

  public void testIteratesAll() {
    buildTree();

    checkOrder(DescendantsIterator.all(myRoot, false), myRoot, myChild1, myLeaf1, myLeaf2, myChild2);
    checkOrder(DescendantsIterator.all(myRoot, true), myChild1, myLeaf1, myLeaf2, myChild2);

    JLabel leaf21 = createLeaf("leaf2_1");
    myChild2.add(leaf21);

    checkOrder(DescendantsIterator.all(myRoot, true), myChild1, myLeaf1, myLeaf2, myChild2, leaf21);
  }

  public void testInstanceFilter() {
    buildTree();
    JLabel leaf21 = createLeaf("leaf21");
    myChild2.add(leaf21);

    checkOrder(DescendantsIterator.instances(myRoot, false, JLabel.class), myLeaf1, myLeaf2, leaf21);

    JLabel root2 = new JLabel();
    root2.add(myChild1);
    root2.add(myChild2);

    checkOrder(DescendantsIterator.instances(root2, false, JLabel.class), root2, myLeaf1, myLeaf2, leaf21);
    checkOrder(DescendantsIterator.instances(root2, true, JLabel.class), myLeaf1, myLeaf2, leaf21);
  }

  public void testTreeFilter() {
    buildTree();
    JLabel child3 = new JLabel();
    myRoot.add(child3);
    JLabel leaf31 = createLeaf("leaf31");
    child3.add(leaf31);
    child3.add(myChild2);
    JLabel leaf21 = createLeaf("leaf21");
    myChild2.add(leaf21);

    checkOrder(DescendantsIterator.all(myRoot, false),
      myRoot, myChild1, myLeaf1, myLeaf2, child3, leaf31, myChild2, leaf21);
    checkOrder(DescendantsIterator.skipDescendants(myRoot, false, JPANEL_ONLY),
      myRoot, myChild1, myLeaf1, myLeaf2, child3);

    JLabel root2 = createLeaf("roo2");
    root2.add(myChild1);
    root2.add(child3);
    checkOrder(DescendantsIterator.skipDescendants(root2, false, JPANEL_ONLY), root2);
    checkOrder(DescendantsIterator.skipDescendants(root2, true, JPANEL_ONLY));
  }

  public void testCustom() {
    buildTree();
    JLabel child3 = createLeaf("child3");
    myRoot.add(child3);
    JLabel leaf31 = createLeaf("leaf31");
    child3.add(leaf31);

    checkOrder(DescendantsIterator.all(myRoot, false), myRoot, myChild1, myLeaf1, myLeaf2, myChild2, child3, leaf31);
    checkOrder(DescendantsIterator.create(myRoot, false, JPANEL_ONLY, JLabel.class), myLeaf1, myLeaf2, child3);
  }

  private void buildTree() {
    myRoot.add(myChild1);
    myChild1.add(myLeaf1);
    myChild1.add(myLeaf2);
    myRoot.add(myChild2);
  }

  private JLabel createLeaf(String name) {
    JLabel leaf = new JLabel();
    leaf.setName(name);
    return leaf;
  }

  private JPanel createNode(String name) {
    JPanel node = new JPanel();
    node.setName(name);
    return node;
  }

  private void checkOrder(DescendantsIterator<?> it, Component ... expected) {
    CHECK.order(expected, Containers.collectList(it));
    checkAtEnd(it);
    it.reset();
    CHECK.order(expected, Containers.collectList(it));
  }

  private void checkAtEnd(Iterator<?> it) {
    assertFalse(it.hasNext());
    Object o;
    try {
      o = it.next();
    } catch (NoSuchElementException e) {
      return;
    }
    fail(it + " has an element: " + o);
  }
}
