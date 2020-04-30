package com.almworks.util.ui.actions.globals;

import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;


/**
 * @author dyoma
 */
public class DescendantGlobalDataListenerTests extends GUITestCase {
  private final MyLogger myEvents = new MyLogger();
  private JPanel myRoot;

  protected void setUp() throws Exception {
    super.setUp();
    myRoot = createPanel("root");
    myRoot.addContainerListener(myEvents);
    myEvents.checkIsEmpty();
  }

  protected void tearDown() throws Exception {
    myRoot = null;
    super.tearDown();
  }

  public void testSubscriptionOne() {
    JPanel child = createPanel("child");
    myRoot.add(child);
    myEvents.checkAdded(child);
    JPanel leaf = createPanel("leaf");
    child.add(leaf);
    myEvents.checkAdded(leaf);
    myRoot.remove(child);
    myEvents.checkRemoved(child);
  }

  public void testSubscriptionMulti() {
    JPanel child = createPanel("child");
    JPanel subChild = createPanel("subChild");
    child.add(subChild);
    myRoot.add(child);
    myEvents.checkAdded(child);
    JPanel leaf = createPanel("leaf");
    subChild.add(leaf);
    myEvents.checkAdded(leaf);
    child.remove(subChild);
    myEvents.checkRemoved(subChild);
  }

  public void testRendererPane() {
    JPanel child = createPanel("child");
    CellRendererPane pane = new CellRendererPane();
    pane.setName("cellRendererPane");
    child.add(pane);
    myRoot.add(child);
    myEvents.checkAdded(child);
    JPanel renderer = createPanel("renderer");
    pane.add(renderer);
    myEvents.checkIsEmpty();
    pane.remove(renderer);
    myEvents.checkIsEmpty();
    child.remove(pane);
    myEvents.checkIsEmpty();

    child.add(pane);
    myEvents.checkIsEmpty();
    pane.add(renderer);
    myEvents.checkIsEmpty();
    myEvents.checkNotListening(pane);
  }

  private JPanel createPanel(String name) {
    JPanel panel = new JPanel();
    panel.setName(name);
    return panel;
  }

  private static class MyLogger extends DescendantGlobalDataListener {
    private final List<Container> myAdded = Collections15.arrayList();
    private final List<Container> myRemoved = Collections15.arrayList();
    private static final CollectionsCompare CHECK = new CollectionsCompare();

    protected void subTreeAdded(Container container) {
      myAdded.add(container);
    }

    protected void subTreeRemoved(Container container) {
      myRemoved.add(container);
    }

    public void checkIsEmpty() {
      assertTrue(isEmpty());
    }

    public boolean isEmpty() {
      return myAdded.isEmpty() && myRemoved.isEmpty();
    }

    public void checkAdded(Container ... containers) {
      CHECK.unordered(myAdded, containers);
      myAdded.clear();
      for (Container container : containers)
        CHECK.contains(this, container.getContainerListeners());
    }

    public void checkRemoved(Container ... containers) {
      CHECK.unordered(myRemoved, containers);
      myRemoved.clear();
      for (Container container : containers)
        checkNotListening(container);
    }

    public void checkNotListening(Container container) {
      assertFalse(container.getName(), Arrays.asList(container.getContainerListeners()).contains(this));
      Component[] children = container.getComponents();
      for (Component child : children) {
        if (child instanceof Container)
          checkNotListening((Container) child);
      }
    }
  }
}
