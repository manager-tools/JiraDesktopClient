package org.almworks.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

public class ArrayUtil {
  /**
   * Checks if array A contains all values from array B; offsets and lengths are used to check only parts of arrays.
   * Arrays must be sorted.
   *
   * @return true if each item in sortedB[offsetB, offsetB+lengthB) at least once appears in
   *         sortedA[offsetA, offsetA+lengthA)
   */
  public static boolean containsAll(long[] sortedA, int offsetA, int lengthA, long[] sortedB, int offsetB,
    int lengthB)
  {
    if (sortedA.length == 0)
      return sortedB.length == 0;
    if (sortedB.length == 0)
      return true;
    assert isSorted(sortedA, offsetA, lengthA);
    assert isSorted(sortedB, offsetB, lengthB);
    int iA = 0;
    int iB;
    for (iB = 0; iB < lengthB; iB++) {
      long sought = sortedB[offsetB + iB];
      while (iA < lengthA) {
        if (sortedA[offsetA + iA] == sought)
          break;
        iA++;
      }
      if (iA == lengthA)
        break;
    }
    return iB == lengthB;
  }

  /**
   * @return true if there is at least one value that is present in both arrays
   */
  public static boolean hasIntersection(long[] sortedA, int offsetA, int lengthA, long[] sortedB, int offsetB,
    int lengthB)
  {
    if (lengthA == 0 || lengthB == 0)
      return false;
    assert isSorted(sortedA, offsetA, lengthA);
    assert isSorted(sortedB, offsetB, lengthB);
    int stopA = offsetA + lengthA;
    int stopB = offsetB + lengthB;
    int iB = offsetB;
    long vB = sortedB[iB++];
    for (int iA = offsetA; iA < stopA; iA++) {
      long vA = sortedA[iA];
      while (iB < stopB && vB < vA)
        vB = sortedB[iB++];
      if (vB == vA)
        return true;
      if (iB == stopB)
        return false;
    }
    return false;
  }

  /**
   * @return true if arrays is sorted in ascending order
   */
  public static boolean isSorted(long[] array, int offset, int length) {
    long v = Long.MIN_VALUE;
    for (int i = 0; i < length; i++) {
      long w = array[i + offset];
      if (w < v)
        return false;
      v = w;
    }
    return true;
  }

  /**
   * @return true if arrays is sorted in ascending order
   */
  public static boolean isSorted(int[] array, int offset, int length) {
    int v = Integer.MIN_VALUE;
    for (int i = 0; i < length; i++) {
      int w = array[i + offset];
      if (w < v)
        return false;
      v = w;
    }
    return true;
  }

  public static boolean isUniqueSorted(int[] array, int offset, int length) {
    if (array == null || length < 2) return true;
    int v = array[offset];
    for (int i = offset + 1; i < offset + length; i++) {
      int c = array[i];
      if (c <= v) return false;
      v = c;
    }
    return true;
  }

  public static boolean isUniqueSorted(int[] array) {
    if (array == null) return true;
    return isUniqueSorted(array, 0, array.length);
  }

  public static boolean isSorted(@Nullable int[] ints) {
    return ints == null || isSorted(ints, 0, ints.length);
  }

  public static boolean equals(long[] arrayA, int offsetA, int lengthA, long[] arrayB, int offsetB, int lengthB) {
    if (lengthA != lengthB)
      return false;
    for (int i = 0; i < lengthA; i++)
      if (arrayA[offsetA++] != arrayB[offsetB++])
        return false;
    return true;
  }

  public static boolean equals(Object[] arrayA, int offsetA, int lengthA, Object[] arrayB, int offsetB, int lengthB) {
    if (lengthA != lengthB)
      return false;
    for (int i = 0; i < lengthA; i++)
      if (!Util.equals(arrayA[offsetA++], arrayB[offsetB++]))
        return false;
    return true;
  }

