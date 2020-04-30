package com.almworks.util.collections;

import com.almworks.util.Pair;
import com.almworks.util.commons.IntProcedure2;
import com.almworks.util.commons.ObjInt2Procedure;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Const;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author : Dyoma
 * @deprecated replaced with integers
 */
@Deprecated
public final class IntArray {
  private int[] myArray;
  private int mySize;

  public IntArray() {
    this(10);
  }

  public IntArray(int size) {
    myArray = new int[size];
    mySize = 0;
  }

  public IntArray(IntArray copyFrom) {
    this(copyFrom == null ? 5 : copyFrom.size());
    addAll(copyFrom);
  }

  private void addAll(IntArray from) {
    if (from != null) {
      for (int i = 0; i < from.size(); i++)
        add(from.get(i));
    }
  }

  public void add(int value) {
    insert(mySize, value);
  }

  public int size() {
    return mySize;
  }

  public int[] toNativeArray() {
    return ArrayUtil.arrayCopy(myArray, 0, mySize);
  }

  private void ensureCapacity(int expectedSize) {
    myArray = ArrayUtil.ensureCapacity(myArray, expectedSize);
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("IntArray[");
    String sep = "";
    for (int i = 0; i < size(); i++) {
      buffer.append(sep);
      sep = ", ";
      buffer.append(String.valueOf(myArray[i]));
    }
    buffer.append("]");
    return buffer.toString();
  }

  public int get(int index) {
    rangeCheck(index);
    return myArray[index];
  }

  public void set(int index, int value) {
    rangeCheck(index);
    myArray[index] = value;
  }

  public int calcSum() {
    return Containers.calcSum(myArray, 0, mySize);
  }

  public int removeAt(int index) {
    int value = myArray[index];
    removeRange(index, index + 1);
    return value;
  }

  public void removeRange(int fromIndex, int toIndexExclusive) {
    rangeCheck(fromIndex);
    if (fromIndex == toIndexExclusive)
      return;
    rangeCheck(toIndexExclusive - 1);
    assert fromIndex < toIndexExclusive : "From:" + fromIndex + " To:" + toIndexExclusive;
    int numMoved = mySize - toIndexExclusive;
    System.arraycopy(myArray, toIndexExclusive, myArray, fromIndex, numMoved);
    mySize = mySize - (toIndexExclusive - fromIndex);
  }

  private void rangeCheck(int index) {
    if (index < 0 || index >= mySize)
      throw new IndexOutOfBoundsException("index:" + index + " size:" + mySize);
  }

  public void insertRange(int fromIndex, int toIndexExclusive, int value) {
    if (fromIndex < 0 || fromIndex > mySize)
      throw new IndexOutOfBoundsException("index:" + fromIndex + " size:" + mySize);
    if (fromIndex >= toIndexExclusive)
      return;
    ensureCapacity(mySize + toIndexExclusive - fromIndex);
    int mov = mySize - fromIndex;
    if (mov > 0)
      System.arraycopy(myArray, fromIndex, myArray, toIndexExclusive, mov);
    for (int i = fromIndex; i < toIndexExclusive; i++)
      myArray[i] = value;
    mySize += toIndexExclusive - fromIndex;
  }

  public void insert(int index, int value) {
    insertRange(index, index + 1, value);
  }

  public void clear() {
    mySize = 0;
  }

  public void insertAll(int fromIndex, IntArray array) {
    if (fromIndex < 0 || fromIndex > mySize)
      throw new IndexOutOfBoundsException("index:" + fromIndex + " size:" + mySize);
    int length = array.size();
    ensureCapacity(mySize + length);
    int mov = mySize - fromIndex;
    if (mov > 0)
      System.arraycopy(myArray, fromIndex, myArray, fromIndex + length, mov);
    System.arraycopy(array.myArray, 0, myArray, fromIndex, length);
    mySize += length;
  }

  /**
   * Returns index of the first element that is greater or equal than key
   */
  public int binarySearch(int value) {
    assert ArrayUtil.isSorted(myArray, 0, mySize);
    int r = ArrayUtil.binarySearch(value, myArray, 0, mySize);
    if (r < 0) {
      r = -r - 1;
    } else {
      // find the first appearance of value
      while (r > 0 && myArray[r - 1] == value) {
        r--;
      }
    }
    assert r >= 0 && r <= mySize;
    return r;
  }

  public int exactBinarySearch(int value) {
    assert ArrayUtil.isSorted(myArray, 0, mySize);
    return Arrays.binarySearch(myArray, 0, mySize, value);
  }

  public void replaceAll(int fromIndex, IntArray newValues) {
    if (fromIndex < 0)
      throw new IndexOutOfBoundsException("from " + fromIndex);
    if (fromIndex + newValues.size() > mySize)
      throw new IndexOutOfBoundsException("from " + fromIndex + " new " + newValues.size() + " size " + mySize);
    System.arraycopy(newValues.myArray, 0, myArray, fromIndex, newValues.size());
  }

  public void removeValue(int value) {
    for (int i = 0; i < size(); i++)
      if (myArray[i] == value) {
        removeAt(i);
        i--;
      }
  }

  public boolean contains(int value) {
    return indexOf(value) >= 0;
  }

  public int indexOf(int value) {
    for (int i = 0; i < mySize; i++)
      if (value == myArray[i])
        return i;
    return -1;
  }

  public static int indexOfNear(int[] ints, int value, int hintIndex) {
    return indexOfNear(ints, ints.length, value, hintIndex);
  }

  public static int indexOfNear(int[] ints, int length, int value, int hintIndex) {
    if (hintIndex < ints.length && ints[hintIndex] == value)
      return hintIndex;
    int low = Math.min(hintIndex, length - 1);
    int hi = hintIndex;
    while (low >= 0 || hi < length) {
      if (low >= 0 && ints[low] == value)
        return low;
      if (hi < length && ints[hi] == value)
        return hi;
      low--;
      hi++;
    }
    return -1;
  }

