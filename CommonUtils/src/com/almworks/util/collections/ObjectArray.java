package com.almworks.util.collections;

import org.almworks.util.ArrayUtil;

import java.util.Arrays;

/**
 * @author dyoma
 */
public class ObjectArray<T> {
  private T[] myArray = null;
  private int mySize = 0;

  public T get(int index) {
    checkIndex(index);
    return myArray[index];
  }

  public void set(int index, T item) {
    checkIndex(index);
    myArray[index] = item;
  }

  public void insertRange(int index, int length, T fill) {
    if (length < 0)
      throw new IllegalArgumentException(String.valueOf(length));
    checkPosition(index);
    if (length == 0)
      return;
    ensureCapasity(mySize + length);
    if (mySize > index) {
      System.arraycopy(myArray, index, myArray, index + length, mySize - index);
    }
    if (length == 1) {
      myArray[index] = fill;
    } else {
      Arrays.fill(myArray, index, index + length, fill);
    }
    mySize += length;
  }

  public void removeRange(int index, int length) {
    if (length < 0)
      throw new IllegalArgumentException(String.valueOf(length));
    checkPosition(index + length);
    if (length == 0)
      return;
    int aliveIndex = index + length;
    System.arraycopy(myArray, aliveIndex, myArray, index, mySize - aliveIndex);
    int newSize = mySize - length;
    Arrays.fill(myArray, newSize, mySize, null);
    mySize = newSize;
  }

  public int size() {
    return mySize;
  }

  public T[] toArray(T[] array) {
    array = ArrayUtil.reallocArray(array, mySize, false);
    System.arraycopy(myArray, 0, array, 0, mySize);
    return array;
  }

  public int capacity() {
    return myArray != null ? myArray.length : 0;
  }

  private void ensureCapasity(int desiredSize) {
    if (myArray != null && myArray.length >= desiredSize)
      return;
    desiredSize = Math.max(mySize * 2, desiredSize);
    T[] newArray = (T[]) new Object[desiredSize];
    if (myArray != null)
      System.arraycopy(myArray, 0, newArray, 0, mySize);
    myArray = newArray;
  }

  private void checkIndex(int index) {
    if (mySize <= index || index < 0)
      throw new IndexOutOfBoundsException(String.valueOf(index) + " "+ size());
  }

  private void checkPosition(int position) {
    if (mySize < position || position < 0)
      throw new IndexOutOfBoundsException(String.valueOf(position) + " " + size());
  }

  T priGet(int i) {
    return myArray[i];
  }

  public void add(T item) {
    insertRange(mySize, 1, item);
  }

  public static <T> ObjectArray<T> create() {
    return new ObjectArray<T>();
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public void removeTail(int newSize) {
    removeRange(newSize, mySize - newSize);
  }

  public void clear() {
    mySize = 0;
    myArray = null;
  }
}
