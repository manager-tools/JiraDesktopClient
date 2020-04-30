package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.*;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.util.List;
import java.util.Set;

class TableValidator {
  static final SQLParts SQL_CHECK_TABLE =
    new SQLParts("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?");

  private final SQLiteConnection myDb;
  private final String myTableName;
  private final DBTable myTable;
  private final boolean myAutoCreate;
  private final boolean myAutoAlter;
  private final String myLiteralTableName;

  private boolean myDatabaseChanged;

  public TableValidator(SQLiteConnection db, String tableName, DBTable table, boolean autoCreate,
    boolean autoAlter)
  {
    myDb = db;
    myTableName = tableName;
    myTable = table;
    myAutoCreate = autoCreate;
    myAutoAlter = autoAlter;
    myLiteralTableName = literalizeTableName(myTableName);
  }

  public static void validate(SQLiteConnection db, String tableName, DBTable table) throws SQLiteException {
    new TableValidator(db, tableName, table, true, true).validate();
  }

  public static void validateOrFail(SQLiteConnection db, String tableName, DBTable table, boolean autoCreate,
    boolean autoAlter) throws SQLiteException, Failed
  {
    new TableValidator(db, tableName, table, autoCreate, autoAlter).validateOrFail();
  }

  public void validate() throws SQLiteException {
    try {
      validateOrFail();
    } catch (Failed e) {
      Log.warn(this + ": failed, recreating [" + e + "]");
      recreateTable();
    }
  }

  private void recreateTable() throws SQLiteException {
    myDb.exec("DROP TABLE IF EXISTS " + myLiteralTableName);
    myDatabaseChanged = true;
    createTable();
  }

  public void createTable() throws SQLiteException {
    String sql = sqlCreateTable(myLiteralTableName, myTable);
    myDb.exec(sql);
    myDatabaseChanged = true;
    for (DBIndex index : myTable.getIndexes()) {
      sql = sqlCreateIndex(myLiteralTableName, index);
      try {
        myDb.exec(sql);
      } catch (SQLiteException e) {
        Log.warn("cannot create index " + index + " on " + myTable, e);
      }
    }
  }

  private static String sqlCreateIndex(String literalTableName, DBIndex index) {
    StringBuilder r = new StringBuilder("CREATE ");
    if (index.isUnique())
      r.append("UNIQUE ");
    r.append("INDEX\n  ");
    // index name
    createIndexName(r, literalTableName, index);
    r.append("\nON ");
    r.append(literalTableName);
    r.append(" (");
    String prefix = "\n  ";
    for (int i = 0; i < index.getColumnCount(); i++) {
      DBColumn column = index.getColumn(i);
      assert checkIndexColumnName(column.getName());
      boolean desc = index.isDescending(i);
      r.append(prefix).append(column.getName()).append(desc ? " DESC" : " ASC");
      prefix = ",\n  ";
    }
    r.append("\n)");
    return r.toString();
  }

  public void validateOrFail() throws SQLiteException, Failed {
    clearTemp();
    if (!hasTable()) {
      if (myAutoCreate) {
        createTable();
      } else {
        throw new Failed(this + ": table not exists");
      }
    } else {
      TableDeclaration existing = getExistingTableDefinition();
      List<String> alterations = collectTableAlterations(existing);
      if (!alterations.isEmpty()) {
        if (!myAutoAlter)
          throw new Failed(this + ": tables differ");
        for (String sql : alterations) {
          myDb.exec(sql);
          myDatabaseChanged = true;
        }
      }
      List<String> indexes = collectIndexAlterations(existing);
      if (!indexes.isEmpty()) {
        if (!myAutoAlter) {
          // it wasn't needed to alter table, but some indexes are missing
          for (String sql : indexes) {
            Log.warn("not creating index: " + sql);
          }
        } else {
          for (String sql : indexes) {
            try {
              myDb.exec(sql);
              // don't set myDatabaseChanged
            } catch (SQLiteException e) {
              Log.warn("cannot create index: " + sql, e);
            }
          }
        }
      }
    }
  }

