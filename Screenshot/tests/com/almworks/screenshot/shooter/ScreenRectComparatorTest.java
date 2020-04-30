package com.almworks.screenshot.shooter;

import com.almworks.util.tests.BaseTestCase;

import java.awt.*;
import java.util.Comparator;

/**
 * @author dyoma
 */
public class ScreenRectComparatorTest extends BaseTestCase {
  public void testYStrict() {
    Rectangle r0_100 = new Rectangle(0, 0, 100, 100);
    Rectangle r50_150 = new Rectangle(50, 50, 100, 100);
    Comparator<Rectangle> comparator = ShootDialog.BY_Y_STRICT;
    checkEqual(comparator, r0_100, r50_150);
    Rectangle r100_100 = new Rectangle(100, 100, 100, 100);
    checkOrder(comparator, r0_100, r100_100);
  }

  public void testScreenComparator() {
    Rectangle r11 = new Rectangle(0, 0, 100, 100);
    Rectangle r12 = new Rectangle(100, 10, 100, 50);
    Rectangle r21 = new Rectangle(0, 100, 100, 50);
    Comparator<Rectangle> comparator = ShootDialog.SCREEN_RECT;
    checkOrder(comparator, r11, r12);
    checkOrder(comparator, r12, r21);
    checkOrder(comparator, r11, r21);
  }

  private void checkOrder(Comparator<Rectangle> comparator, Rectangle small, Rectangle big) {
    assertEquals(-1, comparator.compare(small, big));
    assertEquals(1, comparator.compare(big, small));
  }

  private void checkEqual(Comparator<Rectangle> comparator, Rectangle r1, Rectangle r2) {
    assertEquals(0, comparator.compare(r1, r2));
    assertEquals(0, comparator.compare(r2, r1));
  }
}
