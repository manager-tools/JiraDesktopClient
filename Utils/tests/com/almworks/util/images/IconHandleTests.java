package com.almworks.util.images;

import com.almworks.util.tests.BaseTestCase;

import javax.swing.*;

/**
 * @author Vasya
 */
public class IconHandleTests extends BaseTestCase {
  public void testSmallIcon() {
    IconHandle iconHandle = IconHandle.smallIcon(7777, "com/almworks/util/images/i7777.png");
    Icon empty = iconHandle.getEmpty();
    Icon grayed = iconHandle.getGrayed();
    assertNotNull(empty);
    assertNotNull(grayed);
    assertEquals(50, iconHandle.getIconWidth());
    assertEquals(32, iconHandle.getIconHeight());
    assertEquals(50, empty.getIconWidth());
    assertEquals(32, empty.getIconHeight());
    assertEquals(50, grayed.getIconWidth());
    assertEquals(32, grayed.getIconHeight());
  }

  public void testBadSmallIcon() {
    IconHandle iconHandle = IconHandle.smallIcon(1111);
    Icon empty = iconHandle.getEmpty();
    Icon grayed = iconHandle.getGrayed();
    assertNotNull(empty);
    assertNotNull(grayed);
    assertEquals(16, iconHandle.getIconWidth());
    assertEquals(16, iconHandle.getIconHeight());
    assertEquals(16, empty.getIconWidth());
    assertEquals(16, empty.getIconHeight());
    assertEquals(16, grayed.getIconWidth());
    assertEquals(16, grayed.getIconHeight());
  }

/*
  public void testIcon() {
    IconHandle iconHandle = IconHandle.icon(2222, 13, 11);
    Icon empty = iconHandle.getEmpty();
    Icon grayed = iconHandle.getGrayed();
    assertNotNull(empty);
    assertNotNull(grayed);
    assertEquals(13, iconHandle.getIconWidth());
    assertEquals(11, iconHandle.getIconHeight());
    assertEquals(13, empty.getIconWidth());
    assertEquals(11, empty.getIconHeight());
    assertEquals(13, grayed.getIconWidth());
    assertEquals(11, grayed.getIconHeight());
  }

  public void testMediumSmallIcon() {
    IconHandle iconHandle = IconHandle.mediumIcon(3333);
    Icon empty = iconHandle.getEmpty();
    Icon grayed = iconHandle.getGrayed();
    assertNotNull(empty);
    assertNotNull(grayed);
    assertEquals(32, iconHandle.getIconWidth());
    assertEquals(32, iconHandle.getIconHeight());
    assertEquals(32, empty.getIconWidth());
    assertEquals(32, empty.getIconHeight());
    assertEquals(32, grayed.getIconWidth());
    assertEquals(32, grayed.getIconHeight());
  }
*/

}
