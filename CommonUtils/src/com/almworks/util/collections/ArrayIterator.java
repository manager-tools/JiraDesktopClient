package com.almworks.util.collections;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class ArrayIterator<T> implements ListIterator<T> {
  private final T[] myArray;
  private final int myStart;
  private final int myEnd;
  private final boolean myReadOnly;
  private int myNext;
  private int myCurrent = -1;

  public ArrayIterator(T[] array, int start, int end, boolean readOnly) {
    myArray = array;
    myStart = start;
    myEnd = end;
    myReadOnly = readOnly;
    myNext = myStart;
  }

  public static <T> ListIterator<T> readonly(T[] array) {
    return readonly(array, 0, array.length);
  }

  public static <T> ListIterator<T> readonly(T[] array, int start, int end) {
    return new ArrayIterator<T>(array, start, end, true);
  }

  @Override
  public boolean hasNext() {
    return myNext < myEnd;
  }

  @Override
  public T next() {
    if (!hasNext()) throw new NoSuchElementException(this.toString());
    myNext++;
    myCurrent = myNext - 1;
    return myArray[myCurrent];
  }

  @Override
  public boolean hasPrevious() {
    return myNext > myStart + 1;
  }

  @Override
  public T previous() {
    if (!hasPrevious()) throw new NoSuchElementException(this.toString());
    myNext--;
    myCurrent = myNext - 1;
    return myArray[myCurrent];
  }

  @Override
  public int nextIndex() {
    return myNext - myStart;
  }

  @Override
  public int previousIndex() {
    return Math.max(-1, myNext - 1 - myStart);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException(this.toString());
  }

  @Override
  public void set(T t) {
    if (myReadOnly) throw new UnsupportedOperationException(this.toString());
    if (myCurrent < 0) throw new IllegalStateException(this.toString());
    myArray[myCurrent] = t;
  }

  @Override
  public void add(T t) {
    throw new UnsupportedOperationException(this.toString());
  }

  @Override
  public String toString() {
    return "ArrayIt[" + myStart + ", " + myEnd + ")@" + myNext + (myReadOnly ? "" : "@" + myCurrent);
  }
}