  public static boolean equals(@Nullable long[] arrayA, @Nullable long[] arrayB) {
    if (arrayA == arrayB)
      return true;
    if (arrayA == null || arrayB == null)
      return false;
    return equals(arrayA, 0, arrayA.length, arrayB, 0, arrayB.length);
  }

  public static boolean equals(int[] arrayA, int offsetA, int lengthA, int[] arrayB, int offsetB, int lengthB) {
    if (lengthA != lengthB)
      return false;
    for (int i = 0; i < lengthA; i++)
      if (arrayA[offsetA++] != arrayB[offsetB++])
        return false;
    return true;
  }

  public static boolean equals(byte[] arrayA, int offsetA, int lengthA, byte[] arrayB, int offsetB, int lengthB) {
    if (lengthA != lengthB)
      return false;
    for (int i = 0; i < lengthA; i++)
      if (arrayA[offsetA++] != arrayB[offsetB++])
        return false;
    return true;
  }

  public static boolean equals(@Nullable int[] arrayA, @Nullable int[] arrayB) {
    if (arrayA == arrayB)
      return true;
    if (arrayA == null || arrayB == null)
      return false;
    return equals(arrayA, 0, arrayA.length, arrayB, 0, arrayB.length);
  }

  public static <T> boolean isSorted(T[] array, Comparator<T> comparator) {
    for (int i = 0; i < array.length - 1; i++)
      if (comparator.compare(array[i], array[i + 1]) > 0)
        return false;
    return true;
  }

  public static <T> void reverse(T[] objects, int offset, int length) {
    int lastIndex = offset + length - 1;
    for (int i = 0; i < length / 2; i++) {
      int index1 = offset + i;
      int index2 = lastIndex - i;
      T temp = objects[index1];
      objects[index1] = objects[index2];
      objects[index2] = temp;
    }
  }

  public static <T> void reverse(T[] array) {
    reverse(array, 0, array.length);
  }

  /**
   * Does a quicksort for objects. Not stable!
   * Copied from java.util.Arrays, which allows only primitives to be quick-sorted
   */
  public static <T> void quicksort(T[] a, Comparator<? super T> c) {
    sort1(a, 0, a.length, c);
  }

  private static <T> void sort1(T[] x, int off, int len, Comparator<? super T> comp) {
    // Insertion sort on smallest arrays
    if (len < 7) {
      for (int i = off; i < len + off; i++)
        for (int j = i; j > off && comp.compare(x[j - 1], x[j]) > 0; j--)
          swap(x, j, j - 1);
      return;
    }

    // Choose a partition element, v
    int m = off + (len >> 1);       // Small arrays, middle element
    if (len > 7) {
      int l = off;
      int n = off + len - 1;
      if (len > 40) {        // Big arrays, pseudomedian of 9
        int s = len / 8;
        l = med3(x, l, l + s, l + 2 * s, comp);
        m = med3(x, m - s, m, m + s, comp);
        n = med3(x, n - 2 * s, n - s, n, comp);
      }
      m = med3(x, l, m, n, comp); // Mid-size, med of 3
    }
    T v = x[m];

    // Establish Invariant: v* (<v)* (>v)* v*
    int a = off, b = a, c = off + len - 1, d = c;
    while (true) {
      while (b <= c && comp.compare(x[b], v) <= 0) {
        if (x[b] == v)
          swap(x, a++, b);
        b++;
      }
      while (c >= b && comp.compare(x[c], v) >= 0) {
        if (x[c] == v)
          swap(x, c, d--);
        c--;
      }
      if (b > c)
        break;
      swap(x, b++, c--);
    }

    // Swap partition elements back to middle
    int s, n = off + len;
    s = Math.min(a - off, b - a);
    vecswap(x, off, b - s, s);
    s = Math.min(d - c, n - d - 1);
    vecswap(x, b, n - s, s);

    // Recursively sort non-partition-elements
    if ((s = b - a) > 1)
      sort1(x, off, s, comp);
    if ((s = d - c) > 1)
      sort1(x, n - s, s, comp);
  }

