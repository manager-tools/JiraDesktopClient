package com.almworks.items.impl.dbadapter;

import org.almworks.util.Collections15;

import java.util.List;

public class DBTableBuilder {
  private final String myGuid;
  private final int myVersion;
  private final List<DBColumn> myColumns = Collections15.arrayList();
  private final List<DBColumn> myPrimaryKey = Collections15.arrayList();
  private final List<DBColumn> myNotNulls = Collections15.arrayList();
  private final List<DBIndex> myIndexes = Collections15.arrayList();

  public DBTableBuilder(String guid) {
    this(guid, 0);
  }

  public DBTableBuilder(String guid, int version) {
    if (guid == null)
      throw new NullPointerException();
    guid = guid.trim();
    if (guid.length() == 0)
      throw new IllegalArgumentException("name " + guid);
    myGuid = guid;
    myVersion = version;
  }

  public static DBTableBuilder define(String guid) {
    return new DBTableBuilder(guid);
  }

  public DBTableBuilder column(DBColumn column, boolean notNull) {
    if (column == null || column.getName() == null) {
      assert false : column;
      return this;
    }
    if (myColumns.remove(column)) {
      myNotNulls.remove(column);
      myPrimaryKey.remove(column);
    }
    myColumns.add(column);
    if (notNull) {
      myNotNulls.add(column);
    }
    return this;
  }

  public DBTableBuilder cols(DBColumn... columns) {
    for (DBColumn column : columns)
      column(column, false);
    return this;
  }

  public DBTable create() {
    return new DBTable(myGuid, myVersion, DBColumnSet.create(myColumns), DBColumnSet.create(myPrimaryKey),
      myIndexes, DBColumnSet.create(myNotNulls));
  }

  public DBTableBuilder pk(DBColumn... columns) {
    if (columns == null || columns.length == 0)
      return this;
    assert myPrimaryKey.size() == 0;
    myPrimaryKey.clear();
    for (DBColumn column : columns) {
      if (!myColumns.contains(column)) {
        assert false : column;
        continue;
      }
      if (myPrimaryKey.contains(column)) {
        assert false : column;
        continue;
      }
      myPrimaryKey.add(column);
    }
    return this;
  }

  public DBTableBuilder itempk() {
    return ipk(DBColumn.ITEM);
  }

  public DBTableBuilder ipk(DBLongColumn name) {
    return column(name, true).pk(name);
  }

  public void index(DBIndex index) {
    if (index == null || index.getColumnCount() <= 0)
      return;
    for (int i = 0; i < index.getColumnCount(); i++) {
      DBColumn column = index.getColumn(i);
      if (!myColumns.contains(column)) {
        assert false : column + " " + index;
        return;
      }
    }
    myIndexes.add(index);
  }

  public DBIndex unique(DBColumn... column) {
    boolean[] desc = new boolean[column.length];
    DBIndex index = new DBIndex(true, column, desc);
    myIndexes.add(index);
    return index;
  }

  public DBTableBuilder index(DBColumn... columns) {
    boolean[] desc = new boolean[columns.length];
    DBIndex index = new DBIndex(false, columns, desc);
    index(index);
    return this;
  }

  public DBTableBuilder colNN(DBColumn... columns) {
    for (DBColumn column : columns)
      column(column, true);
    return this;
  }

  public static DBTableBuilder createPrimaryBuilder(String guid, DBColumn keyColumn) {
    DBTableBuilder builder = new DBTableBuilder(guid);
    builder.column(keyColumn, true);
    builder.pk(keyColumn);
    return builder;
  }
}