  private List<String> collectIndexAlterations(TableDeclaration table) {
    List<String> sqls = Collections15.arrayList();
    List<DBIndex> indexes = myTable.getIndexes();
    for (DBIndex index : indexes) {
      if (!table.containsIndex(index)) {
        sqls.add(sqlCreateIndex(myLiteralTableName, index));
      }
    }
    return sqls;
  }

  private void clearTemp() throws SQLiteException {
    // always drop temp table to avoid table name collision
    myDb.exec("DROP TABLE IF EXISTS temp." + myLiteralTableName);
  }

  private List<String> collectTableAlterations(TableDeclaration existing) throws Failed {
    List<String> sqls = Collections15.arrayList();
    Set<String> unneededExistingColumns = existing.copyColumnSet();
    DBColumnSet pks = myTable.getPrimaryKey();
    if (!existing.equalPrimaryKey(pks)) {
      throw new Failed(this + ": cannot change primary keys " + existing.getPKString() + " to " + pks);
    }
    DBColumnSet columns = myTable.getColumns();
    for (int i = 0; i < columns.size(); i++) {
      DBColumn column = columns.get(i);
      String cname = column.getName();
      if (!existing.hasColumn(cname)) {
        // column should be added
        if (pks.contains(column)) {
          throw new Failed(this + ": cannot add primary key " + column);
        }
        if (myTable.isNotNull(column)/* && myTable.getDefaultLiteral(column) == null*/) {
          throw new Failed(this + ": cannot add " + column);
        }
        sqls.add("ALTER TABLE " + myLiteralTableName + " ADD COLUMN " +
          sqlCreateTableColumn(new StringBuilder(), column, false, myTable.isNotNull(column), null
/*myTable.getDefaultLiteral(column)*/).toString());
      } else {
        unneededExistingColumns.remove(cname);
        if (myTable.isNotNull(column) != existing.isNotNull(cname))
          throw new Failed(this + ": cannot change " + cname + " to " + column);
/*
        if (!Util.equals(existing.getDefaultLiteral(cname), myTable.getDefaultLiteral(column)))
          throw new Failed(this + ": cannot change " + cname + " to " + column);
*/
        if (column.getDatabaseClass() != existing.getDatabaseClass(cname))
          Log.warn(this + ": not changing " + cname + " to " + column);
      }
    }
    for (String existingColumn : unneededExistingColumns) {
      if (existing.isNotNull(existingColumn) && existing.getDefaultLiteral(existingColumn) == null)
        throw new Failed(this + ": cannot leave unneeded column " + existingColumn);
      Log.warn("leaving unneeded column [" + existingColumn + "]");
    }
    return sqls;
  }

  private boolean hasTable() throws SQLiteException {
    SQLiteStatement st = SQLiteStatement.DISPOSED;
    try {
      st = myDb.prepare(SQL_CHECK_TABLE);
      return st.bind(1, myTableName).step();
    } finally {
      st.dispose();
    }
  }

  private static String sqlCreateTable(String literalName, DBTable table) {
    StringBuilder r = new StringBuilder();
    r.append("CREATE TABLE ").append(literalName).append(" (");
    DBColumnSet pks = table.getPrimaryKey();
    String prefix = "\n  ";
    DBColumnSet columns = table.getColumns();
    for (int i = 0; i < columns.size(); i++) {
      DBColumn column = columns.get(i);
      r.append(prefix);
      sqlCreateTableColumn(r, column, pks.size() == 1 && pks.get(0).equals(column), table.isNotNull(column), null
/*table.getDefaultLiteral(column)*/);
      prefix = ",\n  ";
    }
    if (pks.size() > 1) {
      r.append(prefix);
      prefix = "PRIMARY KEY (";
      for (int i = 0; i < pks.size(); i++) {
        DBColumn pk = pks.get(i);
        r.append(prefix).append(pk.getName());
        prefix = ", ";
      }
      r.append(")");
    }
    r.append("\n)");
    return r.toString();
  }

