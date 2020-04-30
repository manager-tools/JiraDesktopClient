package com.almworks.util.ui;

import com.almworks.util.tests.BaseTestCase;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public class InlineLayoutTests extends BaseTestCase {
  public static final Dimension PREF = new Dimension(200, 20);
  public static final Dimension MIN = new Dimension(100, 10);
  public static final Dimension MAX = new Dimension(300, 30);
  private final InlineLayout myLayout = new InlineLayout(InlineLayout.VERTICAL);
  private final JPanel myParent = new JPanel(myLayout);
  private final JPanel myChild1 = createChild();
  private final JPanel myChild2 = createChild();

  protected void setUp() throws Exception {
    super.setUp();
    myParent.add(myChild1);
    myParent.add(myChild2);
  }

  public void testPreferedSize() {
    assertEquals(200, myParent.getPreferredSize().width);
    assertEquals(40, myParent.getPreferredSize().height);

    myParent.setSize(200, 40);
    myLayout.layoutContainer(myParent);
    assertEquals(new Rectangle(0, 0, 200, 20), myChild1.getBounds());
    assertEquals(new Rectangle(0, 20, 200, 20), myChild2.getBounds());
  }

  public void testMaxSize() {
    myChild1.setMaximumSize(new Dimension(10, Integer.MAX_VALUE / 2 + 3));
    myChild2.setMaximumSize(new Dimension(10, Integer.MAX_VALUE / 2 + 3));
    assertTrue((long) myChild1.getMaximumSize().height + (long) myChild2.getMaximumSize().height > Integer.MAX_VALUE);
    assertEquals(new Dimension(10, Integer.MAX_VALUE), myParent.getMaximumSize());
  }

  public void testBelowPrefered() {
    myParent.setSize(150, 30);
    myLayout.layoutContainer(myParent);
    assertEquals(new Rectangle(0, 0, 150, 15), myChild1.getBounds());
    assertEquals(new Rectangle(0, 15, 150, 15), myChild2.getBounds());
  }

  public void testBelowMin() {
    myParent.setSize(150, 19);
    myLayout.layoutContainer(myParent);
    assertEquals(new Rectangle(0, 0, 150, 10), myChild1.getBounds());
    assertEquals(new Rectangle(0, 10, 150, 10), myChild2.getBounds());
  }

  public void testInvisibleComponent() {
    assertEquals(new Dimension(200, 40), myParent.getPreferredSize());
    assertEquals(new Dimension(100, 20), myParent.getMinimumSize());
    assertEquals(new Dimension(300, 60), myParent.getMaximumSize());
    myParent.setSize(200, 40);
    myLayout.layoutContainer(myParent);
    assertEquals(new Rectangle(0, 0, 200, 20), myChild1.getBounds());
    assertEquals(new Rectangle(0, 20, 200, 20), myChild2.getBounds());

    myChild1.setVisible(false);
    assertEquals(new Dimension(200, 20), myParent.getPreferredSize());
    assertEquals(new Dimension(100, 10), myParent.getMinimumSize());
    assertEquals(new Dimension(300, 30), myParent.getMaximumSize());
    myParent.setSize(200, 20);
    myLayout.layoutContainer(myParent);
    assertEquals(new Rectangle(0, 0, 200, 20), myChild2.getBounds());

    myChild2.setVisible(false);
    assertEquals(new Dimension(0, 0), myParent.getPreferredSize());
    assertEquals(new Dimension(0, 0), myParent.getMinimumSize());
    assertEquals(new Dimension(0, 0), myParent.getMaximumSize());
  }

  private JPanel createChild() {
    JPanel child = new JPanel();
    child.setPreferredSize(PREF);
    child.setMinimumSize(MIN);
    child.setMaximumSize(MAX);
    return child;
  }
}
