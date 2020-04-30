package com.almworks.util;

import com.almworks.util.tests.BaseTestCase;

/**
 * @author Vasya
 */
public class PairTests extends BaseTestCase {
  public void testBaseContract() {
    testPair(null, null);
    testPair(null, "B");
    testPair("A", null);
    testPair("A", "B");

  }

  private void testPair(Object o1, Object o2) {
    Pair p = Pair.create(o1, o2);
    assertEquals(o1, p.getFirst());
    assertEquals(o2, p.getSecond());
    assertFalse(p.equals(null));
    assertEquals(p, Pair.create(o1, o2));
    System.out.println(p);
  }
}
