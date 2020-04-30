package com.almworks.util.collections;

import com.almworks.util.text.TextUtil;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Const;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Deprecated
public class LongArray {
  @NotNull
  private long[] myArray;
  private int mySize;

  public LongArray() {
    this(0);
  }

  public LongArray(int capacity) {
    myArray = capacity > 0 ? new long[capacity] : Const.EMPTY_LONGS;
    mySize = 0;
  }

  public LongArray(LongArray copyFrom) {
    this(copyFrom != null ? copyFrom.size() : 0);
    if (copyFrom != null) {
      for (int i = 0; i < copyFrom.size(); i++)
        add(copyFrom.get(i));
    }
  }

  public void add(long value) {
    ensureCapasity(mySize + 1);
    myArray[mySize] = value;
    mySize++;
  }

  public long removeLast() {
    if (mySize <= 0) throw new IndexOutOfBoundsException(String.valueOf(mySize));
    long result = myArray[mySize - 1];
    mySize--;
    return result;
  }

  private void ensureCapasity(int expectedCapacity) {
    myArray = ArrayUtil.ensureCapacity(myArray, expectedCapacity);
  }

  public long[] toNativeArray() {
    return ArrayUtil.arrayCopy(myArray, 0, mySize);
  }

  @Override
  public String toString() {
    return "LongArray[" + TextUtil.separate(myArray, 0, mySize, ", ") + "]";
  }

  public int size() {
    return mySize;
  }

  /**
   * Alienate internal buffer and clears. Before calling this method client code should obtain {@link #size()} to get
   * know how much elements of inter buffer are actually in use. The length of returned array can be greate then {@link #size()}
   * <br>After calling this method the {@link com.almworks.util.collections.LongArray} becomes empty.
   * @return internal buffer if it was not null. If internal buffer happens to be null empty array is returned.
   */
  @NotNull
  public long[] alienateArray() {
    long[] array = myArray;
    mySize = 0;
    myArray = Const.EMPTY_LONGS;
    return array;
  }

  public boolean contains(long val) {
    for (int i = 0; i < mySize; i++)
      if (val == myArray[i])
        return true;
    return false;
  }

  public void addAll(long[] values) {
    ensureCapasity(mySize + values.length);
    System.arraycopy(values, 0, myArray, mySize, values.length);
    mySize += values.length;
  }

  public long get(int index) {
    if (index < 0 || index >= mySize)
      throw new IndexOutOfBoundsException(mySize + " " + index);
    return myArray[index];
  }

  public boolean isEmpty() {
    return mySize == 0;
  }

  public void sortUnique() {
    Arrays.sort(myArray, 0, mySize);
    mySize = ArrayUtil.removeSubsequentDuplicates(myArray, 0, mySize);
  }

  public int binarySearch(long value) {
    return ArrayUtil.binarySearch(value, myArray, 0, mySize);
  }
}
