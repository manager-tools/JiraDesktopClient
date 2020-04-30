package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.sqlite4java.*;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.almworks.items.impl.sqlite.Schema.*;

/**
 * TableManager dynamically allocates SQLite tables for DBTables and
 * validates their structure using TableValidator
 * <p/>
 * All tables except statically defined in Schema are created and
 * managed through TableManager.
 * <p/>
 * TableManager exists per session, thus it can be created only in a session context.
 * <p/>
 * TableManager caches information from <code>tables</code> table, using {@link Schema#TCID} property
 * <p/>
 * <b>TABLES</b> table has a special lifecycle, critical for notifications: rows must not be updated,
 * only removed or deleted. Thus far there's no case where a table is deleted.
 */
class TableManager {
  private static final TypedKey<TableManager> TABLE_MANAGER = TypedKey.create("tableManager");
  private static final int MAX_TABLE_INCREMENT_COUNT = 50;
  private static final String DEFAULT_TABLE_NAME = "default_table";

  private static final SQLParts SQL_SELECT_TABLES =
    SQLUtil.select(null, Schema.TABLES, TABLE_ID, TABLES_NAME, TABLES_VERSION, TABLES_PHYSICAL_TABLE);
  private static final SQLParts SQL_INSERT_DATA = new SQLParts().append("INSERT OR REPLACE INTO ")
    .append(Schema.TABLES)
    .append(" (")
    .append(TABLES_NAME.getName())
    .append(", ")
    .append(TABLES_VERSION.getName())
    .append(", ")
    .append(TABLES_PHYSICAL_TABLE.getName())
    .append(") values (?, ?, ?)");


  private final Map<String, TableInfo> myTables = Collections15.hashMap();

  private long myTcid = 0;
  private Object myLastConnection = null;

  /**
   * Used as internal variable during getTableInfo().
   */
  private transient boolean myChanged;

  /**
   * Only in debug mode.
   */
  private boolean _enterLock;

  private TableManager() {
  }

  public static TableManager getFromContext(Map sessionContext) {
    TableManager manager = TABLE_MANAGER.getFrom(sessionContext);
    if (manager == null) {
      manager = new TableManager();
      TABLE_MANAGER.putTo(sessionContext, manager);
    }
    return manager;
  }

  @Nullable
  public TableInfo getTableInfo(TransactionContext context, DBTable table, boolean write)
    throws SQLiteException
  {
    assert enterLock(true);
    try {
      validateTcnAndConnection(context);
      myChanged = false;
      String name = table.getGuid();
      TableInfo info = getTableData(name);
      if (info != null) {
        int version = info.getVersion();
        // todo migration
        if (!info.isValidated(table)) {
          boolean valid = validateTable(context.getConnection(), info.getPhysicalTable(), name, table, write);
          if (valid) {
            info.setValidated(table);
          } else {
            info = null;
          }
        }
      }
      if (info == null && write)
        info = createNewPhysicalTable(context.getConnection(), table);
      if (myChanged) {
        if (!write) {
          assert false : table; // should not change db if !write
          Log.error("db changed without permission: " + table);
        }
        updateTcid(context);
      }
      return info;
    } finally {
      assert enterLock(false);
    }
  }

  private boolean enterLock(boolean lock) {
    assert _enterLock != lock : this;
    _enterLock = lock;
    return true;
  }

  private TableInfo getTableData(String tableName) {
    return myTables.get(tableName);
  }

  private void putTableData(String tableName, TableInfo info) {
    myTables.put(tableName, info);
  }

  private void clear() {
    myTables.clear();
  }

  private void updateTcid(TransactionContext context) throws SQLiteException {
    assert context.getConnection() == myLastConnection : context + " " + myLastConnection;
    myTcid = createTcid(getTcidSequence(myTcid) + 1);
    context.setProperty(Schema.TCID, myTcid);
  }

  private boolean validateTable(SQLiteConnection db, String physicalTable, String name, DBTable table,
    boolean write) throws SQLiteException
  {
    TableValidator validator = new TableValidator(db, physicalTable, table, write, write);
    try {
      validator.validateOrFail();
      return true;
    } catch (TableValidator.Failed e) {
      return false;
    } finally {
      if (validator.isDatabaseChanged()) {
        myChanged = true;
      }
    }
  }

  private TableInfo createNewPhysicalTable(SQLiteConnection db, DBTable table) throws SQLiteException {
    String name = table.getGuid();
    String physicalTable = createTable(db, name, table);
    assert physicalTable != null;
    int id = insertTableData(db, name, table.getVersion(), physicalTable);
    TableInfo info = new TableInfo(id, physicalTable, table.getVersion());
    info.setValidated(table);
    putTableData(name, info);
    return info;
  }