  public static void swap(Object[] x, int a, int b) {
    Object t = x[a];
    x[a] = x[b];
    x[b] = t;
  }

  public static void swap(int[] x, int a, int b) {
    int t = x[a];
    x[a] = x[b];
    x[b] = t;
  }

  private static void vecswap(Object x[], int a, int b, int n) {
    for (int i = 0; i < n; i++, a++, b++)
      swap(x, a, b);
  }

  private static <T> int med3(T[] x, int a, int b, int c, Comparator<? super T> comp) {
    return (comp.compare(x[a], x[b]) < 0 ? (comp.compare(x[b], x[c]) < 0 ? b : comp.compare(x[a], x[c]) < 0 ? c : a) :
      (comp.compare(x[b], x[c]) > 0 ? b : comp.compare(x[a], x[c]) > 0 ? c : a));
  }

  public static int min(int[] array) {
    if (array == null || array.length == 0)
      return Integer.MIN_VALUE;
    int min = Integer.MAX_VALUE;
    for (int a : array) {
      if (a < min)
        min = a;
    }
    return min;
  }

  /**
   * Copied from Arrays.
   *
   * @param from index to start search, inclusive
   * @param to   ending index, exclusive
   */
  public static int binarySearch(int key, int[] a, int from, int to) {
    int low = from;
    int high = to - 1;

    while (low <= high) {
      int mid = (low + high) >> 1;
      int midVal = a[mid];

      if (midVal < key)
        low = mid + 1;
      else if (midVal > key)
        high = mid - 1;
      else
        return mid; // key found
    }
    return -(low + 1);  // key not found.
  }

  /**
   * Copied from {@link java.util.Arrays#binarySearch(long[], long)}
   *
   * @param from inclusive
   * @param to   exclusive
   */
  public static int binarySearch(long key, long[] a, int from, int to) {
    int low = from;
    int high = to - 1;

    while (low <= high) {
      int mid = (low + high) >> 1;
      long midVal = a[mid];

      if (midVal < key)
        low = mid + 1;
      else if (midVal > key)
        high = mid - 1;
      else
        return mid; // key found
    }
    return -(low + 1);  // key not found.
  }

  public static <T> T[] ensureCapacity(@NotNull T[] array, int capacity) {
    return ensureCapacity(array, capacity, true);
  }

  public static <T> T[] ensureCapacity(@NotNull T[] array, int capacity, boolean preserveData) {
    int length = array.length;
    if (length >= capacity)
      return array;
    return reallocArray(array, Math.max(16, Math.max(capacity, length * 2)), preserveData);
  }

  public static long[] ensureCapacity(@Nullable long[] array, int capacity) {
    int length = array == null ? -1 : array.length;
    if (length >= capacity)
      return array;
    if (capacity == 0)
      return Const.EMPTY_LONGS;
    return reallocArray(array, Math.max(16, Math.max(capacity, length * 2)));
  }

  public static int[] ensureCapacity(@Nullable int[] array, int capacity) {
    int length = array == null ? -1 : array.length;
    if (length >= capacity)
      return array;
    if (capacity == 0)
      return Const.EMPTY_INTS;
    return reallocArray(array, Math.max(16, Math.max(capacity, length * 2)));
  }

  public static double[] ensureCapacity(@Nullable double[] array, int capacity) {
    int length = array == null ? -1 : array.length;
    if (length >= capacity)
      return array;
    if (capacity == 0)
      return Const.EMPTY_DOUBLES;
    return reallocArray(array, Math.max(16, Math.max(capacity, length * 2)));
  }

  public static <T> T reallocArray(@NotNull T array, int length) {
    return reallocArray(array, length, true);
  }

