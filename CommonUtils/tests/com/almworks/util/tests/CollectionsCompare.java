package com.almworks.util.tests;

import com.almworks.integers.LongList;
import com.almworks.util.collections.Comparing;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Convertors;
import com.almworks.util.text.TextUtil;
import junit.framework.Assert;
import junit.framework.ComparisonFailure;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Util;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author : Dyoma
 */
public class CollectionsCompare {
  private final Convertor<Object, String> myToString;
  private static final Convertor<Object,String> COMPONENT_STRING = new Convertor<Object, String>() {
    public String convert(Object value) {
      if (value instanceof Component)
        return ((Component) value).getName() + ": " + value.toString();
      return String.valueOf(value);
    }
  };

  public CollectionsCompare() {
    this(Convertors.<Object>getToString());
  }

  public CollectionsCompare(Convertor<Object, String> toString) {
    myToString = toString;
  }

  public void singleElement(Object element, Collection<?> collection) {
    Assert.assertNotNull("Collection is null", collection);
    int size = collection.size();
    if (size == 1 && Util.equals(collection.iterator().next(), element)) return;
    Failure failure = new Failure(myToString);
    failure.actual().setCollection(collection);
    failure.expected().setElement(element);
    if (size == 0) failure.fail("Empty collection");
    if (size > 1) failure.fail("Expected 1 element but was " + size);
    failure.fail();
  }

  public void order(Object[] expected, List<?> actual) {
    order(Arrays.asList(expected), actual);
  }

  public void order(Object[] expected, Object[] actual) {
    order(Arrays.asList(expected), Arrays.asList(actual));
  }

  public <T> void order(List<T> actual, T ... expected) {
    order(Arrays.asList(expected), actual);
  }

  public void order(List<?> expected, List<?> actual) {
    Assert.assertNotNull("Expected in null", expected);
    Assert.assertNotNull("Collection in null", actual);
    if (Comparing.areOrdersEqual(expected, actual)) return;
    Failure failure = new Failure(myToString);
    failure.expected().setCollection(expected);
    failure.actual().setCollection(actual);
    int expectedSize = expected.size();
    int actualSize = actual.size();
    if (expectedSize != actualSize)
      failure.failSizes();
    else
      failure.fail("Elements aren't equal");
  }

  public void order(double[] actual, double ... expected) {
    Assert.assertNotNull("Actual is null", actual);
    Assert.assertNotNull("Expected is null", expected);
    if (Arrays.equals(actual, expected)) return;
    Failure failure = new Failure(myToString);
    failure.actual().setDoubleArray(actual);
    failure.expected().setDoubleArray(expected);
    if (actual.length != expected.length)
      failure.failSizes();
    else
      failure.fail("Doubles aren't equal");
  }

  public void order(int[] actual, int ... expected) {
    Assert.assertNotNull("Actual is null", actual);
    Assert.assertNotNull("Expected is null", expected);
    if (Arrays.equals(actual, expected)) return;
    Failure failure = new Failure(myToString);
    failure.actual().setIntArray(actual);
    failure.expected().setIntArray(expected);
    if (actual.length != expected.length)
      failure.failSizes();
    else
      failure.fail("Ints aren't equal");
  }

  public void order(LongList actual, long ... expected) {
    order(actual.toNativeArray(), expected);
  }

  public void order(LongList expected, LongList actual) {
    order(actual.toNativeArray(), expected.toNativeArray());
  }

  public void order(long[] actual, long ... expected) {
    Assert.assertNotNull("Actual is null", actual);
    Assert.assertNotNull("Expected is null", expected);
    if (Arrays.equals(actual, expected)) return;
    Failure failure = new Failure(myToString);
    failure.actual().setLongArray(actual);
    failure.expected().setLongArray(expected);
    if (actual.length != expected.length)
      failure.failSizes();
    else
      failure.fail("Ints aren't equal");
  }

  public void order(byte[] actual, byte ... expected) {
    Assert.assertNotNull("Actual is null", actual);
    Assert.assertNotNull("Expected is null", expected);
    if (Arrays.equals(actual, expected)) return;
    Failure failure = new Failure(myToString);
    failure.actual().setByteArray(actual);
    failure.expected().setByteArray(expected);
    if (actual.length != expected.length)
      failure.failSizes();
    else
      failure.fail("Bytes aren't equal");
  }

