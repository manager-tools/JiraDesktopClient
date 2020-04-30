package com.almworks.items.impl.sqlite;

import com.almworks.integers.*;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.items.impl.AttributeAdapter;
import com.almworks.items.impl.AttributeCache;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBProperty;
import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.sqlite4java.*;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.almworks.items.impl.sqlite.Schema.*;

/**
 * Provides transactional access to SQLite. Where this object is passed, the transaction has been started.
 */
public class TransactionContext implements TableResolver {
  private static final TypedKey<ChangedItemsCollector> CHANGE_BUILDER =
    TypedKey.create("CIC", ChangedItemsCollector.class);

  private final boolean myWriteAllowed;
  private SessionContext mySessionContext;
  private SQLiteConnection myConnection;
  private Map<TypedKey<?>, ?> myTransactionCache = new HashMap();

  // protected with lock (this)
  private List<SQLiteStatement> myCancellable;
  private boolean myCancelled;

  private final long myTransactionTime;
  private long myIcn = -1;

  private ChangedItemsCollector myChangeBuilder;
  private SQLParts myReusableSQL = new SQLParts();
  private boolean myReusableSQLUsed;


  public static final String ICN_UPDATE_ARRAY = "__ICNUPDATE";
  private static final SQLParts ICN_UPDATE_SQL = new SQLParts().append("INSERT OR REPLACE INTO ")
    .append(ITEMS)
    .append(" (").append(ITEMS_ITEM.getName()).append(",").append(ITEMS_LAST_ICN.getName())
    .append(") SELECT value, ? FROM ").append(ICN_UPDATE_ARRAY);

  private static final SQLParts SELECT_CHANGED_SQL = new SQLParts().append("SELECT ")
    .append(ITEMS_ITEM.getName())
    .append(" FROM ")
    .append(ITEMS)
    .append(" WHERE ")
    .append(ITEMS_LAST_ICN.getName())
    .append(" >= ?");
  private static final SQLParts SELECT_CHANGED_STRICT_SQL = new SQLParts().append("SELECT ")
    .append(ITEMS_ITEM.getName())
    .append(" FROM ")
    .append(ITEMS)
    .append(" WHERE ")
    .append(ITEMS_LAST_ICN.getName())
    .append(" BETWEEN ? AND ?");

  private boolean myChangeFlushDisabled;
  private boolean myPropagationDisabled;


  public TransactionContext(SQLiteConnection connection, SessionContext sessionContext, boolean writeAllowed) {
    myConnection = connection;
    mySessionContext = sessionContext;
    myWriteAllowed = writeAllowed;
    myTransactionTime = System.currentTimeMillis();
  }

  public SessionContext getSessionContext() {
    return mySessionContext;
  }

  public DatabaseContext getDatabaseContext() {
    SessionContext context = mySessionContext;
    return context == null ? null : context.getDatabaseContext();
  }

  private ChangedItemsCollector getChangeBuilder() {
    ChangedItemsCollector changeBuilder = myChangeBuilder;
    if (changeBuilder == null) {
      Map sessionCache = getSessionContext().getSessionCache();
      ChangedItemsCollector builder = (ChangedItemsCollector) sessionCache.remove(CHANGE_BUILDER);
      if (builder == null) {
        builder = new ChangedItemsCollector();
      }
      myChangeBuilder = changeBuilder = builder;
      assert !changeBuilder.hasChanges() : changeBuilder;
    }
    return changeBuilder;
  }

  /**
   * @return time of transaction, to be used for all time-based queries
   */
  public long getTransactionTime() {
    return myTransactionTime;
  }

  public Map<TypedKey<?>, ?> getTransactionCache() {
    return myTransactionCache;
  }

  public SQLiteStatement prepare(SQLParts parts) throws SQLiteException {
    if (!checkDisposed()) {
      // ?
    }
    assert checkSql(parts.toString()) : parts;
    checkCancelled();
    SQLiteStatement r = myConnection.prepare(parts);
    releaseSql(parts);
    return r;
  }

  private boolean checkSql(String sql) {
    // very rough checking of sql without syntax parsing
    // to make sure
    sql = Util.lower(sql).trim();
    return !(sql.startsWith("commit") || sql.startsWith("rollback") || sql.startsWith("begin"));
  }

  public <T> T getProperty(DBProperty<T> property) throws SQLiteException {
    if (!checkDisposed())
      return null;
    T cached = property.getFrom(myTransactionCache);
    if (cached != null) {
      return cached;
    }
    T value = Schema.getProperty(myConnection, property);
    property.putTo(myTransactionCache, value);
    return value;
  }

  public long getLongProperty(DBProperty<Long> property, long defaultValue) throws SQLiteException {
    Long r = getProperty(property);
    return r == null ? defaultValue : r;
  }

    private boolean checkDisposed() {
    if (myConnection == null) {
      assert false : this;
      return false;
    }
    return true;
  }

