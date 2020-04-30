package com.almworks.util.ui;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ContainerDescendantsWatcherTests extends BaseTestCase {
  private static final CollectionsCompare CHECK = CollectionsCompare.createForComponents();
  private final JPanel myRoot = createNode("root");
  private final JPanel myChild = createNode("child");
  private final SampleWatcher myWatcher = new SampleWatcher();

  protected void setUp() throws Exception {
    super.setUp();
    myRoot.add(myChild);
  }

  public void testNotifyWatched() {
    myWatcher.watchSubTree(myRoot);
    myWatcher.checkWatched(myRoot, myChild);

    JPanel child2 = createNode("child2");
    myChild.add(child2);
    myWatcher.checkWatched(child2);

    JLabel leaf = createLeaf("leaf");
    child2.add(leaf);
    myWatcher.checkWatched(leaf);

    JPanel leafChild = createNode("leafChild");
    leaf.add(leafChild);
    myWatcher.checkWatched();
  }

  public void testNotifyStopWatch() {
    myWatcher.watchSubTree(myRoot);
    JLabel leaf1 = createLeaf("leaf1");
    myChild.add(leaf1);
    JLabel leaf2 = createLeaf("leaf2");
    myRoot.add(leaf2);
    leaf2.add(createNode("lead2Child"));
    leaf1.add(createNode("leaf1Child"));

    myRoot.remove(leaf2);
    myWatcher.checkStopped(leaf2);

    myRoot.remove(myChild);
    myWatcher.checkStopped(myChild, leaf1);
  }

  private static JPanel createNode(String name) {
    JPanel node = new JPanel();
    node.setName(name);
    return node;
  }

  private JLabel createLeaf(String name) {
    JLabel leaf = new JLabel();
    leaf.setName(name);
    return leaf;
  }

  private static class SampleWatcher extends ContainerDescendantsWatcher {
    private final List<Container> myWatched = Collections15.arrayList();
    private final List<Object> myStopped = Collections15.arrayList();

    protected void onStartWatchDescendant(Container descendant) {
      myWatched.add(descendant);
    }

    protected void onStopWatchDescendant(Container descentant) {
      myStopped.add(descentant);
    }

    protected boolean shouldWatchDescendants(Container container) {
      assert (container instanceof JPanel) || (container instanceof JLabel);
      return container instanceof JPanel;
    }

    public void checkWatched(Container ... expected) {
      CHECK.unordered(myWatched, expected);
      myWatched.clear();
    }

    public void checkStopped(Container ... expected) {
      CHECK.unordered(myStopped, expected);
      myStopped.clear();
    }
  }
}