  public void singleElement(Object element, Enumeration<?> actual) {
    singleElement(element, Containers.collectList(actual));
  }

  public void unordered(Object[] expected, Enumeration<?> actual) {
    unordered(Containers.collectList(actual), expected);
  }

  public void unordered(Collection<?> actual, Object ... expected) {
    ArrayList<?> expectedList = new ArrayList(Arrays.asList(expected));
    ArrayList<?> actualList = new ArrayList(actual);
    unordered(expectedList, actualList);
  }

  public void unordered(Object[] actual, Object ... expected) {
    if (expected == null)
      expected = Const.EMPTY_OBJECTS;
    if (actual == null)
      actual = Const.EMPTY_OBJECTS;
    unordered(Arrays.asList(expected), Arrays.asList(actual));
  }

  public void unordered(List<?> expectedList, Collection<?> collection) {
    unordered(expectedList, Containers.collectList(collection.iterator()));
  }

  public void unordered(Collection<?> expected, Collection<?> actual) {
    unordered(Collections15.arrayList(expected), actual);
  }

  public void unordered(List<?> expectedList, List<?> actualList) {
    sort(expectedList);
    sort(actualList);
    order(expectedList, actualList);
  }

  public void unordered(Object[] expected, Iterator<?> actual) {
    unordered(Containers.collectList(actual), expected);
  }

  public void unordered(long[] actual, long ... expected) {
    Assert.assertNotNull("Actual is null", actual);
    Assert.assertNotNull("Expected is null", expected);
    actual = ArrayUtil.arrayCopy(actual);
    expected = ArrayUtil.arrayCopy(expected);
    Arrays.sort(actual);
    Arrays.sort(expected);
    order(actual, expected);
  }

  public void unordered(int[] actual, int ... expected) {
    Assert.assertNotNull("Actual is null", actual);
    Assert.assertNotNull("Expected is null", expected);
    actual = ArrayUtil.arrayCopy(actual);
    expected = ArrayUtil.arrayCopy(expected);
    Arrays.sort(actual);
    Arrays.sort(expected);
    order(actual, expected);
  }

  private static void sort(List<?> list) {
    Collections.sort(list, new Comparator() {
      public int compare(Object o1, Object o2) {
        if ((o1 instanceof Comparable) && (o2 instanceof Comparable))
          return ((Comparable<? super Comparable<?>>) o1).compareTo((Comparable<?>)o2);
        return Util.compareInts(getHashCode(o1), getHashCode(o2));
      }
    });
  }

  private static int getHashCode(Object o) {
    return o != null ? o.hashCode() : 0;
  }

  public void order(Object[] expected, Enumeration<?> actual) {
    order(expected, Containers.collectList(actual));
  }

  public void order(Object[] objects, Iterator<?> iterator) {
    order(objects, Containers.collectList(iterator));
  }

  public void singleElement(Object expected, Iterator<?> actual) {
    singleElement(expected, Containers.collectList(actual));
  }

  public void singleElement(Object expected, Object[] actual) {
    singleElement(expected, Arrays.asList(actual));
  }

  public void singleElement(long expected, long[] actual) {
    order(actual, expected);
  }

  public void empty(Collection<?> collection) {
    order(Const.EMPTY_OBJECTS, Collections.enumeration(collection));
  }

  public void empty(Enumeration<?> enumeration) {
    empty(Containers.collectList(enumeration));
  }

  public void empty(Object[] array) {
    empty(array != null ? Arrays.asList(array) : Collections15.<Object>emptyList());
  }

  public void empty(LongList actual) {
    order(actual);
  }

  public void size(int expectedSize, Collection<?> collection) {
    if (expectedSize == collection.size())
      return;
    if (expectedSize == 0)
      empty(collection);
    Failure failure = new Failure(myToString);
    failure.expected().setSize(expectedSize);
    failure.actual().setCollection(collection);
    failure.failSizes();
  }

  public <T> void contains(T element, T[] array) {
    contains(element, Arrays.asList(array));
  }

