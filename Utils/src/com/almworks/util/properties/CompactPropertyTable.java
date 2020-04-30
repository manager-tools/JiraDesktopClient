package com.almworks.util.properties;

import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

/**
 * @author dyoma
 */
public final class CompactPropertyTable {
  public static final TypedKey<Integer> INDEX = TypedKey.create("index");
  private final EnumPropertySet<TypedKey<?>> mySet;
  private final EnumPropertySet<TypedKey<?>> myFullSet;
  @NotNull
  private Object[] myTable;
  private int myRowCount = 0;

  public CompactPropertyTable(EnumPropertySet<TypedKey<?>> set) {
    this(set, 10);
  }

  public CompactPropertyTable(@NotNull EnumPropertySet<TypedKey<?>> set, int initialRows) {
    mySet = set;
    myFullSet = mySet.with(INDEX);
    myTable = new Object[rowStart(initialRows)];
  }

  public <T> T get(int row, TypedKey<? extends T> key) {
    checkRowIndex(row);
    return key.cast(myTable[getIndex(row, key)]);
  }

  public <T> void set(int row, TypedKey<? super T> key, T value) {
    checkRowIndex(row);
    myTable[getIndex(row, key)] = value;
  }

  public int addRow() {
    ensureCapacity(myRowCount + 1);
    myRowCount++;
    return myRowCount - 1;
  }

  public void updateRow(int row, CompactPropertyMap data) {
    assert data.getPropertySet().containsAll(mySet) : data + " > " + mySet;
    for (TypedKey<?> key : mySet)
      copyFromMap(row, key, data);
  }

  private <T> void copyFromMap(int row, TypedKey<T> key, CompactPropertyMap data) {
    set(row, key, data.get(key));
  }

  private <T> void copyToMap(CompactPropertyMap dest, TypedKey<T> key, int row) {
    if (key != INDEX)
      dest.set(key, get(row, key));
  }

  public void getRow(int row, CompactPropertyMap dest) {
    getRow(row, dest, dest.getPropertySet());
  }

  public void getRow(int row, CompactPropertyMap dest, EnumPropertySet<TypedKey<?>> keys) {
    if (row < 0 || row >= myRowCount)
      throw new IndexOutOfBoundsException(row + " " + myRowCount);
    assert myFullSet.containsAll(keys);
    assert dest.getPropertySet().containsAll(keys);
    for (TypedKey<?> key : keys)
      copyToMap(dest, key, row);
    if (keys.contains(INDEX))
      dest.set(INDEX, row);
  }

  public CompactPropertyMap getRow(int row) {
    CompactPropertyMap result = new CompactPropertyMap(myFullSet);
    getRow(row, result);
    return result;
  }

  @NotNull
  public EnumPropertySet<TypedKey<?>> getStoredProperties() {
    return mySet;
  }

  public EnumPropertySet<TypedKey<?>> getAllProperties() {
    return myFullSet;
  }

  public int size() {
    return myRowCount;
  }

  private int getIndex(int row, TypedKey<?> key) {
    return rowStart(row) + mySet.getSafeIndex(key, mySet.size());
  }

  private void ensureCapacity(int expectedRows) {
    int expectedSize = rowStart(expectedRows);
    if (myTable.length > expectedSize)
      return;
    int newSize = myTable.length;
    while (newSize <= expectedSize)
      newSize *= 1.5;
    Object[] newTable = new Object[newSize];
    System.arraycopy(myTable, 0, newTable, 0, rowStart(myRowCount));
    myTable = newTable;
  }

  private int rowStart(int row) {
    return mySet.size() * row;
  }

  private void checkRowIndex(int row) {
    assert row >= 0 && row < myRowCount : row + " " + myRowCount;
    if (row >= myRowCount)
      throw new IndexOutOfBoundsException(row + " " + myRowCount);
  }

  public <T> int findFirst(TypedKey<T> key, T value) {
    int column = mySet.getKnownIndex(key, mySet.size());
    if (column < 0) return -1;
    for (int i = 0; i < myRowCount; i++) {
      int cell = rowStart(i) + column;
      if (Util.equals(value, myTable[cell])) return i;
    }
    return -1;
  }
}
