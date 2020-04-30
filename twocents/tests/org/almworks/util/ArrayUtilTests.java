package org.almworks.util;

import junit.framework.TestCase;

public class ArrayUtilTests extends TestCase {
  public void testRemoveSubsequentDuplicatesInt() {
    checkRemoveSubsequentDuplicates(new int[] {1, 1, 2}, 1, 2);
    checkRemoveSubsequentDuplicates(new int[] {1, 2, 2, 3}, 1, 2, 3);
    checkRemoveSubsequentDuplicates(new int[] {1, 2, 3, 3}, 1, 2, 3);
    checkRemoveSubsequentDuplicates(new int[] {1, 1, 2, 2, 3, 3}, 1, 2, 3);

    checkRemoveSubsequentDuplicates(2, 4, new int[]{10, 10, 10, 10, 20, 20, 20}, 10, 20);
  }

  private void checkRemoveSubsequentDuplicates(int[] array, int ... result) {
    checkRemoveSubsequentDuplicates(0, array.length, array, result);
  }

  private void checkRemoveSubsequentDuplicates(int offset, int length, int[] array, int ... result) {
    int[] arrayCopy = ArrayUtil.arrayCopy(array);
    int newLength = ArrayUtil.removeSubsequentDuplicates(array, offset, length);
    for (int i = 0; i < offset; i++)
      assertEquals(String.valueOf(i), arrayCopy[i], array[i]);
    for (int i = offset + length; i < arrayCopy.length; i++)
      assertEquals(String.valueOf(i), arrayCopy[i], array[i]);
    assertEquals(result.length, newLength);
    for (int i = 0; i < result.length; i++) {
      int expected = result[i];
      assertEquals(String.valueOf(i), expected, array[offset + i]);
    }
  }
}