  private static StringBuilder sqlCreateTableColumn(StringBuilder builder, DBColumn column, boolean solePrimaryKey,
    boolean notNull, String defaultLiteral)
  {
    builder.append(column.getName());
    DBColumnType type = column.getDatabaseClass();
    String sqlType = convertDBColumnTypeToSqlType(type);
    if (sqlType != null) {
      builder.append(' ').append(sqlType);
    }
    if (notNull) {
      builder.append(" NOT NULL");
    }
    String defvalue = defaultLiteral;
    if (defvalue != null) {
      defvalue = defvalue.trim();
      if (!isValidLiteral(defvalue))
        defvalue = "'" + defvalue.replaceAll("\\'", "''") + "'";
      builder.append(" DEFAULT ").append(defvalue);
    }
    if (solePrimaryKey) {
      builder.append(" PRIMARY KEY");
      if (type == DBColumnType.INTEGER)
        builder.append(" AUTOINCREMENT");
    }
    return builder;
  }

  private static boolean checkIndexColumnName(String name) {
    if (name.indexOf('_') >= 0)
      assert false : name + ": column with this name can't be used for index";
    return true;
  }

  private static boolean isValidLiteral(String value) {
    // accept only integer literals
    // string literals with quotes will be converted anyway
    if (value.length() == 0)
      return false;
    for (int i = 0; i < value.length(); i++)
      if (!Character.isDigit(value.charAt(i)))
        return false;
    return true;
  }

  private TableDeclaration getExistingTableDefinition() throws SQLiteException {
    SQLiteStatement pragma = SQLiteStatement.DISPOSED;
    try {
      TableDeclaration result = new TableDeclaration();
      pragma = myDb.prepare(new SQLParts().append("PRAGMA table_info(").append(myLiteralTableName).append(")"), false);
      List<String> pks = Collections15.arrayList();
      boolean columnCheck = false;
      while (pragma.step()) {
        if (!columnCheck) {
          checkTableInfoColumns(pragma);
          columnCheck = true;
        }
        String cname = pragma.columnString(1);
        String ctype = pragma.columnString(2);
        boolean cnotnull = pragma.columnInt(3) != 0;
        String cdef = pragma.columnString(4);
//        if (('\'' + DBTableDefinition.AUTOINCREMENT + '\'').equals(cdef))
//          cdef = DBTableDefinition.AUTOINCREMENT;
        if (pragma.columnInt(5) != 0)
          pks.add(cname);
        result.addColumn(cname, convertSqlTypeToDBColumnType(ctype), cnotnull, cdef);
      }
      if (!pks.isEmpty()) {
        result.setPrimaryKey(pks);
      }
      pragma.dispose();
      pragma = myDb.prepare(new SQLParts().append("PRAGMA index_list(").append(myLiteralTableName).append(")"), false);
      columnCheck = false;
      List<TableDeclaration.Index> indexes = Collections15.arrayList();
      while (pragma.step()) {
        if (!columnCheck) {
          checkIndexListColumns(pragma);
          columnCheck = true;
        }
        String indexName = pragma.columnString(1);
        boolean unique = pragma.columnInt(2) != 0;
        TableDeclaration.Index index = decipherIndexName(result, unique, indexName);
        if (index != null) {
          indexes.add(index);
        } else {
          if (indexName != null && indexName.startsWith("sqlite_")) {
            // system index (analyze)
          } else {
            Log.warn(
              this + ": unrecognized index " + indexName + " (unique: " + unique + ", table " + myLiteralTableName +
                ")");
          }
        }
      }
      if (columnCheck)
        result.addIndexes(indexes);
      return result;
    } finally {
      pragma.dispose();
    }
  }

  private static void createIndexName(StringBuilder builder, String literalTableName, DBIndex index) {
    builder.append("I_");
    builder.append(literalTableName);
    builder.append(index.isUnique() ? "_U" : "_N");
    for (int i = 0; i < index.getColumnCount(); i++) {
      builder.append('_');
      builder.append(index.getColumn(i).getName());
      builder.append(index.isDescending(i) ? "_D" : "_A");
    }
  }