  public <T> void setProperty(DBProperty<T> property, T value) throws SQLiteException {
    if (!checkDisposed())
      return;
    assert myWriteAllowed : this;
    Schema.setProperty(myConnection, property, value);
    property.putTo(myTransactionCache, value);
  }

  public void dispose() {
    ChangedItemsCollector changeBuilder = myChangeBuilder;
    if (changeBuilder != null) {
      changeBuilder.cleanUp();
      mySessionContext.getSessionCache().put(CHANGE_BUILDER, changeBuilder);
      myChangeBuilder = null;
    }
    myConnection = null;
    mySessionContext = null;
    myTransactionCache = null;
  }

  public SQLiteConnection getConnection() {
    return myConnection;
  }

  public TableInfo getTableInfo(DBTable definition, boolean allowCreate) throws SQLiteException {
    assert myWriteAllowed || !allowCreate : this;
    TableManager tm = TableManager.getFromContext(mySessionContext.getSessionCache());
    return tm.getTableInfo(this, definition, allowCreate);
  }

  public String getTableName(DBTable definition, boolean allowCreate) throws SQLiteException {
    assert myWriteAllowed || !allowCreate : this;
    TableInfo ti = getTableInfo(definition, allowCreate);
    return ti == null ? null : ti.getPhysicalTable();
  }

  public long getIcn() throws SQLiteException {
    if (myIcn < 0)
      myIcn = Util.NN(getProperty(Schema.NEXT_ICN), 0L);
    return myIcn;
  }

  public LongList getChangedItemsSorted(long fromIcn) throws SQLiteException {
    long icn = getIcn();
    if (fromIcn > icn)
      return LongList.EMPTY;
    boolean changes = hasItemChanges();
    if (fromIcn == icn && !changes)
      return LongList.EMPTY;
    if (changes) {
      flushChangedItemsICN();
    }
    SQLiteStatement st = prepare(SELECT_CHANGED_SQL);
    try {
      st.bind(1, fromIcn);
      LongSetBuilder r = new LongSetBuilder();
      SQLUtil.loadLongs(st, this, r);
      return r.toList();
    } finally {
      st.dispose();
    }
  }

  public void flushChangedItemsICN() throws SQLiteException {
    if (myChangeFlushDisabled)
      return;
    List<DBAttribute> propagating = getPropagatedAttributes();
    ChangedItemsCollector changeBuilder = myChangeBuilder;
    if (changeBuilder == null || !changeBuilder.hasChanges())
      return;
    LongSetBuilder totalCollector = new LongSetBuilder();
    LongArray workingSet = new LongArray();
    while (true) {
      workingSet.clear();
      LongList changed = changeBuilder.drainChangedItemsSorted();
      if (changed.isEmpty())
        break;
      LongList flushed = totalCollector.toList();
      workingSet.addAll(new LongMinusIterator(changed.iterator(), flushed.iterator()));
      if (workingSet.isEmpty())
        break;
      totalCollector.mergeFromSortedCollection(workingSet);
      propagateChanges(propagating, workingSet);
    }
    updateItemCN(totalCollector.commitToArray());
  }

  private List<DBAttribute> getPropagatedAttributes() throws SQLiteException {
    if (myPropagationDisabled)
      return Collections.emptyList();
    List<DBAttribute> propagating;
    // avoid recursion
    myChangeFlushDisabled = true;
    try {
      propagating = AttributeCache.get(this).getPropagatingAttributes(this);
    } finally {
      myChangeFlushDisabled = false;
    }
    return propagating;
  }

  private void updateItemCN(LongList items) throws SQLiteException {
    if (items.isEmpty())
      return;
    assert myWriteAllowed : this;
    SQLiteLongArray array = useArray(items, ICN_UPDATE_ARRAY, false, false);
    SQLiteStatement st = null;
    try {
      st = prepare(ICN_UPDATE_SQL);
      st.bind(1, getIcn());
      st.stepThrough();
    } finally {
      array.dispose();
      if (st != null)
        st.dispose();
    }
  }

  private void propagateChanges(List<DBAttribute> propagating, LongList items) throws SQLiteException {
    SQLiteLongArray array = useArray(items);
    try {
      for (DBAttribute attribute : propagating) {
        if (!Long.class.equals(attribute.getScalarClass())) {
          Log.warn("propagating attribute " + attribute + " is not long");
          continue;
        }
        propagateChanges(attribute, array);
      }
    } finally {
      array.dispose();
    }
  }

  private void propagateChanges(DBAttribute attribute, SQLiteLongArray array) throws SQLiteException {
    ChangedItemsCollector changeBuilder = getChangeBuilder();
    AttributeAdapter adapter = getDatabaseContext().getAttributeAdapter(attribute);
    String table = getTableName(adapter.getTable(), false);
    if (table == null)
      return;
    DBColumn column = adapter.getScalarColumn();
    SQLParts parts = sql().append("SELECT ")
      .append(column.getName())
      .append(" FROM ")
      .append(table)
      .append(" WHERE ")
      .append(DBColumn.ITEM.getName())
      .append(" IN ")
      .append(array.getName());
    SQLiteStatement st = prepare(parts);
    long[] buffer = null;
    try {
      buffer = SQLUtil.getLongBuffer(mySessionContext, 1000);
      int n;
      do {
        n = st.loadLongs(0, buffer, 0, buffer.length);
        for (int i = 0; i < n; i++) {
          changeBuilder.itemChanged(buffer[i]);
        }
      } while (st.hasRow() && n > 0);
    } finally {
      st.dispose();
      SQLUtil.releaseLongBuffer(mySessionContext, buffer);
      releaseSql(parts);
    }
  }


