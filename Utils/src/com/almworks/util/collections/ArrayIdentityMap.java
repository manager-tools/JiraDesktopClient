package com.almworks.util.collections;

import org.almworks.util.ArrayUtil;

/**
 * @author dyoma
 */
@SuppressWarnings({"unchecked"})
public class ArrayIdentityMap<K, V> {
  private Object[] myKeys;
  private Object[] myValues;
  private int mySize = 0;

  public ArrayIdentityMap(int initialSize) {
    myKeys = new Object[initialSize];
    myValues = new Object[initialSize];
  }

  public V get(K key) {
    for (int i = 0; i < mySize; i++)
      if (key == myKeys[i])
        return (V) myValues[i];
    return null;
  }

  public V put(K key, V value) {
    int index = identityIndexOf(myKeys, mySize, key);
    if (index >= 0) {
      V old = (V) myValues[index];
      myValues[index] = value;
      return old;
    }
    ensureCapasity(mySize + 1);
    myKeys[mySize] = key;
    myValues[mySize] = value;
    mySize++;
    //noinspection ConstantConditions
    return null;
  }

  public V remove(K key) {
    int index = identityIndexOf(myKeys, mySize, key);
    if (index < 0)
      //noinspection ConstantConditions
      return null;
    V old = (V) myValues[index];
    if (index == mySize - 1) {
      mySize--;
      return old;
    }
    System.arraycopy(myKeys, index + 1, myKeys, index, mySize - index - 1);
    System.arraycopy(myValues, index + 1, myValues, index, mySize - index - 1);
    mySize--;
    return old;
  }

  private void ensureCapasity(int capacity) {
    assert myKeys.length == myValues.length;
    if (myKeys.length >= capacity)
      return;
    myKeys = ArrayUtil.ensureCapacity(myKeys, capacity);
    if (myValues.length != myKeys.length)
      myValues = ArrayUtil.reallocArray(myValues, myKeys.length, true);
    assert myKeys.length == myValues.length;
  }

  public int size() {
    return mySize;
  }

  public static <K, V> ArrayIdentityMap<K, V> create(int initialSize) {
    return new ArrayIdentityMap<K,V>(initialSize);
  }

  public boolean isEmpty() {
    return mySize == 0;
  }

  public boolean containsKey(K key) {
    return identityIndexOf(myKeys, mySize, key) >= 0;
  }

  public static <T> int identityIndexOf(T[] array, int size, T value) {
    for (int i = 0; i < size; i++)
      if (value == array[i])
        return i;
    return -1;
  }
}
