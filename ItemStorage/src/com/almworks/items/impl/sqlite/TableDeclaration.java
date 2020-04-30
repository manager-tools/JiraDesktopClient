package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBColumnSet;
import com.almworks.items.impl.dbadapter.DBColumnType;
import com.almworks.items.impl.dbadapter.DBIndex;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TableDeclaration {
  private final Map<String, ColumnDecl> myColumns = Collections15.hashMap();
  private String[] myPrimaryKey = Const.EMPTY_STRINGS;
  private Index[] myIndexes;

  public Set<String> copyColumnSet() {
    return Collections15.hashSet(myColumns.keySet());
  }

  public boolean isNotNull(String column) {
    return myColumns.get(column).myNotNull;
  }

  public String getDefaultLiteral(String column) {
    return myColumns.get(column).myDefault;
  }

  public void addColumn(String name, DBColumnType type, boolean notnull, String def) {
    myColumns.put(name, new ColumnDecl(type, notnull, def));
  }

  public boolean hasColumn(String name) {
    return myColumns.get(name) != null;
  }

  public DBColumnType getDatabaseClass(String cname) {
    return myColumns.get(cname).myType;
  }

  public boolean equalPrimaryKey(DBColumnSet pks) {
    if (myPrimaryKey.length != pks.size())
      return false;
    String prevKey = null;
    for (String key : myPrimaryKey) {
      if (prevKey != null && prevKey.equalsIgnoreCase(key))
        return false;
      if (pks.findByName(key) == null)
        return false;
    }
    return true;
  }

  public String getPKString() {
    return TextUtil.separate(myPrimaryKey, ", ");
  }

  public void setPrimaryKey(List<String> names) {
    myPrimaryKey = names.toArray(new String[names.size()]);
    Arrays.sort(myPrimaryKey, String.CASE_INSENSITIVE_ORDER);
  }

  public void addIndexes(List<Index> indexes) {
    myIndexes = indexes.toArray(new Index[indexes.size()]);
  }

  public boolean containsIndex(DBIndex index) {
    if (myIndexes == null)
      return false;
    for (Index idx : myIndexes) {
      if (idx.equalTo(index))
        return true;
    }
    return false;
  }

  private static class ColumnDecl {
    private final DBColumnType myType;
    private final boolean myNotNull;
    private final String myDefault;

    private ColumnDecl(DBColumnType type, boolean notNull, String aDefault) {
      myType = type;
      myNotNull = notNull;
      myDefault = aDefault;
    }
  }

  public static class Index {
    private final List<String> myColumns = Collections15.arrayList();
    private final List<Boolean> myDesc = Collections15.arrayList();
    private boolean myUnique;

    public void add(String columnName, boolean desc) {
      myColumns.add(columnName);
      myDesc.add(desc);
    }

    public int getColumnCount() {
      assert myColumns.size() == myDesc.size();
      return myColumns.size();
    }

    public void setUnique(boolean unique) {
      myUnique = unique;
    }

    public boolean equalTo(DBIndex index) {
      if (myUnique != index.isUnique())
        return false;
      if (getColumnCount() != index.getColumnCount())
        return false;
      for (int i = 0; i < index.getColumnCount(); i++) {
        DBColumn column = index.getColumn(i);
        if (!myColumns.get(i).equals(column.getName()))
          return false;
        if (!myDesc.get(i).equals(index.isDescending(i)))
          return false;
      }
      return true;
    }
  }
}
