package com.almworks.items.impl.dbadapter;

import com.almworks.util.collections.arrays.ArrayStorageAccessor;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

public abstract class DBColumn<T> {
  public static final DBColumn[] EMPTY_ARRAY = {};
  public static final DBLongColumn ITEM = new DBLongColumn("item");

  private final String myName;

  protected DBColumn(String name) {
    myName = name;
  }

  @NotNull
  public final String getName() {
    return myName;
  }

  @NotNull
  public abstract DBColumnType getDatabaseClass();

  // DB-2
  public abstract Object toDatabaseValue(T userValue);

  public abstract T loadValue(Object storage, int row);

  public abstract ArrayStorageAccessor getArrayAccessor();

  // UTILITY METHODS

  public Object appendNative(int row, Object src, int newRow, Object dst) {
    return getArrayAccessor().copyValue(src, row, dst, newRow);
  }

  public boolean areEqual(T value1, T value2) {
    return Util.equals(value1, value2);
  }

  public StringBuilder toString(StringBuilder builder) {
    builder.append(myName);
//    builder.append(':');
//    builder.append(StringUtil.substringAfterLast(getDatabaseClass().toString(), "."));
    return builder;
  }

  public String toString() {
    return toString(new StringBuilder()).toString();
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof DBColumn))
      return false;

    DBColumn column = (DBColumn) o;
    return myName.equalsIgnoreCase(column.myName);
  }

  public int hashCode() {
    return myName.hashCode();
  }
}