  @SuppressWarnings({"SuspiciousSystemArraycopy", "unchecked"})
  public static <T> T reallocArray(@NotNull T array, int length, boolean preserveData) {
    int current = Array.getLength(array);
    if (current == length)
      return array;
    T newArray = (T) Array.newInstance(array.getClass().getComponentType(), length);
    if (preserveData) {
      System.arraycopy(array, 0, newArray, 0, Math.min(length, current));
    }
    return newArray;
  }

  public static long[] reallocArray(@Nullable long[] array, int length) {
    assert length >= 0 : length;
    int current = array == null ? -1 : array.length;
    if (current == length)
      return array;
    if (length == 0)
      return Const.EMPTY_LONGS;
    long[] newArray = new long[length];
    int copy = Math.min(length, current);
    if (copy > 0)
      System.arraycopy(array, 0, newArray, 0, copy);
    return newArray;
  }

  public static int[] reallocArray(@Nullable int[] array, int length) {
    assert length >= 0 : length;
    int current = array == null ? -1 : array.length;
    if (current == length)
      return array;
    if (length == 0)
      return Const.EMPTY_INTS;
    int[] newArray = new int[length];
    int copy = Math.min(length, current);
    if (copy > 0)
      System.arraycopy(array, 0, newArray, 0, copy);
    return newArray;
  }

  public static int indexOf(int[] ints, int value) {
    return indexOf(ints, 0, ints.length, value);
  }

  public static int indexOf(long[] longs, long value) {
    return indexOf(longs, 0, longs.length, value);
  }

  public static int indexOf(char[] chars, char value) {
    return indexOf(chars, 0, chars.length, value);
  }

  public static int indexOf(long[] longs, int from, int to, long value) {
    for (int i = from; i < to; i++) {
      if (longs[i] == value)
        return i;
    }
    return -1;
  }

  public static int indexOf(int[] ints, int from, int to, int value) {
    for (int i = from; i < to; i++) {
      if (ints[i] == value)
        return i;
    }
    return -1;
  }

  public static int indexOf(char[] chars, int from, int to, char value) {
    for (int i = from; i < to; i++) {
      if (chars[i] == value)
        return i;
    }
    return -1;
  }

  public static int[] arrayCopy(int[] ints) {
    return arrayCopy(ints, 0, ints.length);
  }

  public static byte[] arrayCopy(byte[] bytes) {
    return arrayCopy(bytes, 0, bytes.length);
  }

  public static int[] arrayCopy(int[] array, int offset, int length) {
    if (length == 0)
      return Const.EMPTY_INTS;
    int[] copy = new int[length];
    System.arraycopy(array, offset, copy, 0, length);
    return copy;
  }

  public static byte[] arrayCopy(byte[] array, int offset, int length) {
    if (length == 0)
      return Const.EMPTY_BYTES;
    byte[] copy = new byte[length];
    System.arraycopy(array, offset, copy, 0, length);
    return copy;
  }

  public static <T> T[] arrayCopy(T[] array) {
    if (array == null || array.length == 0)
      return array;
    return arrayCopy(array, 0, array.length);
  }

  public static <T> T[] arrayCopy(T[] array, int offset, int length) {
    if (array == null)
      return array;
    assert length != 0; // Empty array required
    assert offset >= 0 : offset;
    assert length >= 0 : length;
    assert array.length >= offset + length : array.length + " " + offset + " " + length;
    //noinspection unchecked
    T[] copy = allocSameClassArray(array, length);
    System.arraycopy(array, offset, copy, 0, length);
    return copy;
  }

  private static <T> T[] allocSameClassArray(T[] array, int length) {
    return (T[]) Array.newInstance(array.getClass().getComponentType(), length);
  }

  public static long[] arrayCopy(long[] array) {
    return arrayCopy(array, 0, array.length);
  }

  public static long[] arrayCopy(long[] array, int offset, int length) {
    if (length == 0)
      return Const.EMPTY_LONGS;
    long[] copy = new long[length];
    System.arraycopy(array, offset, copy, 0, length);
    return copy;
  }

