package com.almworks.items.impl.dbadapter;

import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class DBColumnSet {
  public static final DBColumnSet EMPTY = new Empty();

  @ThreadSafe
  public abstract int size();

  @ThreadSafe
  @NotNull
  public abstract DBColumn get(int index);

  public DBColumn findByName(String name) {
    if (name == null)
      return null;
    for (int i = 0; i < size(); i++) {
      DBColumn column = get(i);
      if (name.equals(column.getName()))
        return column;
    }
    return null;
  }

  public boolean contains(DBColumn column) {
    return indexOf(column) >= 0;
  }

  public int indexOf(DBColumn column) {
    if (column != null) {
      for (int i = 0; i < size(); i++)
        if (column.equals(get(i)))
          return i;
    }
    return -1;
  }

  public int indexByName(String columnName) {
    if (columnName != null) {
      for (int i = 0; i < size(); i++)
        if (columnName.equals(get(i).getName()))
          return i;
    }
    return -1;
  }

  public void addTo(Collection<DBColumn> collection) {
    int size = size();
    for (int i = 0; i < size; i++)
      collection.add(get(i));
  }

  public String toString() {
    return toString(new StringBuilder()).toString();
  }

  public StringBuilder toString(StringBuilder builder) {
    builder.append('[');
    String prefix = "";
    for (int i = 0; i < size(); i++) {
      DBColumn column = get(i);
      builder.append(prefix);
      column.toString(builder);
      prefix = ",";
    }
    builder.append(']');
    return builder;
  }

  public final boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || !(o instanceof DBColumnSet))
      return false;

    DBColumnSet that = (DBColumnSet) o;
    int size = size();
    if (size != that.size())
      return false;

    for (int i = 0; i < size; i++) {
      if (!get(i).equals(that.get(i))) {
        return false;
      }
    }
    return true;
  }

  public final int hashCode() {
    int r = 91919;
    for (int i = size() - 1; i >= 0; i--)
      r = r * 19 + get(i).hashCode();
    return r;
  }

  public static DBColumnSet create(@Nullable Collection<? extends DBColumn> columns) {
    if (columns == null || columns.isEmpty())
      return EMPTY;
    int size = columns.size();
    if (size == 1) {
      return new DBColumnSingle(columns.iterator().next());
    }
    if (size == 2) {
      Iterator<? extends DBColumn> ii = columns.iterator();
      DBColumn c1 = ii.next();
      DBColumn c2 = ii.next();
      return new DBColumnCouple(c1, c2);
    }
    return new DBColumnArray(columns.toArray(new DBColumn[size]));
  }

  public static DBColumnSet create(DBColumn column) {
    if (column == null) {
      assert false;
      return EMPTY;
    }
    return new DBColumnSingle(column);
  }

  public static DBColumnSet create(DBColumn... columns) {
    if (columns == null || columns.length == 0)
      return EMPTY;
    if (columns.length == 1)
      return new DBColumnSingle(columns[0]);
    if (columns.length == 2)
      return new DBColumnCouple(columns[0], columns[1]);
    return new DBColumnArray(ArrayUtil.arrayCopy(columns));
  }

  public DBColumnSet with(DBColumn column) {
    if (indexOf(column) >= 0)
      return this;
    int size = size();
    switch (size) {
    case 0:
      return new DBColumnSingle(column);
    case 1:
      return new DBColumnCouple(get(0), column);
    default:
      List<DBColumn> columns = Collections15.arrayList(size + 1);
      addTo(columns);
      columns.add(column);
      return new DBColumnArray(columns.toArray(new DBColumn[columns.size()]));
    }
  }

  public static class Empty extends DBColumnSet {
    public int size() {
      return 0;
    }

    public DBColumn get(int index) {
      assert false : index;
      return null;
    }

    public DBColumn findByName(String name) {
      return null;
    }

    public boolean contains(DBColumn column) {
      return false;
    }

    public int indexOf(DBColumn column) {
      return -1;
    }

    public void addTo(Collection<DBColumn> collection) {
    }

    private Empty() {
    }
  }
}
