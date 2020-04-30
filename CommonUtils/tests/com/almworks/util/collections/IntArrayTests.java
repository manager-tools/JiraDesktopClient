package com.almworks.util.collections;

import com.almworks.util.Pair;
import com.almworks.util.commons.ObjInt2Procedure;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Const;

/**
 * @author dyoma
 */
public class IntArrayTests extends BaseTestCase {
  private final IntArray myArray = new IntArray();
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  public void testRemoveSubsequentDuplicates() {
    myArray.addAll(1, 1, 2);
    myArray.removeSubsequentDuplicates();
    checkArray(1, 2);
    myArray.clear();

    myArray.addAll(1, 2, 2, 3);
    myArray.removeSubsequentDuplicates();
    checkArray(1, 2, 3);
    myArray.clear();

    myArray.addAll(1, 2, 3, 3);
    myArray.removeSubsequentDuplicates();
    checkArray(1, 2, 3);
    myArray.clear();

    myArray.addAll(1, 1, 2, 2, 3, 3);
    myArray.removeSubsequentDuplicates();
    checkArray(1, 2, 3);
  }

  public void testVisitSequentialValueIntervals() {
    ObjInt2Procedure<IntArray> visitor = new ObjInt2Procedure<IntArray>() {
      @Override
      public void invoke(IntArray arr, int a, int b) {
        assertTrue(a <= b);
        for (int i = a; i <= b; i++) {
          assertFalse(arr.contains(i));
          arr.add(i);
        }
      }
    };
    IntArray a = IntArray.create(487, 492, 493);
    a.visitSequentialValueIntervals(visitor, myArray, false);
    checkArray(492, 493, 487);

    myArray.clear();
    a.visitSequentialValueIntervals(visitor, myArray, true);
    checkArray(487, 492, 493);

    myArray.clear();
    a.clear();
    a.addAll(1, 1, 3, 4, 6);
    a.visitSequentialValueIntervals(visitor, myArray, true);
    checkArray(1, 3, 4, 6);

    myArray.clear();
    a.visitSequentialValueIntervals(visitor, myArray, false);
    checkArray(6, 3, 4, 1);

    myArray.clear();
    a.clear();
    a.visitSequentialValueIntervals(visitor, myArray, true);
    checkArray();
    a.visitSequentialValueIntervals(visitor, myArray, false);
    checkArray();

    a.add(1);
    a.visitSequentialValueIntervals(visitor, myArray, true);
    checkArray(1);
    myArray.clear();
    a.visitSequentialValueIntervals(visitor, myArray, false);
    checkArray(1);

    myArray.clear();
    a.add(2);
    a.visitSequentialValueIntervals(visitor, myArray, true);
    checkArray(1, 2);
    myArray.clear();
    a.visitSequentialValueIntervals(visitor, myArray, false);
    checkArray(1, 2);

    myArray.clear();
    a.clear();
    a.addAll(1, 3, 5);
    a.visitSequentialValueIntervals(visitor, myArray, true);
    checkArray(1, 3, 5);
    myArray.clear();
    a.visitSequentialValueIntervals(visitor, myArray, false);
    checkArray(5, 3, 1);
  }

  public void testDifference() {
    checkDifference(null, null, null, null);
    int[] a1 = {1};
    int[] a2 = {2};
    int[] a12 = new int[] {1, 2};
    checkDifference(a1, null, a1, null);
    checkDifference(a12, null, a12, null);
    checkDifference(a1, a12, null, a2);
    checkDifference(a1, a1, null, null);
    checkDifference(a12, a12, null, null);

    checkDifference(new int[]{-1, 0, 2, 3}, a12, new int[]{-1, 0, 3}, a1);
    checkDifference(new int[]{-2, -1}, a12, new int[]{-2, -1}, a12);
    checkDifference(new int[]{-2, -1, 1}, a12, new int[]{-2, -1}, a2);
  }
  
  private void checkDifference(int[] array, int[] other, int[] arrayOnly, int[] otherOnly) {
    doCheckDifference(array, other, arrayOnly, otherOnly);
    doCheckDifference(other, array, otherOnly, arrayOnly);
  }

  private void doCheckDifference(int[] array, int[] other, int[] arrayOnly, int[] otherOnly) {
    IntArray a = IntArray.create(array);
    Pair<IntArray,IntArray> pair = a.difference(other);
    if (arrayOnly == null) arrayOnly = Const.EMPTY_INTS;
    if (otherOnly == null) otherOnly = Const.EMPTY_INTS;
    CHECK.order(pair.getFirst().toNativeArray(), arrayOnly);
    CHECK.order(pair.getSecond().toNativeArray(), otherOnly);
  }

  private void checkArray(int ... ints) {
    CHECK.order(myArray.toNativeArray(), ints);
  }
}