  public static boolean isSorted(long[] array) {
    return isSorted(array, 0, array.length);
  }

  public static void reverse(int[] a) {
    for (int i = 0, j = a.length - 1; i < j; i++, j--) {
      int t = a[i];
      a[i] = a[j];
      a[j] = t;
    }
  }

  public static <T> int indexOf(T[] array, T value) {
    return indexOf(array, 0, array.length, value);
  }

  public static int indexOf(Object[] array, int from, int to, Object value) {
    for (int i = from; i < to; i++) {
      if (Util.equals(array[i], value))
        return i;
    }
    return -1;
  }

  public static long[] join(long[]... arrays) {
    if (arrays.length == 0)
      return Const.EMPTY_LONGS;
    int singleIndex = -1;
    int total = 0;
    for (int i = 0; i < arrays.length; i++) {
      if (arrays[i] != null && arrays[i].length > 0) {
        if (singleIndex == -1)
          singleIndex = i;
        else
          singleIndex = -2;
        total += arrays[i].length;
      }
    }
    if (singleIndex >= 0)
      return arrays[singleIndex];
    long[] result = new long[total];
    int offset = 0;
    for (int i = 0; i < arrays.length; i++) {
      if (arrays[i] != null && arrays[i].length > 0) {
        int length = arrays[i].length;
        System.arraycopy(arrays[i], 0, result, offset, length);
        offset += length;
      }
    }
    return result;
  }

  public static int[] join(int[]... arrays) {
    if (arrays.length == 0)
      return Const.EMPTY_INTS;
    int singleIndex = -1;
    int total = 0;
    for (int i = 0; i < arrays.length; i++) {
      if (arrays[i] != null && arrays[i].length > 0) {
        if (singleIndex == -1)
          singleIndex = i;
        else
          singleIndex = -2;
        total += arrays[i].length;
      }
    }
    if (singleIndex >= 0)
      return arrays[singleIndex];
    int[] result = new int[total];
    int offset = 0;
    for (int i = 0; i < arrays.length; i++) {
      if (arrays[i] != null && arrays[i].length > 0) {
        int length = arrays[i].length;
        System.arraycopy(arrays[i], 0, result, offset, length);
        offset += length;
      }
    }
    return result;
  }

  public static <T> T[] prepend(T[] array, T... objs) {
    if (objs == null || objs.length == 0)
      return arrayCopy(array);
    T[] result = allocSameClassArray(array, array.length + objs.length);
    System.arraycopy(objs, 0, result, 0, objs.length);
    System.arraycopy(array, 0, result, objs.length, array.length);
    return result;
  }

  public static String toString(int[] ints) {
    if (ints == null || ints.length == 0)
      return "[]";
    StringBuilder builder = new StringBuilder("[");
    String sep = "";
    for (int i : ints) {
      builder.append(sep);
      builder.append(i);
      sep = ",";
    }
    builder.append("]");
    return builder.toString();
  }

  public static String toString(long[] longs) {
    if (longs == null || longs.length == 0)
      return "[]";
    StringBuilder builder = new StringBuilder("[");
    String sep = "";
    for (long l : longs) {
      builder.append(sep);
      builder.append(l);
      sep = ",";
    }
    builder.append("]");
    return builder.toString();
  }

  public static String toString(byte[] bytes) {
    if (bytes == null || bytes.length == 0)
      return "[]";
    StringBuilder builder = new StringBuilder("[");
    appendToString(builder, bytes);
    builder.append("]");
    return builder.toString();
  }

  private static void appendToString(StringBuilder builder, byte[] bytes) {
    String sep = "";
    for (byte i : bytes) {
      builder.append(sep);
      builder.append(Integer.toHexString(i));
      sep = ",";
    }
  }

  public static String toString(byte[][] bytes) {
    StringBuilder builder = new StringBuilder("[");
    String sep = "[";
    for (byte[] array : bytes) {
      builder.append(sep);
      sep = ", [";
      appendToString(builder, array);
      builder.append(']');
    }
    builder.append(']');
    return builder.toString();
  }

