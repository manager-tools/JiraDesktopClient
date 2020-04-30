package com.almworks.util.collections;

import com.almworks.integers.IntIterable;
import com.almworks.integers.IntIterator;
import com.almworks.integers.LongIterable;
import com.almworks.integers.LongIterator;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.IntIntFunction;
import com.almworks.util.commons.IntIntFunction2;
import com.almworks.util.commons.IntProcedure2;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.*;

public class CollectionUtil {

  /**
   * This method transfers a collection of objects from iterator to visitor, according to visitor's contract.
   *
   * @param source      iterator over some set of objects
   * @param destination a visitor that accepts the objects
   * @see BoundedElementVisitor
   */
  public static <E> void transfer(Iterator<? extends E> source, BoundedElementVisitor<? super E> destination) {
    if (source == null || destination == null)
      return;
    destination.startVisit();
    try {
      while (source.hasNext())
        destination.visit(source.next());
    } catch (RuntimeException e) {
      destination.visitException(e);
      throw e;
    } finally {
      destination.endVisit();
    }
  }

  public static int compareArrays(byte[] array1, byte[] array2) {
    int len1 = array1.length;
    int len2 = array2.length;
    int dif = len1 - len2;
    if (dif != 0)
      return dif;
    for (int i = 0; i < len1; i++) {
      dif = array1[i] - array2[i];
      if (dif != 0)
        return dif;
    }
    return 0;
  }

  public static <T> int indexOf(T[] array, T element) {
    for (int i = 0; i < array.length; i++) {
      if (Util.equals(array[i], element))
        return i;
    }
    return -1;
  }

  public static String implode(Collection<String> strings, String delimiter) {
    StringBuffer result = new StringBuffer();
    for (Iterator<String> ii = strings.iterator(); ii.hasNext();) {
      if (result.length() > 0)
        result.append(delimiter);
      result.append(ii.next());
    }
    return result.toString();
  }

  /**
   * Copied from Arrays.binarySearch
   * <p>
   * Searches the specified array for the specified object using the binary
   * search algorithm.  The array must be sorted into ascending order
   * according to the specified comparator (as by the <tt>Sort(Object[],
   * Comparator)</tt> method, above), prior to making this call.  If it is
   * not sorted, the results are undefined.
   * If the array contains multiple
   * elements equal to the specified object, there is no guarantee which one
   * will be found.
   *
   * @param list   the list to be searched.
   * @param comparator  a function that returns value similar to comparator
   * @return index of the search key, if it is contained in the list;
   *         otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
   *         <i>insertion point</i> is defined as the point at which the
   *         key would be inserted into the list: the index of the first
   *         element greater than the key, or <tt>list.size()</tt>, if all
   *         elements in the list are less than the specified key.  Note
   *         that this guarantees that the return value will be &gt;= 0 if
   *         and only if the key is found.
   * @throws ClassCastException if the array contains elements that are not
   *                            <i>mutually comparable</i> using the specified comparator,
   *                            or the search key in not mutually comparable with the
   *                            elements of the array using this comparator.
   * @see Comparable
   * @see Arrays#sort(Object[], Comparator)
   */
  public static <T> int binarySearch(List<T> list, Function<T, Integer> comparator) {
    int low = 0;
    int high = list.size() - 1;

    while (low <= high) {
      int mid = (low + high) >> 1;
      T midVal = list.get(mid);
      int cmp = comparator.invoke(midVal);

      if (cmp < 0)
        low = mid + 1;
      else if (cmp > 0)
        high = mid - 1;
      else
        return mid; // key found
    }
    return -(low + 1);  // key not found.
  }

  public static int binarySearch(int length, IntIntFunction comparator) {
    int low = 0;
    int high = length-1;

    while (low <= high) {
      int mid = (low + high) >> 1;
      int comp = comparator.invoke(mid);

      if (comp < 0)
        low = mid + 1;
      else if (comp > 0)
        high = mid - 1;
      else
        return mid; // key found
    }
    return -(low + 1);  // key not found.
  }


  /**
   * Performs quicksort. Copied from {@link java.util.Arrays#sort(int[])} then corrected.
   * @param length - number of elements to sort
   * @param order - "comparator". Parameter are indexes of elements to be compared. Will be invoked with parameters
   * from 0 to <code>length</code>-1 inclusive.
   * @param swap - procedure to swap. Parameters are indexes of elements to be swapped. Will be invoked with parameters
   * from 0 to <code>length</code>-1 inclusive.   
   */
  public static void quicksort(int length, IntIntFunction2 order, IntProcedure2 swap) {
    sort1(0, length, order, swap);
  }