  public <T> void contains(T element, Collection<T> collection) {
    if (collection.contains(element))
      return;
    Failure failure = new Failure(myToString);
    failure.expected().setElement(element);
    failure.actual().setCollection(collection);
    failure.fail("Expected contains");
  }

  public static String toString(int[] ints) {
    StringBuilder builder = new StringBuilder("[");
    String sep = "";
    for (int i : ints) {
      builder.append(sep);
      builder.append(i);
      sep = ", ";
    }
    builder.append("]");
    return builder.toString();
  }

  public static CollectionsCompare createForComponents() {
    return new CollectionsCompare(COMPONENT_STRING);
  }

  public void bits(BitSet actual, int ... expected) {
    int expectedSize = expected.length;
    while (expectedSize > 0 && expected[expectedSize - 1] == 0)
      expectedSize--;
    if (expectedSize != expected.length) {
      int[] tmp = new int[expectedSize];
      System.arraycopy(expected, 0, tmp, 0, expectedSize);
      expected = tmp;
    }
    int[] actualInts = new int[actual.length()];
    for (int i = 0; i < actualInts.length; i++)
      actualInts[i] = actual.get(i) ? 1 : 0;
    order(actualInts, expected);
  }

  private static class FailureSide {
    private final Convertor<Object, String> myToString;
    private String myString;
    private int mySize = -1;

    private FailureSide(Convertor<Object, String> toString) {
      myToString = toString;
    }

    public void setCollection(Collection<?> collection) {
      mySize = collection.size();
      myString = toText(collection);
    }

    public void setIntArray(int[] array) {
      mySize = array.length;
      myString = toText(array);
    }

    public void setLongArray(long[] array) {
      mySize = array.length;
      myString = toText(array);
    }

    public void setByteArray(byte[] array) {
      mySize = array.length;
      myString = toText(array);
    }

    public void setDoubleArray(double[] array) {
      mySize = array.length;
      myString = toText(array);
    }

    public void setElement(Object element) {
      mySize = 1;
      myString = elementToText(element);
    }

    public void setArray(Object[] expected) {
      mySize = expected.length;
      setCollection(Arrays.asList(expected));
    }

    private String toText(Collection<?> collection) {
      StringBuffer result = new StringBuffer();
      String separator = "";
      for (Object o : collection) {
        result.append(separator);
        separator = "\n";
        result.append(elementToText(o));
      }
      return result.toString();
    }

    private String toText(int[] array) {
      return TextUtil.separate(array, 0, array.length, "\n");
    }

    private String toText(double[] array) {
      return TextUtil.separate(Arrays.stream(array).boxed().iterator(), "\n", Convertors.getToString());
    }

    private String toText(long[] array) {
      return TextUtil.separate(array, 0, array.length, "\n");
    }

    private String toText(byte[] array) {
      return TextUtil.separate(array, 0, array.length, "\n");
    }

    private String toText(BitSet bitSet) {
      StringBuilder builder = new StringBuilder();
      String sep = "";
      for (int i = 0; i < bitSet.length(); i++) {
        builder.append(sep);
        sep = "\n";
        builder.append(bitSet.get(i) ? 1 : 0);
      }
      return builder.toString();
    }

    private String elementToText(Object o) {
      return myToString.convert(o);
    }

    public int getSize() {
      return mySize;
    }

    public String getString() {
      return myString;
    }

    public void setSize(int size) {
      mySize = size;
      myString = "";
    }

    public void setBitSet(BitSet bitSet) {
      mySize = bitSet.length();
      myString = toText(bitSet);
    }
  }

  private static class Failure {
    private final FailureSide myExpected;
    private final FailureSide myActual;

    private Failure(Convertor<Object, String> toString) {
      myExpected = new FailureSide(toString);
      myActual = new FailureSide(toString);
    }

    public FailureSide expected() {
      return myExpected;
    }

    public FailureSide actual() {
      return myActual;
    }

    public void fail(String message) {
      throw new ComparisonFailure(message, myExpected.getString(), myActual.getString());
    }

    public void failSizes() {
      fail("Size mismatch. Expected <" + expected().getSize() + "> but was <" + actual().getSize() + ">");
    }

    public void fail() {
      fail("");
    }
  }
}