  public static <T> T[] collectionToArray(@NotNull Collection<? extends T> collection, T[] emptyArray) {
    return collection.isEmpty() ? emptyArray : collection.toArray(reallocArray(emptyArray, collection.size(), false));
  }

  public static int[] copyWithoutIndex(int[] array, int index) {
    int[] result = new int[array.length - 1];
    System.arraycopy(array, 0, result, 0, index);
    if (index == array.length - 1)
      return result;
    System.arraycopy(array, index + 1, result, index, result.length - index);
    return result;
  }

  public static long[] copyWithoutIndex(long[] array, int index) {
    long[] result = new long[array.length - 1];
    System.arraycopy(array, 0, result, 0, index);
    if (index == array.length - 1)
      return result;
    System.arraycopy(array, index + 1, result, index, result.length - index);
    return result;
  }

  public static <T> T[] copyWithoutIndex(T[] array, int index) {
    if (index < 0 || array.length == 0) return array;
    T[] result = allocSameClassArray(array, array.length - 1);
    System.arraycopy(array, 0, result, 0, index);
    if (index == array.length - 1)
      return result;
    System.arraycopy(array, index + 1, result, index, result.length - index);
    return result;
  }

  public static int[] prepend(int[] array, int... ints) {
    int[] result = new int[array.length + ints.length];
    System.arraycopy(array, 0, result, 0, array.length);
    System.arraycopy(ints, 0, result, array.length, ints.length);
    return result;
  }

  public static int removeSubsequentDuplicates(int[] array, int offset, int length) {
    if (length < 2)
      return length;
    int index = offset;
    length += offset;
    while (index < length - 1) {
      int prev = array[index];
      int seqEnd = index + 1;
      while (seqEnd < length && prev == array[seqEnd])
        seqEnd++;
      if (seqEnd > index + 1)
        System.arraycopy(array, seqEnd, array, index + 1, length - seqEnd);
      length -= seqEnd - index - 1;
      index++;
    }
    return length - offset;
  }

  public static int removeSubsequentDuplicates(long[] array, int offset, int length) {
    if (length < 2)
      return length;
    int index = offset;
    length += offset;
    while (index < length - 1) {
      long prev = array[index];
      int seqEnd = index + 1;
      while (seqEnd < length && prev == array[seqEnd])
        seqEnd++;
      if (seqEnd > index + 1)
        System.arraycopy(array, seqEnd, array, index + 1, length - seqEnd);
      length -= seqEnd - index - 1;
      index++;
    }
    return length - offset;
  }

  public static int removeSubsequentDuplicates(Object[] array, int offset, int length) {
    if (length < 2)
      return length;
    int index = offset;
    length += offset;
    while (index < length - 1) {
      Object prev = array[index];
      int seqEnd = index + 1;
      while (seqEnd < length && Util.equals(prev, array[seqEnd]))
        seqEnd++;
      if (seqEnd > index + 1)
        System.arraycopy(array, seqEnd, array, index + 1, length - seqEnd);
      length -= seqEnd - index - 1;
      index++;
    }
    return length - offset;
  }

  public static int[] indexArray(int startingIndex, int length) {
    int[] indices = new int[length];
    for (int i = 0; i < length; i++)
      indices[i] = startingIndex + i;
    return indices;
  }