  private int insertTableData(SQLiteConnection db, String name, int version, String physicalTable)
    throws SQLiteException
  {
    SQLiteStatement st = db.prepare(SQL_INSERT_DATA, true);
    try {
      st.bind(1, name);
      st.bind(2, version);
      st.bind(3, physicalTable);
      st.step();
      return (int) db.getLastInsertId();
    } finally {
      st.dispose();
    }
  }

  private String createTable(SQLiteConnection db, String name, DBTable table) throws SQLiteException {
    Log.debug(this + ": creating table " + name);
    List<String> names = normalizeTableName(name);
    assert names != null && !names.isEmpty() : name;
    for (String tableName : names) {
      if (tryCreateTable(db, tableName, table)) {
        return tableName;
      }
    }
    String tableName = names.get(names.size() - 1);
    int version = table.getVersion();
    String namev = tableName + "_v" + version;
    if (tryCreateTable(db, namev, table))
      return namev;
    StringBuilder namei = new StringBuilder(tableName).append('_');
    Random r = new Random();
    for (int i = 0; i < MAX_TABLE_INCREMENT_COUNT; i++) {
      int c = ((int) 'a') + r.nextInt(26);
      namei.append((char) c);
      String n = namei.toString();
      if (tryCreateTable(db, n, table))
        return n;
    }
    throw new SQLiteException(SQLiteConstants.WRAPPER_USER_ERROR,
      this + " failed: cannot allocate physical table for " + name);
  }

  private boolean tryCreateTable(SQLiteConnection db, String physicalTable, DBTable table) {
    TableValidator validator = new TableValidator(db, physicalTable, table, true, true);
    try {
      validator.createTable();
      Log.debug(this + ": successfully created " + physicalTable);
      return true;
    } catch (SQLiteException e) {
      Log.debug(this + ": cannot create " + physicalTable + ": " + e);
      return false;
    } finally {
      if (validator.isDatabaseChanged()) {
        myChanged = true;
      }
    }
  }

  private List<String> normalizeTableName(String name) {
    if (name == null) name = "";
    List<String> variants = Collections15.arrayList();
    String[] pieces = name.trim().split(":+");
    StringBuilder r = new StringBuilder(name.length());
    String suffix = null;
    boolean prependedFirstDigitWithUnderscore;
    for (int i = pieces.length - 1; i >= 0; i--){
      String piece = pieces[i];
      piece = piece.trim();
      if (piece.length() == 0) continue;
      r.setLength(0);
      prependedFirstDigitWithUnderscore = false;
      for (int j = 0; j < piece.length(); j++) {
        char c = piece.charAt(j);
        if (Character.isLetterOrDigit(c)) {
          c = Character.toLowerCase(c);
          if (c >= '0' && c <= '9' || c >= 'a' && c <= 'z') {
            if (j == 0 && c >= '0' && c <= '9') {
              // SQLite does not allow to create tables starting with digits, prepend with _
              r.append('_');
              prependedFirstDigitWithUnderscore = true;
            }
            r.append(c);
          }
        } else if (c == '_' || c == '-' || c == '.' || c == ',' || c == '*' || Character.isWhitespace(c)) {
          r.append('_');
        }
      }
      if (r.length() == 0) r.append('x');
      if (suffix != null) {
        r.append('_');
        r.append(suffix);
      }
      suffix = r.toString();
      variants.add(suffix);
      if (prependedFirstDigitWithUnderscore) {
        // Extra underscore is unneeded in suffix
        suffix = suffix.substring(1);
      }
    }
    if (variants.isEmpty()) variants.add(DEFAULT_TABLE_NAME);
    return variants;
  }

  private void validateTcnAndConnection(TransactionContext context) throws SQLiteException {
    SQLiteConnection db = context.getConnection();
    Long tcidL = context.getProperty(Schema.TCID);
    long tcid = tcidL == null ? 0 : tcidL;
    if (db != myLastConnection || myTcid != tcid) {
      if (myTcid != 0) {
        Log.debug(this + ": detected table change " + Schema.formatTcid(myTcid) + " to " + Schema.formatTcid(tcid));
      }
      clear();
      SQLiteStatement st = db.prepare(SQL_SELECT_TABLES, true);
      try {
        int i = 0;
        while (st.step()) {
          int id = st.columnInt(0);
          String name = Util.NN(st.columnString(1)).trim();
          int version = st.columnInt(2);
          String pname = Util.NN(st.columnString(3)).trim();
          i++;
          if (id <= 0 || name.length() == 0 || pname.length() == 0) {
            Log.warn("bad table in row " + i + ": " + id + " " + name + " " + version + " " + pname);
          } else {
            myTables.put(name, new TableInfo(id, pname, version));
          }
        }
      } finally {
        st.dispose();
      }
      myLastConnection = db;
      myTcid = tcid;
    }
  }

  public String toString() {
    Object db = myLastConnection;
    return db == null ? "TM" : "TM(" + db + ")";
  }
}
