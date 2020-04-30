package com.almworks.util.collections;

import org.almworks.util.ArrayUtil;
import org.almworks.util.Const;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings({"ReturnOfNull"})
public final class SortedArrayMap<K, V> {
  private final SortedArraySet<K> myKeys;
  private V[] myValues = (V[]) Const.EMPTY_OBJECTS;

  public SortedArrayMap() {
    this(Containers.<K>hashCodeComparator());
  }

  public SortedArrayMap(Comparator<? super K> comparator) {
    myKeys = new SortedArraySet<K>(comparator);
  }

  public V get(K key) {
    int index = myKeys.getIndex(key);
    return index >= 0 ? myValues[index] : null;
  }

  public int getKeyIndex(K key) {
    return myKeys.getIndex(key);
  }

  public V getValue(int index) {
    checkIndex(index);
    return myValues[index];
  }

  public void putValue(int keyIndex, V value) {
    checkIndex(keyIndex);
    myValues[keyIndex] = value;
  }

  private void checkIndex(int keyIndex) {
    if (keyIndex >= myKeys.size())
      throw new IndexOutOfBoundsException(keyIndex + " " + myKeys.size());
  }

  public void put(K key, V value) {
    int index = myKeys.addReturnIndex(key);
    if (index >= 0)
      myValues[index] = value;
    else {
      index = -index - 1;
      insertAt(index, value);
    }
  }

  public static <K, V> SortedArrayMap<K, V> create() {
    return new SortedArrayMap<K,V>();
  }

  public static <K, V> SortedArrayMap<K, V> create(Comparator<? super K> comparator) {
    assert comparator != null;
    return new SortedArrayMap<K, V>(comparator);
  }

  public static <K extends Comparable, V> SortedArrayMap<K, V> createComparable() {
    return new SortedArrayMap<K,V>(null);
  }

  private void insertAt(int index, V value) {
    int size = myKeys.size();
    myValues = ArrayUtil.ensureCapacity(myValues, size);
    System.arraycopy(myValues, index, myValues, index + 1, size - index - 1);
    myValues[index] = value;
  }

  public void clear() {
    Arrays.fill(myValues, 0, myKeys.size(), null);
    myKeys.clear();
  }

  public List<? extends K> unmodifiableKeys() {
    return myKeys.unmodifiableList();
  }

  public K getKey(int index) {
    return myKeys.getByIndex(index);
  }

  public void remove(int index) {
    if (index < 0) return;
    int size = myKeys.size();
    if (index >= size) throw new IndexOutOfBoundsException(index + " " + size);
    myKeys.removeByIndex(index);
    if (index < size - 1) System.arraycopy(myValues, index + 1, myValues, index, size - index - 1);
    myValues[size - 1] = null;
  }

  public int size() {
    return myKeys.size();
  }
}