  /**
   * Search for index of value. the hintIndex is hint where the value is expected to be.
   * Performs paralle search for value in two directions starting at given hint
   *
   * @param ints      array to search in
   * @param length    number of used elements in array
   * @param value     the value to search for
   * @param hintIndex the hint where to start search
   * @return index such that <code>ints[index]==value</code> and <code>index in [0,length)</code> or -1 if no such index exists.
   */
  public static int indexOfNear(int[] ints, int length, int value, int hintIndex) {
    if (hintIndex < length && ints[hintIndex] == value)
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

  public static int sum(int[] buffer, int from, int to) {
    int r = 0;
    for (int i = from; i < to; i++)
      r += buffer[i];
    return r;
  }

  public static long sum(long[] buffer) {
    return sum(buffer, 0, buffer.length);
  }

  public static long sum(long[] buffer, int from, int to) {
    long r = 0;
    for (int i = from; i < to; i++)
      r += buffer[i];
    return r;
  }

  public static int sum(byte[] buffer) {
    return sum(buffer, 0, buffer.length);
  }

  public static int sum(byte[] buffer, int from, int to) {
    int r = 0;
    for (int i = from; i < to; i++)
      r += buffer[i];
    return r;
  }

  public static double sum(double[] buffer) {
    return sum(buffer, 0, buffer.length);
  }

  public static double sum(double[] buffer, int from, int to) {
    double r = 0;
    for (int i = from; i < to; i++)
      r += buffer[i];
    return r;
  }

  public static int binarySearch(int val, int[] array) {
    return binarySearch(val, array, 0, array.length);
  }

  public static boolean equalDifferentElements(@NotNull Object[] array, @NotNull List<?> list) {
    if (array.length != list.size())
      return false;
    for (int i = 0; i < array.length; i++) {
      Object listValue = list.get(i);
      if (!contains(array, listValue))
        return false;
    }
    return true;
  }

  public static boolean contains(@NotNull Object[] array, Object value) {
    for (Object o : array)
      if (Util.equals(o, value))
        return true;
    return false;
  }

  public static boolean equals(byte[] array1, byte[] array2) {
    if (array1 == array2)
      return true;
    if (array1 == null || array2 == null)
      return false;
    if (array1.length != array2.length)
      return false;
    return Arrays.equals(array1, array2);
  }

  public static <T> int binarySearch(T[] a, int fromIndex, int toIndex, T key, Comparator<? super T> c) {
    if (c == null) {
        return binarySearch(a, fromIndex, toIndex, key);
    }
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
        int mid = (low + high) >>> 1;
        T midVal = a[mid];
        int cmp = c.compare(midVal, key);

        if (cmp < 0)
            low = mid + 1;
        else if (cmp > 0)
            high = mid - 1;
        else
            return mid; // key found
    }
    return -(low + 1);  // key not found.
  }

