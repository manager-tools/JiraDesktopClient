package com.almworks.util.collections;

import com.almworks.util.tests.BaseTestCase;

/**
 * @author dyoma
 */
public class IntLastTouchedQueueTests extends BaseTestCase {
  public void testTouch() {
    IntLastTouchedQueue q = new IntLastTouchedQueue(3);
    assertNull(q.touch(1));
    assertNull(q.touch(2));
    assertNull(q.touch(3));
    assertEquals(1, q.touch(4).intValue());
    assertNull(q.touch(2));
    assertEquals(3, q.touch(5).intValue());
  }

  public void testRemove() {
    IntLastTouchedQueue q = new IntLastTouchedQueue(3);
    assertFalse(q.remove(1));
    assertNull(q.touch(1));
    assertFalse(q.remove(2));
    assertNull(q.touch(2));
    assertTrue(q.remove(1));
    assertNull(q.touch(3));
    assertNull(q.touch(4));
    assertNull(q.touch(2));
    assertEquals(3, q.touch(5).intValue());
    assertFalse(q.remove(3));
    assertFalse(q.remove(10));
    assertTrue(q.remove(5));
    assertNull(q.touch(6));
  }
}
