package com.almworks.util.collections;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Const;

/**
 * @author dyoma
 */
public class ObjectArrayTest extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final ObjectArray<String> myArray = new ObjectArray<String>();

  public void testInsert() {
    assertEquals(0, myArray.size());
    myArray.insertRange(0, 5, "a");
    assertEquals(5, myArray.size());
    CHECK.order(toArray(), new String[] {"a", "a", "a", "a", "a"});
    myArray.set(3, "b");
    myArray.set(0, "c");
    myArray.set(4, "x");
    CHECK.order(toArray(), new String[] {"c", "a", "a", "b", "x"});
    myArray.removeRange(1, 2);
    CHECK.order(toArray(), new String[] {"c", "b", "x"});
    checkNullTail();
    myArray.insertRange(1, 2, "d");
    CHECK.order(toArray(), new String[] {"c", "d", "d", "b", "x"});
    myArray.add("y");
    CHECK.order(toArray(), new String[] {"c", "d", "d", "b", "x", "y"});
    myArray.removeTail(2);
    CHECK.order(toArray(), new String[] {"c", "d"});
  }

  private String[] toArray() {
    return myArray.toArray(Const.EMPTY_STRINGS);
  }

  private void checkNullTail() {
    for (int i = myArray.size(); i < myArray.capacity(); i++)
      assertNull(String.valueOf(i), myArray.priGet(i));
  }

  public void testUtils() {
    myArray.add("a");
    myArray.insertRange(1, 4, "b");
    myArray.add("c");
    CHECK.order(toArray(), new String[] {"a", "b", "b", "b", "b", "c"});
    assertEquals(10, myArray.capacity());
    myArray.insertRange(6, 4, "x");
    assertEquals(myArray.size(), myArray.capacity());
    CHECK.order(toArray(), new String[] {"a", "b", "b", "b", "b", "c", "x", "x", "x", "x"});
    myArray.removeTail(3);
    CHECK.order(toArray(), new String[] {"a", "b", "b"});
    checkNullTail();
    myArray.removeTail(3);
    CHECK.order(toArray(), new String[] {"a", "b", "b"});
    checkNullTail();
  }
}