  public static <T> int binarySearch(T[] a, int fromIndex, int toIndex, T key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
        int mid = (low + high) >>> 1;
        Comparable midVal = (Comparable)a[mid];
        int cmp = midVal.compareTo(key);

        if (cmp < 0)
            low = mid + 1;
        else if (cmp > 0)
            high = mid - 1;
        else
            return mid; // key found
    }
    return -(low + 1);  // key not found.
  }

  public static <T> T[] copyWith(T[] array, T element) {
    T[] newArray = reallocArray(array, array.length + 1, true);
    newArray[array.length] = element;
    return newArray;
  }

  public static <T> long[] copyWith(long[] array, long element) {
    long[] newArray = reallocArray(array, array.length + 1, true);
    newArray[array.length] = element;
    return newArray;
  }

  /**
   * Inserts element into array at specified index. Array elements at positions greater than index are shifted right up to
   * element at position size (exclusive).<br>
   * Before insertion ensures that the array has capacity atleast size+1
   * @return the array or new copy if array is reallocated
   */
  public static <T> T[] insert(T[] array, int size, int index, T element) {
    array = ArrayUtil.ensureCapacity(array, size + 1);
    System.arraycopy(array, index, array, index + 1, size - index);
    array[index] = element;
    return array;
  }

  public static <T> long[] insert(long[] array, int size, int index, long element) {
    array = ArrayUtil.ensureCapacity(array, size + 1);
    System.arraycopy(array, index, array, index + 1, size - index);
    array[index] = element;
    return array;
  }

  /**
   * @see #removeSubsequentEquals(Object[], int, int, java.util.Comparator)
   */
  public static <T> int removeSubsequentEquals(T[] array, Comparator<? super T> comparator) {
    if (array == null)
      return 0;
    return removeSubsequentEquals(array, 0, array.length, comparator);
  }

  /**
   * Removes subsequent elements if comparator returns 0. If array is sorted this removes equal (by comparator) elemts.
   * When sequence of equal elements is found only first is kept others are removed.
   * @param array element to filter
   * @param offset index to start from (inclusive)
   * @param endIndex index to proceed to (exclusive)
   * @param comparator comparator to compare with
   * @return number of elements kept. New length of the array
   */
  public static <T> int removeSubsequentEquals(T[] array, int offset, int endIndex, Comparator<? super T> comparator) {
    if (array == null)
      return 0;
    if (offset + 1 >= endIndex)
      return endIndex - offset;
    int targetIndex = offset + 1;
    T lastElement = array[offset];
    for (int i = offset + 1; i < endIndex; i++) {
      T current = array[i];
      if (comparator.compare(lastElement, current) == 0)
        continue;
      lastElement = current;
      array[targetIndex] = current;
      targetIndex++;
    }
    return targetIndex - offset;
  }

  public static <T> int removeSubsequentEqualDuplicates(T[] array, int offset, int length) {
    if (length < 2)
      return length;
    int index = offset;
    length += offset;
    while (index < length - 1) {
      T prev = array[index];
      int seqEnd = index + 1;
      while (seqEnd < length && Util.equals(prev, array[seqEnd]))
        seqEnd++;
      if (seqEnd > index + 1)
        System.arraycopy(array, seqEnd, array, index + 1, length - seqEnd);
      length -= seqEnd - index - 1;
      index++;
    }
    return length - offset;
  }

  public static boolean equals(Object[] array1, Object[] array2) {
    if (array1 == array2) return true;
    if (array1 == null || array2 == null) return false;
    return equals(array1, 0, array1.length, array2, 0, array2.length);
  }

  public static void anyArrayToString(StringBuilder builder, Object array) {
    if (array == null || !array.getClass().isArray()) return;
    int length = Array.getLength(array);
    builder.append("[");
    for (int i = 0; i < length; i++) {
      if (i > 0) builder.append(", ");
      Object element = Array.get(array, i);
      if (element == null) builder.append("<null>");
      else if (element.getClass().isArray()) anyArrayToString(builder, element);
      else builder.append(element);
    }
    builder.append("]");
  }

  public static <K, V> void mapToArrays(Map<K, V> map, K[] keys, V[] values) {
    int i = 0;
    for (Map.Entry<K, V> entry : map.entrySet()) {
      keys[i] = entry.getKey();
      values[i] = entry.getValue();
      i++;
    }
  }

  /**
   * Check if both lists have equal elements in the same order (and list's sizes are equal too).<br>
   * Elements are equal if no one is greater than another according to comparator.
   * @param array1 list1
   * @param array2 list2
   * @param comparator comparator to check elements equality.
   * @param <T> element type
   * @return true if lists are equal
   */
  public static <T> boolean arraysEqualsByComparator(T[] array1, T[] array2, Comparator<? super T> comparator) {
    if (array1 == array2) return true;
    if (array1 == null || array2 == null) return false;
    if (array1.length != array2.length) return false;
    for (int i = 0; i < array1.length; i++) if (comparator.compare(array1[i], array2[i]) != 0) return false;
    return true;
  }

  public static <T> boolean listsEqualsByComparator(List<? extends T> list1, List<? extends T> list2, Comparator<? super T> comparator) {
    if (list1 == list2) return true;
    if (list1 == null || list2 == null) return false;
    int size = list1.size();
    if (size != list2.size()) return false;
    for (int i = 0; i < size; i++) if (comparator.compare(list1.get(i), list2.get(i)) != 0) return false;
    return true;
  }
}
