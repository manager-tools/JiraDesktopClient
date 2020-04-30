package com.almworks.util.collections;

import org.almworks.util.ArrayUtil;
import org.almworks.util.Const;
import org.almworks.util.Util;

import java.util.*;

public final class SortedArraySet<T> implements Iterable<T>{
  private final Comparator<? super T> myComparator;
  private T[] myValues = (T[]) Const.EMPTY_OBJECTS;
  private int mySize = 0;

  public SortedArraySet(Comparator<? super T> comparator) {
    myComparator = comparator;
  }

  public int addReturnIndex(T value) {
    int index = getIndex(value);
    if (index < 0) {
      int insert = -index - 1;
      myValues = ArrayUtil.ensureCapacity(myValues, mySize + 1);
      if (insert < mySize)
        System.arraycopy(myValues, insert, myValues, insert + 1, mySize - insert);
      myValues[insert] = value;
      mySize++;
    }
    return index;
  }

  public boolean add(T key) {
    return addReturnIndex(key) < 0;
  }

  public int size() {
    return mySize;
  }

  /**
   * @return not-negative index of the key if the set contains equal (as Object#equals) value or negative insertion point
   */
  public int getIndex(T key) {
    if (mySize < 10) for (int i = 0; i < mySize; i++) if (Util.equals(key, myValues[i])) return i;
    int index = ArrayUtil.binarySearch(myValues, 0, mySize, key, myComparator);
    if (index < 0)
      return index;
    if (Util.equals(myValues[index], key))
      return index;
    int iLow = index - 1;
    while (iLow > 0 && myComparator.compare(myValues[iLow], key) == 0)
      if (Util.equals(myValues[iLow], key))
        return iLow;
      else
        iLow--;
    int iHigh = index + 1;
    while (iHigh < mySize && myComparator.compare(myValues[iHigh], myValues[iHigh]) == 0)
      if (Util.equals(myValues[iHigh], key))
        return iHigh;
      else
        iHigh++;
    return -1 - iHigh;
  }

  public static <T> SortedArraySet<T> create() {
    return new SortedArraySet<T>(Containers.<T>hashCodeComparator());
  }

  public void remove(T value) {
    int index = getIndex(value);
    removeByIndex(index);
  }

  public void removeByIndex(int index) {
    if (index < 0)
      return;
    if (index < mySize -1)
      System.arraycopy(myValues, index + 1, myValues, index, mySize - index - 1);
    myValues[mySize - 1] = null;
    mySize--;
  }

  public void clear() {
    Arrays.fill(myValues, 0, mySize, null);
    mySize = 0;
  }

  public Object[] toArray() {
    if (mySize == 0)
      return Const.EMPTY_OBJECTS;
    Object[] array = new Object[mySize];
    System.arraycopy(myValues, 0, array, 0, mySize);
    return array;
  }

  public void getItems(Collection<? super T> target) {
    for (int i = 0; i < mySize; i++)
      target.add(myValues[i]);
  }

  public List<? extends T> unmodifiableList() {
    return new ROArrayListWrapper<T>(myValues, mySize);
  }

  public static <T> SortedArraySet<T> create(Collection<? extends T> collection) {
    SortedArraySet<T> result = create();
    if (collection == null || collection.isEmpty())
      return result;
    T[] array = (T[]) collection.toArray();
    Arrays.sort(array, result.myComparator);
    int size = ArrayUtil.removeSubsequentEquals(array, 0, array.length, result.myComparator);
    result.myValues = array;
    result.mySize = size;
    return result;
  }

  public T getByIndex(int index) {
    if (index < 0 || index >= mySize)
      throw new IndexOutOfBoundsException("size: " + mySize + " index: " + index);
    return myValues[index];
  }

  public void addAll(List<? extends T> items) {
    if (items.isEmpty()) return;
    myValues = ArrayUtil.ensureCapacity(myValues, mySize + items.size());
    for (int i = 0; i < items.size(); i++) myValues[mySize + i] = items.get(i);
    Arrays.sort(myValues, 0, mySize + items.size(), myComparator);
    mySize = ArrayUtil.removeSubsequentEqualDuplicates(myValues, 0, mySize + items.size());
  }

  public boolean isEmpty() {
    return mySize == 0;
  }

  public Iterator<T> iterator() {
    return ((List<T>) unmodifiableList()).iterator();
  }
}
