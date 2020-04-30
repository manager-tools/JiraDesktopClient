package com.almworks.util.ui;

import junit.framework.TestCase;

import static com.almworks.util.ui.TextImproveUtil.findWordEnd;
import static com.almworks.util.ui.TextImproveUtil.findWordStart;

/**
 *
 */
public class ImproveUnitTest extends TestCase {
  private String testString1 = "one two three";
  public void testFindStartEndWordTest() {
    assertEquals(findWordStart(testString1, 3), 0);
    assertEquals(findWordStart(testString1, 7), 3); // remove whitespace

    assertEquals(findWordEnd(testString1, 0), 4);
    assertEquals(findWordEnd(testString1, 8), 13);
  }
}
