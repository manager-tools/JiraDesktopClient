package com.almworks.util.components;

import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.ui.UIComponentWrapper;

import javax.swing.*;
import java.awt.*;

/**
 * @author Vasya
 */
public class PlaceHolderTests extends BaseTestCase {
  private PlaceHolder placeHolder;
  private JPanel myComponent;
  private final Dimension EMPTY_DIMENSION = new Dimension();

  public PlaceHolderTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    placeHolder = new PlaceHolder();
    myComponent = new JPanel();
  }

  public void testNullComponent() {
    placeHolder.show((JComponent) null);
    assertEquals(EMPTY_DIMENSION, placeHolder.getPreferredSize());
  }

  public void testNullWrapper() {
    placeHolder.show((UIComponentWrapper) null);
    assertEquals(EMPTY_DIMENSION, placeHolder.getPreferredSize());
  }

  public void testClear() {
    placeHolder.clear();
    assertEquals(EMPTY_DIMENSION, placeHolder.getPreferredSize());
  }

  public void testNormal() {
    Dimension preferredSize = new Dimension(10, 20);
    myComponent.setPreferredSize(preferredSize);
    placeHolder.show(myComponent);
    assertEquals(preferredSize, placeHolder.getPreferredSize());
  }

  protected void tearDown() throws Exception {
    placeHolder = null;
    myComponent = null;
  }
}
