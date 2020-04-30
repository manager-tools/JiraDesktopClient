package com.almworks.items.impl.sqlite;

import com.almworks.integers.LongCollector;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBColumnSet;
import com.almworks.items.impl.dbadapter.DBRow;
import com.almworks.items.impl.dbadapter.ItemVisitor;
import com.almworks.sqlite4java.*;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Map;

public class SQLUtil {
  // the real limitation is 999, which could be brought up further, but this limit helps organize queries efficiently
  public static final int MAX_SQL_PARAMS = 100;

  private static final TypedKey<long[]> IDS_BUFFER = TypedKey.create("IDS_BUFFER");
  private static final int IDS_BUFFER_SIZE = 1024;
  private static final int MIN_BUFFER_SIZE2 = 8;
  private static final int MAX_BUFFER_SIZE2 = 16;
  private static final int ARRAY_OVERHEAD8 = 8;
  private static final int MAX_CACHEABLE_BUFFER_SIZE = (1 << MAX_BUFFER_SIZE2) - ARRAY_OVERHEAD8;
  private static final BufferKey[] BUFFER_KEYS = BufferKey.initialize();

  public static int loadLongs(SQLiteStatement stmt, @Nullable TransactionContext context, LongCollector target)
    throws SQLiteException
  {
    SessionContext sessionContext = context == null ? null : context.getSessionContext();
    long[] buffer = getLongBuffer(sessionContext, 1024);
    int total = 0;
    try {
      if (context != null) context.addCancellable(stmt);
      int L;
      do {
        L = stmt.loadLongs(0, buffer, 0, buffer.length);
        for (int i = 0; i < L; i++)
          target.add(buffer[i]);
        total += L;
      } while (stmt.hasRow() && L > 0);
      return total;
    } finally {
      if (context != null) context.removeCancellable(stmt);
      releaseLongBuffer(sessionContext, buffer);
    }
  }

  public static int loadItems(SQLiteStatement stmt, @Nullable TransactionContext context, ItemVisitor target)
    throws SQLiteException
  {
    SessionContext sessionContext = context == null ? null : context.getSessionContext();
    long[] buffer = getLongBuffer(sessionContext, 1024);
    int total = 0;
    try {
      if (context != null) context.addCancellable(stmt);
      int L;
      boolean r = true;
      do {
        L = stmt.loadLongs(0, buffer, 0, buffer.length);
        total += L;
        if (L > 0) {
          r = target.visitItems(buffer, 0, L);
        }
      } while (stmt.hasRow() && L > 0 && r);
      return total;
    } finally {
      if (context != null) context.removeCancellable(stmt);
      releaseLongBuffer(sessionContext, buffer);
    }
  }

  public static Object[] appendWhere(DBRow sample, SQLParts sql) {
    DBColumnSet queryColumns = sample.getColumns();
    Object[] dbValues;
    if (queryColumns.size() == 0)
      dbValues = Const.EMPTY_OBJECTS;
    else {
      dbValues = new Object[queryColumns.size()];
      sql.append(" WHERE ");
      String prefix = "";
      for (int i = 0; i < queryColumns.size(); i++) {
        DBColumn queryColumn = queryColumns.get(i);
        sql.append(prefix);
        sql.append(queryColumn.getName());
        dbValues[i] = queryColumn.toDatabaseValue(sample.getValue(queryColumn));
        if (dbValues[i] != null)
          sql.append(" = ?");
        else
          sql.append(" is null");
        prefix = " AND ";
      }
    }
    return dbValues;
  }

  public static void bindParameter(SQLiteStatement stmt, int index, Object value) throws SQLiteException {
    if (value instanceof Long)
      stmt.bind(index, ((Long) value).longValue());
    else if (value instanceof Integer)
      stmt.bind(index, ((Integer) value).intValue());
    else if (value instanceof String)
      stmt.bind(index, ((String) value));
    else if (value instanceof byte[])
      stmt.bind(index, (byte[]) value);
    else if (value == null)
      stmt.bindNull(index);
    else {
      assert false : value.getClass() + " " + value;
      throw new SQLiteException(SQLiteConstants.WRAPPER_USER_ERROR, "Unknown type: " + value);
    }
  }

  private SQLUtil() {
  }

  static void bindRow(SQLiteStatement select, DBRow sample) throws SQLiteException {
    DBColumnSet columns = sample.getColumns();
    for (int i = 0; i < columns.size(); i++) {
      DBColumn column = columns.get(i);
      try {
        bindParameter(select, i + 1, column.toDatabaseValue(sample.getValue(column)));
      } catch (ClassCastException e) {
        Log.debug("CCE: " + column + " " + sample);
        throw e;
      }
    }
  }

  static SQLParts select(SQLParts sql, String physicalTable, DBColumn... what) {
    if (sql == null)
      sql = new SQLParts();
    sql.append("SELECT ");
    if (what.length == 0) {
      sql.append("1");
    } else {
      String prefix = "";
      for (DBColumn column : what) {
        sql.append(prefix).append(column.getName());
        prefix = ",";
      }
    }
    sql.append(" FROM ");
    sql.append(physicalTable);
    return sql;
  }