  public boolean hasItemChanges() {
    ChangedItemsCollector builder = myChangeBuilder;
    return builder != null && builder.hasChanges();
  }

  @ThreadSafe
  public void cancel() {
    SQLiteStatement[] stmts;
    synchronized (this) {
      if (myCancelled)
        return;
      myCancelled = true;
      if (myCancellable == null || myCancellable.isEmpty()) {
        return;
      }
      stmts = myCancellable.toArray(new SQLiteStatement[myCancellable.size()]);
    }
    for (SQLiteStatement stmt : stmts) {
      if (stmt != null) {
        Log.debug("cancelling " + stmt);
        stmt.cancel();
      }
    }
  }

  public synchronized void addCancellable(SQLiteStatement stmt) throws SQLiteInterruptedException {
    checkCancelled();
    if (myCancellable == null)
      myCancellable = Collections15.arrayList();
    myCancellable.add(stmt);
  }

  public synchronized void removeCancellable(SQLiteStatement stmt) {
    if (myCancellable != null)
      myCancellable.remove(stmt);
  }

  public synchronized void checkCancelled() throws SQLiteInterruptedException {
    if (myCancelled)
      throw new SQLiteInterruptedException();
  }

  public void execCancellable(String sql) throws SQLiteException {
    SQLiteStatement stmt = myConnection.prepare(sql);
    try {
      addCancellable(stmt);
      stmt.step();
    } finally {
      stmt.dispose();
      removeCancellable(stmt);
    }
  }

  public ExtractionProcessor search(BoolExpr<DP> expr) {
    return ExtractionProcessor.create(expr, this);
  }

  public SQLiteLongArray useArray(LongList values) throws SQLiteException {
    return useArray(values, false, false);
  }

  public SQLiteLongArray useArray(LongList values, boolean ordered, boolean unique) throws SQLiteException {
    return useArray(values, null, ordered, unique);
  }

  public SQLiteLongArray useIterable(LongIterable values) throws SQLiteException {
    SQLiteLongArray array = myConnection.createArray();
    return bindIterable(array, values, false, false);
  }

  public SQLiteLongArray useArray(LongList values, String name, boolean ordered, boolean unique) throws SQLiteException {
    SQLiteLongArray array = myConnection.createArray(name, true);
    return bindArray(array, values, ordered, unique);
  }

  public SQLiteLongArray bindArray(SQLiteLongArray array, LongList values, boolean ordered, boolean unique) throws SQLiteException {
    int n = values.size();
    long[] buffer = SQLUtil.getLongBuffer(getSessionContext(), n);
    try {
      assert buffer.length >= n;
      values.toNativeArray(0, buffer, 0, n);
      try {
        array.bind(buffer, 0, n, ordered, unique);
      } catch (SQLiteException e) {
        array.dispose();
        throw e;
      }
    } finally {
      SQLUtil.releaseLongBuffer(getSessionContext(), buffer);
    }
    return array;
  }

  public SQLiteLongArray bindIterable(SQLiteLongArray array, LongIterable values, boolean ordered, boolean unique) throws SQLiteException {
    SessionContext context = getSessionContext();
    long[] buffer = SQLUtil.getLongBuffer(context, 100);
    try {
      LongArray a = new LongArray(buffer, 0);
      if (values instanceof LongList) {
        a.addAll((LongList) values);
      } else {
        a.addAll(values.iterator());
      }
      int n = a.size();
      long[] vb = a.extractHostArray();
      try {
        array.bind(vb, 0, n, ordered, unique);
      } catch (SQLiteException e) {
        array.dispose();
        throw e;
      }
    } finally {
      SQLUtil.releaseLongBuffer(context, buffer);
    }
    return array;
  }

  public void itemChanged(long item) {
    assert myWriteAllowed : this;
    getChangeBuilder().itemChanged(item);
  }

  public final SQLParts sql() {
    if (myReusableSQLUsed)
      return new SQLParts();
    myReusableSQLUsed = true;
    return myReusableSQL;
  }

  public final void releaseSql(SQLParts parts) {
    if (parts != myReusableSQL)
      return;
    myReusableSQL.clear();
    myReusableSQLUsed = false;
  }

  // used for special transaction during bootstrap to materialize attributes

  public void disablePropagation() {
    myPropagationDisabled = true;
  }

  public boolean isWriteAllowed() {
    return myWriteAllowed;
  }
}