  /**
   * Index_name ::= "I_" table_name "_" uniqueness "_" indexElement [ "_" indexElement ]*
   * uniqueness ::= "U" | "N"
   * indexElement ::= columnname "_" direction
   * direction ::= "A" | "D"
   */
  private TableDeclaration.Index decipherIndexName(TableDeclaration table, boolean unique, String index) {
    int len = index.length();
    if (!index.startsWith("I_"))
      return null;
    int p = 2 + myLiteralTableName.length();
    if (!index.substring(2, p).equalsIgnoreCase(myLiteralTableName))
      return null;
    if (p >= len || index.charAt(p) != '_')
      return null;
    p++;
    if (p >= len)
      return null;
    char uniquenessChar = index.charAt(p++);
    boolean uniqueIndex;
    if (uniquenessChar == 'u' || uniquenessChar == 'U')
      uniqueIndex = true;
    else if (uniquenessChar == 'n' || uniquenessChar == 'N')
      uniqueIndex = false;
    else
      return null;
    if (uniqueIndex != unique)
      return null;
    TableDeclaration.Index builder = new TableDeclaration.Index();
    while (p < len) {
      if (p >= len || index.charAt(p) != '_')
        return null;
      p++;
      StringBuilder b = new StringBuilder(20);
      while (p < len) {
        char c = index.charAt(p++);
        if (c == '_')
          break;
        b.append(c);
      }
      if (p == len)
        return null;
      String columnName = b.toString();
      if (!table.hasColumn(columnName))
        return null;
      char dir = index.charAt(p++);
      boolean desc;
      if (dir == 'a' || dir == 'A')
        desc = false;
      else if (dir == 'd' || dir == 'D')
        desc = true;
      else
        return null;
      try {
        builder.add(columnName, desc);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
    if (builder.getColumnCount() == 0)
      return null;
    builder.setUnique(unique);
    return builder;
  }

  private static DBColumnType convertSqlTypeToDBColumnType(String sqlType) {
    if ("integer".equalsIgnoreCase(sqlType))
      return DBColumnType.INTEGER;
    else if ("text".equalsIgnoreCase(sqlType))
      return DBColumnType.TEXT;
    else if ("blob".equalsIgnoreCase(sqlType))
      return DBColumnType.BLOB;
    else
      return DBColumnType.ANY;
  }

  private static String convertDBColumnTypeToSqlType(DBColumnType type) {
    if (type == null)
      return null;
    switch (type) {
    case INTEGER:
      return "integer";
    case TEXT:
      return "text";
    case BLOB:
      return "blob";
    }
    return null;
  }


  private static void checkTableInfoColumns(SQLiteStatement pragma) throws SQLiteException {
    int c = pragma.columnCount();
    if (c != 6)
      throw new SQLiteException(0, "pragma table_info column count " + c);
    checkColumnName(pragma, 0, "cid");
    checkColumnName(pragma, 1, "name");
    checkColumnName(pragma, 2, "type");
    checkColumnName(pragma, 3, "notnull");
    checkColumnName(pragma, 4, "dflt_value");
    checkColumnName(pragma, 5, "pk");
  }

  private static void checkIndexListColumns(SQLiteStatement pragma) throws SQLiteException {
    int c = pragma.columnCount();
    if (c != 3)
      throw new SQLiteException(0, "pragma index_list column count " + c);
    checkColumnName(pragma, 0, "seq");
    checkColumnName(pragma, 1, "name");
    checkColumnName(pragma, 2, "unique");
  }

  private static void checkColumnName(SQLiteStatement pragma, int column, String name) throws SQLiteException {
    String realName = pragma.getColumnName(column);
    if (!name.equalsIgnoreCase(realName))
      throw new SQLiteException(0, "pragma column name (" + column + ") " + realName);
  }

  /**
   * @return name in double quotes if the name is not an identifier
   */
  private static String literalizeTableName(String name) {
    int len = name.length();
    for (int i = 0; i < len; i++) {
      char c = name.charAt(i);
      if (!(Character.isLetterOrDigit(c) || c == '_' || c == '$')) {
        return "\"" + name.replaceAll("\"", "\"\"") + "\"";
      }
    }
    return name;
  }

  public String toString() {
    return "verifying " + myTableName;
  }

  public boolean isDatabaseChanged() {
    return myDatabaseChanged;
  }

  public static class Failed extends Exception {
    public Failed(String reason) {
      super(reason);
    }
  }
}
