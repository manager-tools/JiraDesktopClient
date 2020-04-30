package com.almworks.items.impl.dbadapter;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DBTable {
  private final String myGuid;

  private final int myVersion;

  private final DBColumnSet myColumns;

  private final DBColumnSet myPrimaryKey;

  private final List<DBIndex> myIndexes;

  private final DBColumnSet myNotNullColumns;

  DBTable(String guid, int version, DBColumnSet columns, DBColumnSet primaryKey,
    List<DBIndex> indexes, DBColumnSet notNullColumns)
  {
    assert guid != null;
    assert version >= 0;
    assert columns != null;
    assert columns.size() > 0;
    assert checkIndexes(columns, primaryKey, indexes);
    assert checkColumns(columns, notNullColumns);
    myGuid = guid;
    myColumns = columns;
    myVersion = version;
    myPrimaryKey = primaryKey;
    myIndexes = Collections15.unmodifiableListCopy(indexes);
    myNotNullColumns = notNullColumns;
  }

  public DBTable getDefinition() {
    return this;
  }

  public String getGuid() {
    return myGuid;
  }

  public int getVersion() {
    return myVersion;
  }

  public DBColumnSet getColumns() {
    return myColumns;
  }

  @Nullable
  public DBColumnSet getPrimaryKey() {
    return myPrimaryKey;
  }

  @NotNull
  public List<DBIndex> getIndexes() {
    return myIndexes;
  }

  public boolean isNotNull(DBColumn column) {
    assert myColumns.contains(column) : column + " " + this;
    return myPrimaryKey.contains(column) || myNotNullColumns.contains(column);
  }

  public StringBuilder toString(StringBuilder r) {
    r.append(myGuid);
//    if (myVersion != 0) {
//      r.append("[v").append(myVersion).append(']');
//    }
//    myColumns.toString(r);

    // we really don't need this for debug
//    if (myPrimaryKey.size() > 0) {
//      r.append("pk");
//      myPrimaryKey.toString(r);
//    }
//    if (myIndexes.size() > 0) {
//      r.append('[').append(myIndexes.size()).append(" indexes]");
//    }

    return r;
  }

  public String toString() {
    return toString(new StringBuilder()).toString();
  }

  private static boolean checkIndexes(DBColumnSet columns, DBColumnSet primaryKey, List<DBIndex> indexes) {
    if (primaryKey != null) {
      for (int i = 0; i < primaryKey.size(); i++) {
        assert columns.contains(primaryKey.get(i)) : primaryKey.get(i);
      }
    }
    if (indexes != null) {
      for (DBIndex index : indexes) {
        for (int i = 0; i < index.getColumnCount(); i++) {
          assert columns.contains(index.getColumn(i)) : index.getColumn(i);
        }
      }
    }
    return true;
  }

  private static boolean checkColumns(DBColumnSet columns, DBColumnSet checked) {
    for (int i = 0; i < checked.size(); i++) {
      DBColumn column = checked.get(i);
      assert columns.contains(column) : column + " " + columns;
    }
    return true;
  }

  public DBRowSet createFullRowSet() {
    return new DBRowSet(myColumns);
  }

  public DBRow createFullRow() {
    return DBRowUtil.create(myColumns);
  }

  public boolean verifyColumns(DBColumnSet columns) {
    for (int i = 0; i < columns.size(); i++) {
      DBColumn column = columns.get(i);
      verifyColumn(column);
    }
    return true;
  }

  public boolean verifyColumn(DBColumn column) {
    assert myColumns.findByName(column.getName()) != null : this + " " + column;
    return true;
  }

  public boolean containsColumn(DBColumn column) {
    return myColumns.findByName(column.getName()) != null;
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    DBTable that = (DBTable) o;

    if (myVersion != that.myVersion)
      return false;
    if (!myColumns.equals(that.myColumns))
      return false;
    if (!myIndexes.equals(that.myIndexes))
      return false;
    if (!myGuid.equals(that.myGuid))
      return false;
    if (!myNotNullColumns.equals(that.myNotNullColumns))
      return false;
    if (!myPrimaryKey.equals(that.myPrimaryKey))
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myGuid.hashCode();
    result = 31 * result + myVersion;
    result = 31 * result + myColumns.hashCode();
    result = 31 * result + myPrimaryKey.hashCode();
    result = 31 * result + myIndexes.hashCode();
    result = 31 * result + myNotNullColumns.hashCode();
    return result;
  }
}
