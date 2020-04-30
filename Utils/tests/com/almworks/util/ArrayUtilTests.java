package com.almworks.util;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ArrayUtilTests extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();

  public void testIsSorted() {
    assertTrue(ArrayUtil.isSorted(new long[] {}, 0, 0));
    assertTrue(ArrayUtil.isSorted(new long[] {0}, 0, 1));
    assertTrue(ArrayUtil.isSorted(new long[] {Long.MIN_VALUE}, 0, 1));
    assertTrue(ArrayUtil.isSorted(new long[] {Long.MAX_VALUE}, 0, 1));
    assertTrue(ArrayUtil.isSorted(new long[] {Long.MAX_VALUE}, 0, 0));
    assertTrue(ArrayUtil.isSorted(new long[] {1, 2, 3}, 0, 3));
    assertTrue(ArrayUtil.isSorted(new long[] {1, 3, 2}, 0, 2));
    assertTrue(ArrayUtil.isSorted(new long[] {1, 3, 2, 4}, 2, 2));
    assertTrue(ArrayUtil.isSorted(new long[] {1, 1}, 0, 2));
    assertTrue(ArrayUtil.isSorted(new long[] {1, 1, 2, 2, 2}, 0, 5));

    assertFalse(ArrayUtil.isSorted(new long[] {1, 3, 2, 4}, 1, 2));
    assertFalse(ArrayUtil.isSorted(new long[] {1, 3, 2, 4}, 0, 4));
  }

  public void testHasIntersection() {
    assertFalse(ArrayUtil.hasIntersection(new long[] {}, 0, 0, new long[] {}, 0, 0));
    assertFalse(ArrayUtil.hasIntersection(new long[] {1}, 0, 0, new long[] {}, 0, 0));
    assertFalse(ArrayUtil.hasIntersection(new long[] {}, 0, 0, new long[] {1}, 0, 0));
    assertFalse(ArrayUtil.hasIntersection(new long[] {1}, 0, 0, new long[] {1}, 0, 0));
    assertFalse(ArrayUtil.hasIntersection(new long[] {-1, 0, 1}, 0, 2, new long[] {-1, 0, 1, 2, 3}, 2, 3));

    assertTrue(ArrayUtil.hasIntersection(new long[] {1}, 0, 1, new long[] {1}, 0, 1));
    assertTrue(ArrayUtil.hasIntersection(new long[] {1, 1}, 0, 2, new long[] {1}, 0, 1));
    assertTrue(ArrayUtil.hasIntersection(new long[] {2, 3, 4, 5, 6, 7, 8, 9}, 0, 8, new long[] {-4, -2, 5, 15}, 0, 4));

    assertTrue(ArrayUtil.hasIntersection(new long[] {2}, 0, 1, new long[] {1, 2}, 0, 2));
    assertFalse(ArrayUtil.hasIntersection(new long[] {10}, 0, 1, new long[] {1, 2, 3}, 0, 3));
  }

  public void testContainsAll() {
    assertTrue(ArrayUtil.containsAll(new long[] {}, 0, 0, new long[] {}, 0, 0));
    assertTrue(ArrayUtil.containsAll(new long[] {1}, 0, 1, new long[] {}, 0, 0));
    assertTrue(ArrayUtil.containsAll(new long[] {1}, 0, 1, new long[] {1}, 0, 1));
    assertTrue(ArrayUtil.containsAll(new long[] {1, 10, 20, 30, 40}, 0, 5, new long[] {10, 30, 50}, 0, 2));

    assertFalse(ArrayUtil.containsAll(new long[] {}, 0, 0, new long[] {1}, 0, 1));
    assertFalse(ArrayUtil.containsAll(new long[] {1, 10, 20, 30, 40}, 0, 5, new long[] {10, 29, 50}, 0, 2));
    assertFalse(ArrayUtil.containsAll(new long[] {1, 10, 20, 30, 40}, 0, 2, new long[] {10, 20, 40}, 0, 3));
  }
  
  public void testEnsureCapacity() {
    CharSequence[] array = new CharSequence[] {"a", "b", "c"};
    assertSame(array, ArrayUtil.ensureCapacity(array, 2));
    assertSame(array, ArrayUtil.ensureCapacity(array, 3));
    CharSequence[] newArray = ArrayUtil.ensureCapacity(array, 4);
    assertEquals(16, newArray.length);
    assertEquals(CharSequence.class, newArray.getClass().getComponentType());
    for (int i = 0; i < array.length; i++)
      assertEquals(array[i], newArray[i]);
  }

  public void testReverse() {
    List<Integer> objects = Collections15.arrayList(new Integer[] {1, 2, 3, 4});
    CHECK.order(new Integer[] {1, 2, 4, 3}, reverse(objects, 2, 2));
    CHECK.order(new Integer[] {1, 4, 3, 2}, reverse(objects, 1, 3));
  }

  private Object[] reverse(List<?> objects, int offset, int length) {
    Object[] array = objects.toArray();
    ArrayUtil.reverse(array, offset, length);
    return array;
  }

  public void testQuickSort() {
    checkQuickSort(100000);
    checkQuickSort(8);
    checkQuickSort(7);
    checkQuickSort(6);
    checkQuickSort(2);
    checkQuickSort(1);
  }

  private void checkQuickSort(int size) {
    String[] array = new String[size];
    Random random = new Random();
    for (int i = 0; i < size; i++) {
      char c = (char)((random.nextInt() % 96) + 32);
      array[i] = c + String.valueOf(random.nextDouble());
    }

    Comparator<String> comparator = String.CASE_INSENSITIVE_ORDER;
    long start = System.currentTimeMillis();
    ArrayUtil.quicksort(array, comparator);
    long time = System.currentTimeMillis() - start;
    System.out.println("quicksort took " + time + "ms");

    for (int i = 0; i < size - 1; i++) {
      if (comparator.compare(array[i], array[i + 1]) > 0) {
        fail(i + " " + array[i] + " " + array[i + 1]);
      }
    }
  }

}