  public int indexOfNear(int value, int hintIndex) {
    return indexOfNear(myArray, mySize, value, hintIndex);
  }

  public void swap(int oldIndex, int newIndex) {
    int t = myArray[oldIndex];
    myArray[oldIndex] = myArray[newIndex];
    myArray[newIndex] = t;
  }

  public void addAll(int... ints) {
    if (ints.length == 0)
      return;
    ensureCapacity(mySize + ints.length);
    System.arraycopy(ints, 0, myArray, mySize, ints.length);
    mySize += ints.length;
  }

  public void sort() {
    if (mySize < 2) return;
    Arrays.sort(myArray, 0, mySize);
  }

  public void removeSubsequentDuplicates() {
    if (mySize < 2) return;
    int index = 0;
    while (index < mySize - 1) {
      int prev = myArray[index];
      int seqEnd = index + 1;
      while (seqEnd < mySize && prev == myArray[seqEnd])
        seqEnd++;
      if (seqEnd > index + 1)
        System.arraycopy(myArray, seqEnd, myArray, index + 1, mySize - seqEnd);
      mySize -= seqEnd - index - 1;
      index++;
    }
  }

  public boolean isSorted() {
    return ArrayUtil.isSorted(myArray, 0, mySize);
  }

  public boolean isUniqueSorted() {
    return ArrayUtil.isUniqueSorted(myArray, 0, mySize);
  }

  /**
   * @see #visitSequentialValueIntervals(com.almworks.util.commons.ObjInt2Procedure, Object, boolean)
   */
  public void visitSequentialValueIntervals(IntProcedure2 visitor, boolean forward) {
    visitSequentialValueIntervals(ObjInt2Procedure.CALL_PROCEDURE, visitor, forward);
  }

  /**
   * Finds intervals of equal or sequential values and invokes visitor with first(min) and last(max) value.<br>
   * Can search for intervals in forward and backward directions - first find intervals with smaller indexes or larger
   * indexes.<br>
   * Each interval is provided as (val[smaller], val[larger]) in any case, even when searching from end to beginning.
   * Examples:<br>
   * 1. {1, 2, 3} single call (1, 3)
   * 2. {1, 1, 2, 3} single call (1, 3)
   * 3. {1, 1, 3, 4, 4, 6} three calls (1, 1); (3, 4); (6, 6) for forward=true. (6, 6); (3, 4); (1, 1) for forward=false
   * @param visitor internal iterator (visitor)
   * @param arg argument passed to procedure on each invokation
   * @param forward defines direction. true from index 0 to size-1, false backward from index size-1 to 0.
   */
  public <T> void visitSequentialValueIntervals(ObjInt2Procedure<? super T> visitor, T arg, boolean forward) {
    assert isSorted();
    if (mySize == 0) return;
    if (mySize == 1) {
      visitor.invoke(arg, myArray[0], myArray[0]);
      return;
    }
    int start = forward ? 0 : mySize - 1;
    int end = forward ? mySize : -1;
    int delta = forward ? 1 : -1;
    int intervalIndex = start;
    for (int i = start + delta; i != end; i += delta) {
      int value = myArray[i];
      int prevValue = myArray[i - delta];
      if (value == prevValue || value == prevValue + delta) continue;
      int a = myArray[intervalIndex];
      int b = myArray[i - delta];
      visitor.invoke(arg, forward ? a : b, forward ? b : a);
      intervalIndex = i;
    }
    int a = myArray[intervalIndex];
    int b = myArray[end - delta];
    visitor.invoke(arg, forward ? a : b, forward ? b : a);
  }


  public static IntArray create(int ... array) {
    IntArray result = new IntArray();
    if (array != null) result.addAll(array);
    return result;
  }

  public static IntArray sortedNoDuplicates(int ... array) {
    IntArray result = create(array);
    result.sort();
    result.removeSubsequentDuplicates();
    return result;
  }

  public void addFromTo(IntArray other, int first, int lastExclusive) {
    if (other == null) return;
    lastExclusive = Math.min(lastExclusive, other.size());
    if (lastExclusive <= first) return;
    ensureCapacity(mySize + lastExclusive - first);
    for (int i = first; i < lastExclusive; i++) add(other.get(i));
  }

  public static IntArray sortedNoDuplicates(IntArray array) {
    IntArray result = new IntArray(array);
    result.sort();
    result.removeSubsequentDuplicates();
    return result;
  }

  /**
   * Computes set difference to other array. Result is represented as elements only in this and only in other.<br>
   * This must be unique-sorted
   * @param other uniqu-sorted array
   * @return pair&lt;thisOnly, otherOnlt&gt; result and each pair component is notNull
   */
  @NotNull
  public Pair<IntArray, IntArray> difference(int[] other) {
    assert isUniqueSorted();
    assert ArrayUtil.isUniqueSorted(other);
    if (other == null) other = Const.EMPTY_INTS;
    IntArray thisOnly = new IntArray();
    IntArray otherOnly = new IntArray();
    int cI = 0;
    for (int i = 0; i < size(); i++) {
      int index = get(i);
      while (cI < other.length && other[cI] < index) {
        otherOnly.add(other[cI]);
        cI++;
      }
      if (cI >= other.length) {
        thisOnly.addFromTo(this, i, size());
        break;
      }
      assert index <= other[cI];
      if (index != other[cI]) thisOnly.add(index);
      else cI++;
    }
    while (cI < other.length) {
      otherOnly.add(other[cI]);
      cI++;
    }
    return Pair.create(thisOnly, otherOnly);
  }
}
