package com.almworks.util.collections;

import org.almworks.util.Const;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author dyoma
 */
public class IntSetArray {
  @NotNull
  private int[][] myArray;

  public IntSetArray() {
    this(10);
  }

  public IntSetArray(int initialSize) {
    myArray = new int[initialSize][];
  }

  public int getSize(int setIndex) {
    checkIndex(setIndex);
    int[] ints = myArray[setIndex];
    return ints != null ? ints.length : 0;
  }

  public int getIntAt(int setIndex, int intIndex) {
    checkIndex(setIndex);
    int[] ints = myArray[setIndex];
    if (intIndex < 0 || ints == null || intIndex >= ints.length)
      throw new IndexOutOfBoundsException(intIndex + " " + getSize(setIndex));
    return ints[intIndex];
  }

  @NotNull
  public int[] getAll(int setIndex) {
    checkIndex(setIndex);
    int[] ints = myArray[setIndex];
    if (ints == null)
      return Const.EMPTY_INTS;
    int[] result = new int[ints.length];
    System.arraycopy(ints, 0, result, 0, ints.length);
    return result;
  }

  private void checkIndex(int setIndex) {
    if (setIndex < 0 || setIndex >= myArray.length)
      throw new IndexOutOfBoundsException(setIndex + " " + myArray.length);
  }

  public void add(int setIndex, int anInt) {
    checkIndex(setIndex);
    int[] ints = myArray[setIndex];
    if (ints == null)
      myArray[setIndex] = new int[] {anInt};
    else {
      if (Arrays.binarySearch(ints, anInt) >= 0)
        return;
      int[] newInts = new int[ints.length + 1];
      System.arraycopy(ints, 0, newInts, 1, ints.length);
      newInts[0] = anInt;
      Arrays.sort(newInts);
      myArray[setIndex] = newInts;
    }
  }

  public void remove(int setIndex, int anInt) {
    int[] ints = myArray[setIndex];
    if (ints == null || Arrays.binarySearch(ints, anInt) < 0)
      return;
    int[] newInts;
    if (ints.length == 1)
      newInts = null;
    else {
      newInts = new int[ints.length - 1];
      int index = 0;
      for (int i : ints) {
        if (i == anInt)
          continue;
        newInts[index] = i;
        index++;
      }
      assert index == newInts.length;
    }
    myArray[setIndex] = newInts;
  }

  public void expandUpTo(int minSize) {
    ensureCapacity(minSize);
  }

  private void ensureCapacity(int minSize) {
    if (myArray.length >= minSize)
      return;
    int newSize = myArray.length;
    while (newSize < minSize)
      newSize *= 1.5;
    int[][] newSets = new int[newSize][];
    System.arraycopy(myArray, 0, newSets, 0, myArray.length);
    myArray = newSets;
  }
}
