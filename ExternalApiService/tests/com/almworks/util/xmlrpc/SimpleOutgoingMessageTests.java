package com.almworks.util.xmlrpc;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;

import java.util.Collection;

public class SimpleOutgoingMessageTests extends BaseTestCase {
  public void testEqualsHashCode() {
    SimpleOutgoingMessage m1 = new SimpleOutgoingMessage("test", 1, "c", 3);
    SimpleOutgoingMessage m2 = new SimpleOutgoingMessage("test", 1, "c", 3);
    SimpleOutgoingMessage m3 = new SimpleOutgoingMessage("test", 1, "c");
    SimpleOutgoingMessage m4 = new SimpleOutgoingMessage("test1", 1, "c", "X");
    assertEquals(m1, m2);
    assertNotSame(m1, m3);
    assertNotSame(m1, m4);
    assertNotSame(m3, m4);
  }

  public void testVector() {
    CollectionsCompare compare = new CollectionsCompare();
    SimpleOutgoingMessage m1 = new SimpleOutgoingMessage("test", 1, "c", 3);
    compare.order(new Object[] {1, "c", 3}, m1.getRpcParameters().iterator());

    Collection<?> empty = new SimpleOutgoingMessage("xxx").getRpcParameters();
    assertNotNull(empty);
    assertEquals(0, empty.size());
  }
}