  private static <T> void sort1(int off, int len, IntIntFunction2 order, IntProcedure2 swap) {
    // Insertion sort on smallest arrays
    if (len < 7) {
      for (int i = off; i < len + off; i++)
        for (int j = i; j > off && order.invoke(j - 1, j) > 0; j--)
          swap.invoke(j, j - 1);
      return;
    }

    // Choose a partition element, v
    int m = off + (len >> 1);       // Small arrays, middle element
    if (len > 7) {
      int l = off;
      int n = off + len - 1;
      if (len > 40) {        // Big arrays, pseudomedian of 9
        int s = len / 8;
        l = med3(l, l + s, l + 2 * s, order);
        m = med3(m - s, m, m + s, order);
        n = med3(n - 2 * s, n - s, n, order);
      }
      m = med3(l, m, n, order); // Mid-size, med of 3
    }

    // Establish Invariant: v* (<v)* (>v)* v*
    int a = off, b = a, c = off + len - 1, d = c;
    while (true) {
      while (b <= c) {
        int comp = order.invoke(b, m);
        if (comp > 0)
          break;
        if (comp == 0) {
          if (a == m)
            m = b;
          else if (b == m)
            m = a;
          swap.invoke(a++, b);
        }
        b++;
      }
      while (c >= b) {
        int comp = order.invoke(c, m);
        if (comp < 0)
          break;
        if (comp == 0) {
          if (c == m)
            m = d;
          else if (d == m)
            m = c;
          swap.invoke(c, d--);
        }
        c--;
      }
      if (b > c)
        break;
      swap.invoke(b++, c--);
    }

    // Swap partition elements back to middle
    int s, n = off + len;
    s = Math.min(a - off, b - a);
    vecswap(off, b - s, s, swap);
    s = Math.min(d - c, n - d - 1);
    vecswap(b, n - s, s, swap);

    // Recursively sort non-partition-elements
    if ((s = b - a) > 1)
      sort1(off, s, order, swap);
    if ((s = d - c) > 1)
      sort1(n - s, s, order, swap);
  }

  private static void vecswap(int a, int b, int n, IntProcedure2 swap) {
    for (int i = 0; i < n; i++, a++, b++)
      swap.invoke(a, b);
  }

  private static <T> int med3(int a, int b, int c, IntIntFunction2 order) {
    return (order.invoke(a, b) < 0 ? (order.invoke(b, c) < 0 ? b : order.invoke(a, c) < 0 ? c : a) :
      (order.invoke(b, c) > 0 ? b : order.invoke(a, c) > 0 ? c : a));
  }


  public static <T> boolean isSorted(List<T> list, Comparator<T> comparator) {
    if (list == null)
      return false;
    int count = list.size();
    if (count < 2)
      return true;
    T val = list.get(0);
    for (int i = 1; i < count; i++) {
      T next = list.get(i);
      if (comparator.compare(val, next) > 0)
        return false;
      val = next;
    }
    return true;
  }

  public static <K, V> K getKeyByValue(Map<K, ? super V> map, V value) {
    for(final Map.Entry<K, ? super V> e : map.entrySet()) {
      if(value == null) {
        if(e.getValue() == null) {
          return e.getKey();
        }
      } else {
        if(value.equals(e.getValue())) {
          return e.getKey();
        }
      }
    }
    return null;
  }

  public static String stringJoin(Iterable<?> collection, String separator) {
    if(separator == null) {
      separator = "";
    }

    final StringBuilder sb = new StringBuilder();
    for(final Object o : collection) {
      sb.append(String.valueOf(o)).append(separator);
    }

    final int length = sb.length();
    if(length > 0) {
      sb.setLength(length - separator.length());
    }

    return sb.toString();
  }

  /**
   * Creates and returns a {@link java.util.HashSet}, containing
   * all elements of the given collections.
   * @param cols Source collections.
   * @param <T> Element type.
   * @return The set containing the union of {@code cols}.
   */
  public static <T> Set<T> setUnion(Collection<? extends T>... cols) {
    final Set<T> result = Collections15.hashSet();
    for(final Collection<? extends T> col : cols) {
      result.addAll(col);
    }
    return result;
  }

  public static List<Long> collectLongs(LongIterable source) {
    LongIterator it = source.iterator();
    List<Long> result = Collections15.arrayList();
    while (it.hasNext()) result.add(it.nextValue());
    return result;
  }

  public static List<Integer> collectInts(IntIterable source) {
    IntIterator it = source.iterator();
    List<Integer> result = Collections15.arrayList();
    while (it.hasNext()) result.add(it.nextValue());
    return result;
  }
}
