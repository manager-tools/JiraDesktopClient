package com.almworks.util.collections.arrays;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.almworks.util.Collections15.arrayList;

public class ObjectArrayAccessorTests extends BaseTestCase {
  private final static ObjectArrayAccessor acc = ObjectArrayAccessor.INSTANCE;
  private final static CollectionsCompare ord = new CollectionsCompare();

  public void testSetObjectValue() throws Exception {
    checkSet(1, 2, 3, 4, 5);
    checkSet(null, 2, 3, 4, 5);
    checkSet(null, 2, null, 4, 5);
    checkSet(1, 2, 3, null, 5);
    checkSet(1, 2, 3, 4, null);
    checkSet(1, 2, null, null, 5);
    checkSet(null, 2, null, 4, null);
  }

  private static void checkSet(Object... elem) {
    Object a1 = null;
    List<Integer> ind2 = arrayList();
    for (int i = 0; i < elem.length; ++i) {
      a1 = acc.setObjectValue(a1, i, elem[i]);
      ind2.add(i);
    }
    Object a2 = null;
    Collections.shuffle(ind2);
    for (Integer i : ind2) {
      a2 = acc.setObjectValue(a2, i, elem[i]);
    }
    for (int i = 0; i < elem.length; ++i) {
      assertEquals(Arrays.toString(elem) + " [" + i + "]", acc.getObjectValue(a1, i), acc.getObjectValue(a2, i));
    }
  }

  public void testShiftRight() {
    Object s = fillStorage(1, 2, 3);
    checkStorage(acc.shiftRight(s, 1, 1), 1, null, 2, 3);
    checkStorage(acc.shiftRight(s, 1, 3), 1, null, null, null, 2, 3);
    checkStorage(acc.shiftRight(s, 3, 2), 1, 2, 3, null, null);
    s = acc.setObjectValue(s, 3, null);
    checkStorage(acc.shiftRight(s, 4, 1), 1, 2, 3, null, null);
  }

  private static void checkStorage(Object storage, Object... sample) {
    for (int i = 0; i < sample.length; ++i) {
      assertEquals(Arrays.toString(sample) + " [" + i + "]", sample[i], acc.getObjectValue(storage, i));
    }
  }

  private static Object fillStorage(Object... elem) {
    Object a = null;
    for (int i = 0; i < elem.length; i++) {
      a = acc.setObjectValue(a, i, elem[i]);
    }
    return a;
  }
}
