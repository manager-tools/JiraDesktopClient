package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.*;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Failure;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Schema {
  // GLOBALS table stores singleton values
  public static final String GLOBALS = "_globals";
  public static final DBIntColumn GLOBALS_ID = new DBIntColumn("id");
  public static final DBStringColumn GLOBALS_KEY = new DBStringColumn("key");
  public static final DBAnyTypeColumn GLOBALS_VALUE = new DBAnyTypeColumn("value");

  // ITEMS table
  public static final String ITEMS = "_items";
  public static final DBLongColumn ITEMS_ITEM = DBColumn.ITEM;
  public static final DBLongColumn ITEMS_LAST_ICN = new DBLongColumn("icn");

  // TABLES table contains information of all dynamic tables
  public static final String TABLES = "_tables";
  public static final DBIntColumn TABLE_ID = new DBIntColumn("table_id");
  public static final DBStringColumn TABLES_NAME = new DBStringColumn("name");
  public static final DBIntColumn TABLES_VERSION = new DBIntColumn("version");
  public static final DBStringColumn TABLES_PHYSICAL_TABLE = new DBStringColumn("physical");

  /**
   * Database Identifier
   */
  public static final SystemProperty<Integer> DIN =
    new SystemProperty<Integer>(-1, "com.almworks.items.api:p:din", Integer.class) {
      protected Integer initializeValue(SQLiteConnection db, Integer din) {
        return din != null && din != 0 ? null : generateDin();
      }

      public String formatValue(Integer value) {
        return formatDin(value == null ? 0 : value);
      }
    };

  /**
   * Item Change Number
   */
  public static final SystemProperty<Long> NEXT_ICN =
    new SystemProperty<Long>(-2, "com.almworks.items.api:p:icn", Long.class) {
      protected Long initializeValue(SQLiteConnection db, Long icn) throws SQLiteException {
        long minIcn = nextItemIcn(db);
        if (icn != null && icn == minIcn) return null;
        if (icn == null) {
          Log.debug(this + ": resetting ICN to " + minIcn);
          return minIcn;
        }
        if (icn < minIcn) {
          Log.warn(this + ": db ICN is " + icn + ", resetting to " + minIcn);
          return minIcn;
        }
        Log.warn(this + ": keeping large ICN " + icn + ", minICN " + minIcn);
        return null;
      }
    };

  /**
   * Table change id: higher int is change sequential number, lower int is change unixtime
   */
  public static final SystemProperty<Long> TCID = new SystemProperty<Long>(-3, "com.almworks.items.api:p:tcid", Long.class) {
    protected Long initializeValue(SQLiteConnection db, Long tcid) throws SQLiteException {
      return tcid == null || tcid <= 0 ? createTcid(0) : null;
    }

    public String formatValue(Long value) {
      return formatTcid(value);
    }
  };

  private static final DateFormat TCID_DATE_FORMAT = new SimpleDateFormat("yyMMdd-HHmmss");

  private static final SQLParts SELECT_SYSTEM_PROPERTY = new SQLParts().append("SELECT ")
    .append(GLOBALS_VALUE.getName()).append(" FROM ").append(GLOBALS)
    .append(" WHERE ").append(GLOBALS_ID.getName()).append(" = ?").fix();

  private static final SQLParts SELECT_PROPERTY =
    new SQLParts().append("SELECT value FROM ").append(Schema.GLOBALS).append(" WHERE key = ?").fix();

  private static final SQLParts UPDATE_SYSTEM_PROPERTY = new SQLParts().append("INSERT OR REPLACE INTO ")
    .append(Schema.GLOBALS)
    .append(" (")
    .append(Schema.GLOBALS_KEY.getName())
    .append(", ")
    .append(Schema.GLOBALS_VALUE.getName())
    .append(", ").append(Schema.GLOBALS_ID.getName()).append(") values (?, ?, ?)").fix();

  private static final SQLParts UPDATE_PROPERTY = new SQLParts().append("INSERT OR REPLACE INTO ")
    .append(Schema.GLOBALS)
    .append(" (")
    .append(Schema.GLOBALS_KEY.getName())
    .append(", ")
    .append(Schema.GLOBALS_VALUE.getName())
    .append(") values (?, ?)").fix();


  private static DBTable createGLOBALS() {
    DBTableBuilder builder =
      DBTableBuilder.createPrimaryBuilder(Schema.GLOBALS, GLOBALS_ID);
    builder.colNN(GLOBALS_KEY).unique(GLOBALS_KEY);
    builder.cols(GLOBALS_VALUE);
    return builder.create();
  }

  private static DBTable createITEMS() {
    DBTableBuilder builder = DBTableBuilder.createPrimaryBuilder(Schema.ITEMS, ITEMS_ITEM);
    builder.colNN(ITEMS_LAST_ICN);
    builder.index(ITEMS_LAST_ICN);
    return builder.create();
  }

  private static DBTable createTABLES() {
    DBTableBuilder builder = DBTableBuilder.createPrimaryBuilder(Schema.TABLES, TABLE_ID);
    builder.column(TABLES_NAME, true)
      .column(TABLES_VERSION, true)
      .column(TABLES_PHYSICAL_TABLE, true);
    builder.unique(TABLES_NAME, TABLES_VERSION);
    return builder.create();
  }

  // table definitions
  public static interface Def {

    DBTable GLOBALS = createGLOBALS();

    DBTable ITEMS = createITEMS();

    // DB-11 primary key isn't integer
    // todo primary key (name, version) ?
    DBTable TABLES = createTABLES();
  }

  /**
   * Make sure all statically defined tables are there.
   */
  public static int validate(TransactionContext context) throws SQLiteException {
    try {
      SQLiteConnection db = context.getConnection();
      TableValidator.validateOrFail(db, GLOBALS, Def.GLOBALS, true, true);
      TableValidator.validateOrFail(db, ITEMS, Def.ITEMS, true, true);
      TableValidator.validateOrFail(db, TABLES, Def.TABLES, true, true);
      for (SystemProperty prop : SystemProperty.all()) {
        prop.initialize(db);
      }
      return getProperty(db, DIN);
    } catch (TableValidator.Failed e) {
      Log.error("cannot open database", e);
      throw new Failure(e);
    }
  }


  private static long nextItemIcn(SQLiteConnection db) throws SQLiteException {
    SQLiteStatement st = db
      .prepare(new SQLParts().append("SELECT max(").append(ITEMS_LAST_ICN.getName()).append(") FROM ").append(ITEMS), false);
    try {
      boolean b = st.step();
      if (b) {
        return st.columnNull(0) ? 0 : st.columnLong(0) + 1;
      } else {
        assert false;
        return 0;
      }
    } finally {
      st.dispose();
    }
  }

  static String formatDin(int din) {
    return String.format("%02X-%02X-%02X-%02X", (din >> 24) & 0xFF, (din >> 16) & 0xFF, (din >> 8) & 0xFF, din & 0xFF);
  }

  static String formatTcid(long tcid) {
    StringBuilder r = new StringBuilder();
    int changeNumber = getTcidSequence(tcid);
    long changeTime = getTcidMillis(tcid);
    r.append(changeNumber);
    if (changeTime > 0) {
      r.append(':').append(TCID_DATE_FORMAT.format(new Date(changeTime)));
    }
    return r.toString();
  }

  private static long getTcidMillis(long tcid) {
    return (tcid & 0xFFFFFFFFL) * 1000L;
  }

  public static int getTcidSequence(long tcid) {
    return (int) ((tcid >> 32) & 0x7FFFFFFFL);
  }

  public static long createTcid(int sequence) {
    assert sequence >= 0 : sequence;
    int time = (int) (System.currentTimeMillis() / 1000);
    long tcid = (((long) sequence) << 32) | (((long) time) & 0xFFFFFFFFL);
    return tcid;
  }

  static int generateDin() {
    return (int) ((System.currentTimeMillis() * 7817L)) ^ (int) (System.nanoTime() * 7247) ^
      (new Object().hashCode() * 7283) ^ Util.NN(System.getProperty("java.home")).hashCode() ^
      Util.NN(System.getProperty("user.home")).hashCode();
  }

  @Nullable
  public static <T> T getProperty(SQLiteConnection db, DBProperty<T> property) throws SQLiteException {
    StatementAccessor<T> accessor = StatementAccessor.forClass(property.getValueClass());
    if (accessor == null) {
      assert false : property;
      return null;
    }
    SQLiteStatement st = SQLiteStatement.DISPOSED;
    try {
      if (property instanceof SystemProperty) {
        st = db.prepare(SELECT_SYSTEM_PROPERTY, true);
        st.bind(1, ((SystemProperty) property).getId());
      } else {
        st = db.prepare(SELECT_PROPERTY, true);
        st.bind(1, property.getName());
      }
      return st.step() ? accessor.column(st, 0) : null;
    } finally {
      st.dispose();
    }
  }

  public static <T> void setProperty(SQLiteConnection db, DBProperty<T> property, @Nullable T value)
    throws SQLiteException
  {
    StatementAccessor<T> accessor = StatementAccessor.forClass(property.getValueClass());
    if (accessor == null) {
      assert false : property;
      return;
    }
    SQLiteStatement st = SQLiteStatement.DISPOSED;
    try {
      if (property instanceof SystemProperty) {
        st = db.prepare(UPDATE_SYSTEM_PROPERTY, true);
        st.bind(3, ((SystemProperty) property).getId());
      } else {
        st = db.prepare(UPDATE_PROPERTY, true);
      }
      st.bind(1, property.getName());
      accessor.bind(st, 2, value);
      st.step();
    } finally {
      st.dispose();
    }
  }
}