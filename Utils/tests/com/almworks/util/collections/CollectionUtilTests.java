package com.almworks.util.collections;

import com.almworks.util.commons.IntIntFunction;
import com.almworks.util.commons.IntIntFunction2;
import com.almworks.util.commons.IntProcedure2;
import com.almworks.util.tests.BaseTestCase;

import java.util.Arrays;
import java.util.Random;

/**
 * @author dyoma
 */
public class CollectionUtilTests extends BaseTestCase {
  private final long myNanoTime = System.nanoTime();
  private final Random myRandom = new Random(myNanoTime);
  private final String myCondition = myNanoTime + "L";

  public void testRandomQuickSort() {
    //    Random random = new Random(116576651102827L);
    for (int i = 0; i < 10; i++) {
      final int[] ints = generateArray();
      IntIntFunction2 order = new IntIntFunction2() {
        public int invoke(int a, int b) {
          return ints[a] - ints[b];
        }
      };
      IntProcedure2 swap = new IntProcedure2() {
        public void invoke(int a, int b) {
          int t = ints[a];
          ints[a] = ints[b];
          ints[b] = t;
        }
      };
      CollectionUtil.quicksort(ints.length, order, swap);
      checkSorted(i, ints);
    }
  }

  private int[] generateArray() {
    final int[] ints = new int[myRandom.nextInt(100) + 1];
    for (int j = 0; j < ints.length; j++)
      ints[j] = myRandom.nextInt(ints.length);
    return ints;
  }

  public void testQuickSortBug() {
    final int[] ints = {13, 12, 12, 16, 13, 14, 16};
    IntIntFunction2 order = new IntIntFunction2() {
      public int invoke(int a, int b) {
        return ints[a] - ints[b];
      }
    };
    IntProcedure2 swap = new IntProcedure2() {
      public void invoke(int a, int b) {
        int t = ints[a];
        ints[a] = ints[b];
        ints[b] = t;
      }
    };
    CollectionUtil.quicksort(ints.length, order, swap);
    checkSorted(0, ints);
  }

  private void checkSorted(int iteration, int[] ints) {
    for (int i = 1; i < ints.length; i++)
      assertTrue(myCondition + " " + iteration + " " + i, ints[i - 1] <= ints[i]);
  }

  public void testRandomBinarySearch() {
    for (int i = 0; i < 50; i++) {
      final int[] ints = generateArray();
      Arrays.sort(ints);
      final int index = myRandom.nextInt(ints.length);
      assertEquals(myCondition, ints[index], ints[CollectionUtil.binarySearch(ints.length, new IntIntFunction() {
        public int invoke(int a) {
          return ints[a] - ints[index];
        }
      })]);
      assertEquals(myCondition, -1, CollectionUtil.binarySearch(ints.length, new IntIntFunction() {
        public int invoke(int a) {
          return ints[a] - (ints[0] - 1);
        }
      }));
      assertEquals(myCondition, -ints.length - 1, CollectionUtil.binarySearch(ints.length, new IntIntFunction() {
        public int invoke(int a) {
          return ints[a] - (ints[ints.length - 1] + 1);
        }
      }));
      final int value = ints[index];
      for (int j = index; j < ints.length; j++)
        ints[j]++;
      for (int j = 0; j < index; j++)
        ints[j]--;
      assertEquals(myCondition, -index-1, CollectionUtil.binarySearch(ints.length, new IntIntFunction() {
        public int invoke(int a) {
          return ints[a] - value;
        }
      }));
    }
  }
}
