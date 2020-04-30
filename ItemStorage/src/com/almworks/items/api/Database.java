package com.almworks.items.api;

import com.almworks.items.util.DBNamespace;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.properties.Role;
import org.almworks.util.detach.Lifespan;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Database is the entry point to the ItemStorage API. It is registered with component container,
 * and for test purposes an instance of SQLiteDatabase can be created manually.
 * <p>
 * <strong>Job Queues:</strong> Normally, there are two job queues -
 * one for background tasks and for write transactions ("main"), and the other only for foreground read transactions
 * ("view"). Each queue has a separate thread and SQLite session. The reason for separation is that foreground reads should
 * execute as quickly as possible (the user is waiting), while background transactions may take considerable time
 * (synchronization).
 */
public interface Database {
  Role<Database> ROLE = Role.role(Database.class.getName(), Database.class);

  DBNamespace NS = DBNamespace.moduleNs("com.almworks.items.api");

  /**
   * Places read transaction in the job queue and returns Future result.
   *
   * @param priority defines the priority of the job. Normally, foreground priority places read transaction in the
   * <b>view</b> job queue, and background priority places transaction in the <b>main</b> queue.
   * @param transaction the transaction
   * @param <T> type of the result, extractable from DBResult. If not needed, use Object or Void.
   * @return an instance of DBResult, which is used to control the execution and get the result
   */
  <T> DBResult<T> read(DBPriority priority, ReadTransaction<T> transaction);

  /**
   * Places write transaction in the job queue and returns Future result.
   *
   * @param priority defines the priority of the job. Write transactions are always placed in the <b>main</b>
   * queue. For foreground transactions, the relative priority is bumped up a little to place them before background
   * write transactions.
   * @param transaction the transaction
   * @param <T> type of the result, extractable from DBResult. If not needed, use Object or Void.
   * @return an instance of DBResult, which is used to control the execution and get the result
   * @see DBPriority#toBackgroundPriority()
   */
  <T> DBResult<T> write(DBPriority priority, WriteTransaction<T> transaction);

  /**
   * Registers a DBListener with the database, which will receive updates after database is updated.
   * <p>
   * Because there's no underlying query, the listener will receive DBEvents with only 
   * <code>changed</code> component (no <code>added</code> or <code>removed</code>).
   * <p>
   * The listener receives events in a separate read transaction, which is run with the FOREGROUND priority. Database
   * will make best effort to call the listener each time the database changes, however, there's no obligation.
   * (A test case may be created where listener does not get called at all.) The "best effort" contract, therefore,
   * is transitioned to every component that uses this method (caches, live queries) and to the end-user features.
   * <p>
   * There's no defined before-after relationships between calling this method and any write transactions, so
   * the listener may start receiving events at any time during the subscription lifespan. For example, if you register
   * a listener and after that start a write transaction, the listener may or may not receive the update for that
   * transaction.
   * <p>
   * When the listener is called (except for the first time), it's guaranteed that at least one write transaction has
   * happened since the last call to the listener. More than one transaction may have happened, in which case the
   * affected items for all of them are accumulated in one DBEvent.
   * <p>
   * Unlike {@link #liveQuery} listener, there's no forced call to the listener right after its registration. The first
   * call will happen at some point after a real write transaction. If you need to initialize some data, you should
   * either do that on the first call to the listener, or start a separate initializing read transaction by yourself.
   *
   * @param lifespan the lifespan of the subscription
   * @param listener the listener
   */
  void addListener(Lifespan lifespan, DBListener listener);

  /**
   * Register a live query listener, which "watches for" issue that satisfy
   * a given expression.
   * <p>
   * The expression defines a subset of all possible items in the database.
   * The listener is notified whenever items are added to the subset, removed
   * from the subset, or when items that are in the subset are changed. The
   * listener will not be notified about items that are not (and were not) in
   * the subset.
   * <p>
   * Like with {@link #addListener} method, the listener is called from
   * a read transaction that follows one or many write transaction. The read
   * transaction runs with FOREGROUND priority.
   * <p>
   * Like {@link #addListener} method, the contract for this method does not
   * tell when the listener will be called. However, when it is called, the
   * DBEvent parameter will contain all changes that happened since the last call.
   * <p>
   * <strong>Forced call:</strong> Immediately after this method is called, a read transaction is scheduled
   * to perform initial read-out. The first event received by the listener
   * will happen soon and DBEvent will contain all items matching the expression
   * in <code>changed</code> attribute.
   * <p>
   * This method returns DBLiveQuery object, which can be used from any thread to
   * check "current" state of the query result.
   *
   * The listener will receive updates each time an item begins to
   *
   * @param lifespan the lifespan of the subscription
   * @param expr the query
   * @param listener the listener
   * @return live query
   */
  DBLiveQuery liveQuery(Lifespan lifespan, BoolExpr<DP> expr, DBLiveQuery.Listener listener);

  /**
   * The application may call this method with <code>true</code> parameter if it detects
   * that the computer is idle, and it's a good time to do a long-running maintenance.
   * <p>
   * Calling this method with <code>false</code> flag advises database to stop maintenance
   * if it is running.
   *
   * @param housekeepingAllowed true if database can do long-running maintenance
   */
  void setLongHousekeepingAllowed(boolean housekeepingAllowed);

  void dump(PrintStream writer) throws IOException;

  void registerTrigger(DBTrigger trigger);

  UserDataHolder getUserData();

  /**
   * Creates an instance of DBFilter, which is simply a wrapper around expression
   * and database.
   */
  DBFilter filter(BoolExpr<DP> expr);

  <T> DBResult<T> readForeground(ReadTransaction<T> transaction);

  <T> DBResult<T> readBackground(ReadTransaction<T> transaction);

  <T> DBResult<T> writeBackground(WriteTransaction<T> transaction);

  <T> DBResult<T> writeForeground(WriteTransaction<T> transaction);

  void dump(String filename);

  public String dumpString();

  boolean isDbThread();
}