  /**
   * For debug. Prints all rows returned by given SQL
   */
  public static void printAll(SQLiteConnection db, SQLParts sql, PrintStream stream, Object... params)
    throws SQLiteException
  {
    SQLiteStatement statement = db.prepare(sql, false);
    try {
      if (params != null)
        for (int i = 0; i < params.length; i++)
          bindParameter(statement, i + 1, params[i]);
      while (statement.step()) {
        for (int i = 0; i < statement.columnCount(); i++) {
          stream.print(statement.columnValue(i));
          stream.print("|");
        }
        stream.println();
      }
    } finally {
      statement.dispose();
    }
  }

  public static void printAll(SQLiteConnection db, SQLParts sql, Object... params) throws SQLiteException {
    printAll(db, sql, System.out, params);
  }

  public static void printAll(SQLiteConnection db, String sql, Object... params) throws SQLiteException {
    printAll(db, new SQLParts(sql), params);
  }

  protected static StringBuilder columns(StringBuilder sql, DBColumn[] dbLoadColumns) {
    String prefix = "";
    for (DBColumn dbLoadColumn : dbLoadColumns) {
      sql.append(prefix).append(dbLoadColumn.getName());
      prefix = ", ";
    }
    return sql;
  }

  protected static SQLParts columns(SQLParts sql, DBColumnSet dbLoadColumns) {
    String prefix = "";
    for (int i = 0; i < dbLoadColumns.size(); i++) {
      sql.append(prefix).append(dbLoadColumns.get(i).getName());
      prefix = ", ";
    }
    return sql;
  }

  protected static SQLParts columns(SQLParts sql, DBColumn firstColumn, DBColumnSet dbLoadColumns) {
    sql.append(firstColumn.getName());
    for (int i = 0; i < dbLoadColumns.size(); i++) {
      sql.append(", ").append(dbLoadColumns.get(i).getName());
    }
    return sql;
  }

  protected static String columns(DBColumn[] columns) {
    return columns(new StringBuilder(), columns).toString();
  }

  public static long[] getLongBuffer(SessionContext context, int minimumSize) {
    if (minimumSize <= 0)
      return Const.EMPTY_LONGS;
    BufferKey key = null;
    if (minimumSize < MAX_CACHEABLE_BUFFER_SIZE) {
      for (BufferKey bkey : BUFFER_KEYS) {
        if (bkey.size() >= minimumSize) {
          key = bkey;
          break;
        }
      }
    }
    if (key == null)
      return new long[minimumSize];
    long[] buffer = context == null ? null : (long[]) context.getSessionCache().remove(key);
    return buffer == null ? new long[key.size()] : buffer;
  }

  public static void releaseLongBuffer(SessionContext sessionContext, long[] buffer) {
    if (buffer == null || sessionContext == null) return;
    int len = buffer.length;
    if (len == 0 || len > MAX_CACHEABLE_BUFFER_SIZE) return;
    for (BufferKey bkey : BUFFER_KEYS) {
      if (bkey.size() > len) break;
      if (bkey.size() == len) {
        sessionContext.getSessionCache().put(bkey, buffer);
        break;
      }
    }
  }

  public static void replaceParts(SQLParts from, SQLParts to, Map<String, String> replacement) {
    if (replacement == null || replacement.isEmpty()) {
      to.append(from);
      return;
    }
    for (String part : from.getParts()) {
      String r = replacement.get(part);
      to.append(r == null ? part : r);
      assert r == null || r.equals(part) || checkPartNotContainsReplacement(replacement, part);
    }
  }

  private static boolean checkPartNotContainsReplacement(Map<String, String> replacement, String part) {
    for (String k : replacement.keySet()) {
      if (!part.equals(k) && part.contains(k)) {
        Log.warn("replaced part " + k + " is mentioned in part " + part);
      }
    }
    return true;
  }

  public static SQLParts replacePart(SQLParts sql, String search, String replace) {
    SQLParts replacement = new SQLParts();
    replacePart(sql, replacement, search, replace);
    if (sql.isFixed()) replacement.fix();
    return replacement;
  }

  public static void replacePart(SQLParts from, SQLParts to, String search, String replace) {
    for (String part : from.getParts()) {
      if (part.equals(search)) {
        to.append(replace);
      } else {
        if (part.indexOf(search) >= 0) {
          Log.warn("replaced part [" + search + "] is used not separately [" + part + "]");
        }
        to.append(part);
      }
    }
  }

  private static class BufferKey {
    private final int mySize;

    private BufferKey(int size) {
      mySize = size;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      BufferKey bufferKey = (BufferKey) o;

      if (mySize != bufferKey.mySize)
        return false;

      return true;
    }

    @Override
    public int hashCode() {
      return mySize;
    }

    public int size() {
      return mySize;
    }

    @Override
    public String toString() {
      return "buffer[" + mySize + "]";
    }

    public static BufferKey[] initialize() {
      BufferKey[] r = new BufferKey[MAX_BUFFER_SIZE2 - MIN_BUFFER_SIZE2 + 1];
      int sz2 = 1 << MIN_BUFFER_SIZE2;
      for (int i = 0; i < r.length; i++) {
        r[i] = new BufferKey(sz2 - ARRAY_OVERHEAD8);
        sz2 <<= 1;
      }
      return r;
    }
  }
}
