package com.almworks.items.impl.dbadapter;

import org.almworks.util.Collections15;

import java.util.List;

public class DBIndexBuilder {
  private final List<DBColumn> myColumns = Collections15.arrayList();
  private final List<Boolean> myDesc = Collections15.arrayList();
  private boolean myUnique;

  public DBIndexBuilder setUnique(boolean unique) {
    myUnique = unique;
    return this;
  }

  public DBIndexBuilder add(DBColumn column, boolean desc) {
    if (myColumns.contains(column))
      throw new IllegalArgumentException(String.valueOf(column));
    myColumns.add(column);
    myDesc.add(desc);
    return this;
  }

  public DBIndex create() {
    boolean[] desc = new boolean[myDesc.size()];
    int i = 0;
    for (Boolean b : myDesc) {
      desc[i++] = b;
    }
    return new DBIndex(myUnique, myColumns.toArray(new DBColumn[myColumns.size()]), desc);
  }

  public int getColumnCount() {
    return myColumns.size();
  }
}
