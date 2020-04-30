package com.almworks.util.collections;

import java.util.Comparator;

public class NullTolerantComparator<T> implements Comparator<T> {
  private final Comparator<? super T> myIntolerantComparator;
  private final boolean myNullsLess;

  public NullTolerantComparator(Comparator<? super T> intolerantComparator, boolean nullsLess) {
    myIntolerantComparator = intolerantComparator;
    myNullsLess = nullsLess;
  }

  public static <T> NullTolerantComparator<T> nullsFirst(Comparator<T> intolerantComparator) {
    return new NullTolerantComparator<T>(intolerantComparator, true);
  }

  public static <T> NullTolerantComparator<T> nullsLast(Comparator<T> intolerantComparator) {
    return new NullTolerantComparator<T>(intolerantComparator, false);
  }

  /**
   * Compares two objects assuming that at least one of them is null. <br>
   * If both objects are not null returns 0.<br>
   * Expected usage:<br>
   * <code>
   * if (a == null || b == null) return compareWithNull(a, b, nullsLess); <br>
   * ... // compare not nulls
   * </code>
   * @param nullsLess result of comparison null to notNull, when true null values are less then not null
   * @return 0 if both objects are null or both are not null or not zero result of comparison null to notNull.
   */
  public static <T> int compareWithNull(T o1, T o2, boolean nullsLess) {
    if (o1 == null) return o2 == null ? 0 : (nullsLess ? -1 : 1);
    else if (o2 == null) return -(nullsLess ? -1 : 1);
    else return 0;
  }

  public int compare(T o1, T o2) {
    if (o1 == null || o2 == null) return compareWithNull(o1, o2, myNullsLess);
    else return myIntolerantComparator.compare(o1, o2);
  }
}
