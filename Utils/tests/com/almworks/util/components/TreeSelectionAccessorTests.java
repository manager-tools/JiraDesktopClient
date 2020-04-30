package com.almworks.util.components;

import com.almworks.util.collections.ChangeCounter;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author dyoma
 */
public class TreeSelectionAccessorTests extends BaseTestCase {
  public TreeSelectionAccessorTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  public void testSelectionUpdated() {
    JTree tree = new JTree();
    TreeSelectionAccessor accessor = new TreeSelectionAccessor(tree);
    DefaultTreeModel model = TreeModelBridge.create("xxx").becomeRoot();
    TreeModelBridge<String> root = TreeModelBridge.create("root");
    tree.setModel(model);
    TreeModelBridge<String> child = TreeModelBridge.create("child");
    root.add(child);
    root.replaceRoot(model);

    accessor.setSelected(child);
    ChangeCounter counter = new ChangeCounter();
    accessor.addSelectedItemsListener(counter);
    root.fireChanged();
    assertEquals(0, counter.getCount());
    child.fireChanged();
    assertEquals(1, counter.getCount());

    accessor.setSelected(root);
    counter.reset();
    root.fireChanged();
    assertEquals(1, counter.getCount());
    child.fireChanged();
    assertEquals(1, counter.getCount());
  }
}
