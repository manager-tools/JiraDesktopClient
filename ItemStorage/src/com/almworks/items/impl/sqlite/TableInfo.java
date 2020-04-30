package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.DBTable;
import org.almworks.util.Collections15;

import java.util.List;

public final class TableInfo {
  /**
   * ID in "tables" table. This id will never be assigned to another table, though this table may migrate
   * to another table_id.
   *
   * @see com.almworks.items.impl.db.sqlite.TableManager
   */
  private final int myTableId;

  private final String myPhysicalTable;
  private final int myVersion;
  private final List<DBTable> myValidatedTables = Collections15.arrayList();


  public TableInfo(int tableId, String physicalTable, int version) {
    myTableId = tableId;
    myPhysicalTable = physicalTable;
    myVersion = version;
  }

  public int getTableId() {
    return myTableId;
  }

  public String getPhysicalTable() {
    return myPhysicalTable;
  }

  public int getVersion() {
    return myVersion;
  }

  public boolean isValidated(DBTable table) {
    return myValidatedTables.contains(table);
  }

  public void setValidated(DBTable table) {
    myValidatedTables.add(table);
  }

  public String toString() {
    return myPhysicalTable + "[" + myTableId + "," + myVersion + "]";
  }
}
