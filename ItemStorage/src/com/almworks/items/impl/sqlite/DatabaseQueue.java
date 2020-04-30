package com.almworks.items.impl.sqlite;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteInterruptedException;
import com.almworks.sqlite4java.SQLiteProfiler;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.exec.ThreadFactory;
import org.almworks.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.almworks.items.impl.sqlite.DatabaseJob.TransactionType.READ_ROLLBACK;
import static com.almworks.items.impl.sqlite.DatabaseJob.TransactionType.WRITE;

class DatabaseQueue implements DatabaseExecutor {
  public static final TypedKey<Integer> PAGE_SIZE = TypedKey.create("pageSize");
  public static final TypedKey<Integer> CACHE_SIZE = TypedKey.create("cacheSize");
  public static final TypedKey<Long> REINCARNATE_TIMEOUT = TypedKey.create("reincarnateTimeout");
  public static final TypedKey<Long> BUSY_TIMEOUT = TypedKey.create("busyTimeout");

  private static final int DEFAULT_PAGE_SIZE = 4096;
  private static final int DEFAULT_CACHE_SIZE = 2000;
  private static final long DEFAULT_REINCARNATE_TIMEOUT = 3000;
  private static final long DEFAULT_BUSY_TIMEOUT = Const.HOUR;

  private static final int STOP_NOT_REQUESTED = 0;
  private static final int STOP_REQUESTED = 1;
  private static final int STOP_REQUIRED = 2;

  private static final long MAX_STACKED_ROLLBACK_TRANSACTION_DURATION = 500;

  private final Map<TypedKey, ?> myConnectionParameters = Collections15.synchronizedHashMap();
  private final File myDatabaseFile;
  private final String myName;
  private final DatabaseContext myDatabaseContext;

  private final SimpleModifiable myQueueEmptySignal = new SimpleModifiable();

  private final ChangeListener myJobListener = new ChangeListener() {
    public void onChange() {
      checkJobs();
    }
  };

  private final Object myLock = new Object();

  // protected by myLock {
  private final Map<Object, DatabaseJob> myJobs = Collections15.linkedHashMap();
  private DatabaseJob myCurrentJob;
  private Thread myThread;
  private int myStopState = 0;
  // } end of protected by myLock

  /**
   * confined to myThread
   */
  private SQLiteConnection myConnection;
  private SessionContext mySessionContext;
  private TransactionContext myCurrentTransaction;
  private long myCurrentTransactionStartTime;
  private SQLiteProfiler myProfiler;

  public DatabaseQueue(@Nullable File databaseFile, String name, DatabaseContext databaseContext) {
    myDatabaseFile = databaseFile;
    myName = name;
    myDatabaseContext = databaseContext;
  }

  public String toString() {
    return "DQ[" + myName + "]";
  }

  public <T> void setParameter(TypedKey<T> key, T value) {
    key.putTo(myConnectionParameters, value);
  }

  private <T> T getParameter(TypedKey<T> key, T defaultValue) {
    T r = key.getFrom(myConnectionParameters);
    return r == null ? defaultValue : r;
  }

  public void start() {
    synchronized (myLock) {
      if (myThread != null) {
        return;
      }
      myThread = ThreadFactory.create(myName, new Runnable() {
        public void run() {
          runQueue();
        }
      });
      myStopState = STOP_NOT_REQUESTED;
    }
    myThread.start();
  }

  public void stop(boolean gracefully) {
    DatabaseJob currentJob = null;
    List<DatabaseJob> removedJobs = null;

    synchronized (myLock) {
      myStopState = Math.max(myStopState, gracefully ? STOP_REQUESTED : STOP_REQUIRED);
      Thread thread = myThread;
      if (thread == null) {
        return;
      }
      if (!gracefully) {
        removedJobs = Collections15.arrayList(myJobs.values());
        myJobs.clear();
        currentJob = myCurrentJob;
      }
      myLock.notify();
    }
    if (currentJob != null) {
      currentJob.cancel();
    }

    finishJobs(currentJob, removedJobs);
  }

  private void finishJobs(DatabaseJob currentJob, List<DatabaseJob> jobs) {
    if (currentJob != null) currentJob.setFinished(false);
    if (jobs != null) {
      for (DatabaseJob job : jobs) {
        job.setFinished(false);
      }
    }
  }

  public void waitFor() throws InterruptedException {
    Thread thread;
    synchronized (myLock) {
      thread = myThread;
    }
    if (thread == Thread.currentThread()) {
      assert false : this;
      return;
    }
    if (thread != null) {
      thread.join();
    }
  }

  public void flush() throws InterruptedException {
    synchronized (myLock) {
      while (!myJobs.isEmpty() || myCurrentJob != null) {
        myLock.wait(1000);
        myLock.notify();
      }
    }
  }

  /**
   * This method returns modifiable that gets onChange() fired every time this queue gets empty.
   * Take care when subscribing with STRAIGHT thread gate: make it quick!
   */
  public Modifiable getQueueEmptySignal() {
    return myQueueEmptySignal;
  }

  private void runQueue() {
    try {
      queueFunction();
    } catch (InterruptedException e) {
      Log.warn(this + " interrupted", e);
    } catch (RuntimeException e) {
      Log.error(e);
    } catch (Error e) {
      Log.error(e);
      if (e instanceof ThreadDeath)
        throw e;
    } catch (SQLiteException e) {
      Log.warn(e);
    } finally {
      threadStopped();
    }
  }

  private void queueFunction() throws SQLiteException, InterruptedException {
    Log.debug(this + " started");
    createConnection();
    createSessionContext();
    initConnection(myConnection);

    // this var is needed to run empty-queue signal outside myLock
    // 0 = not fired, 1 = fire now!, 2 = just fired (reverse to 0 when job is processed)
    int fireEmptyState = 0;

    while (true) {
      DatabaseJob job;
      synchronized (myLock) {
        clearCurrentJob();
        while (true) {
          if (myStopState == STOP_REQUIRED || (myStopState == STOP_REQUESTED && myJobs.isEmpty())) {
            // stop thread
            return;
          }
          job = selectJob(true);
          if (job != null) {
            myCurrentJob = job;
            break;
          }
          if (myCurrentTransaction != null) {
            // break to finish READ_ROLLBACK
            break;
          }
          assert fireEmptyState == 0 || fireEmptyState == 2;
          if (fireEmptyState == 0) {
            // fire signal and continue
            fireEmptyState = 1;
            break;
          }
          myLock.wait(1000);
          myLock.notify();
        }
      }

      if (fireEmptyState == 1) {
        myQueueEmptySignal.fireChanged();
        fireEmptyState = 2;
        continue;
      }
      fireEmptyState = 0;

      if (job == null) {
        assert myCurrentTransaction != null;
        rollback();
        continue;
      }

      assert job != null;
      assert myCurrentJob != null;

      runJob(job);
    }
  }

  private void createSessionContext() {
    mySessionContext = new SessionContext(myDatabaseContext);
  }

  private void createConnection() {
    if (myConnection != null) {
      assert false;
      myConnection.dispose();
      myConnection = null;
    }
    myConnection = new SQLiteConnection(myDatabaseFile);
    if (myDatabaseContext.getConfiguration().isProfilingEnabled()) {
      myProfiler = myConnection.profile();
    }
  }

  private void rollback() {
    disposeContext();
    try {
      myConnection.exec("ROLLBACK");
    } catch (SQLiteException e) {
      // ignore
    }
  }

  private void disposeContext() {
    TransactionContext context = myCurrentTransaction;
    assert context != null : this;
    myCurrentTransaction = null;
    myCurrentTransactionStartTime = 0;
    if (context != null) {
      context.dispose();
    }
  }

  private DatabaseJob selectJob(boolean remove) {
    assert Thread.holdsLock(myLock);
    if (myJobs.isEmpty())
      return null;
    DatabaseJob best = null;
    Object bestKey = null;
    int bestPriority = Integer.MIN_VALUE;
    for (Iterator<Map.Entry<Object, DatabaseJob>> ii = myJobs.entrySet().iterator(); ii.hasNext();) {
      Map.Entry<Object, DatabaseJob> entry = ii.next();
      DatabaseJob job = entry.getValue();
      if (job.getState() != DatabaseJob.State.PENDING) {
        // i.e. remove cancelled
        ii.remove();
        continue;
      }
      // todo check if runnable
      int priority = job.getPriority();
      if (best != null) {
        if (priority < bestPriority) {
          continue;
        } else if (priority == bestPriority) {
          // todo choose the quickest, maintain statistics <jobclass> -> <timing>
          continue;
        }
      }
      best = job;
      bestKey = entry.getKey();
      bestPriority = priority;
    }
    if (best != null && remove) {
      assert bestKey != null;
      DatabaseJob removed = myJobs.remove(bestKey);
      assert removed == best : removed + " " + best;
      best = removed;
    }
    return best;
  }

  private DatabaseJob clearCurrentJob() {
    assert Thread.holdsLock(myLock);
    DatabaseJob lastJob = myCurrentJob;
    myCurrentJob = null;
    if (lastJob != null) {
      lastJob.setListener(null);
      myLock.notify();
    }
    return lastJob;
  }

  private void runJob(DatabaseJob job) throws InterruptedException, SQLiteException {
    long now = System.currentTimeMillis();
    DatabaseJob.TransactionType type = job.getTransactionType();
    if (myCurrentTransaction != null) {
      // todo somehow assert that the last transaction was READ_ROLLBACK
      if (type != READ_ROLLBACK || (now - myCurrentTransactionStartTime) > MAX_STACKED_ROLLBACK_TRANSACTION_DURATION) {
        rollback();
      }
    }
    if (type == READ_ROLLBACK) {
      if (myCurrentTransaction == null) {
        begin(false);
      }
    } else {
      assert myCurrentTransaction == null : myCurrentTransaction + " " + job;
      begin(type == WRITE);
    }
    assert myCurrentTransaction != null;
    boolean clearContext = true;
    boolean success = false;
    try {
      job.execute(myCurrentTransaction);
      boolean cancelled = job.isCancelled();
      if (type != READ_ROLLBACK) {
        if (cancelled) {
          rollback();
        } else {
          commit();
        }
      }
      job.setFinished(!cancelled);
      success = true;
      clearContext = cancelled;
    } catch (Throwable e) {
      rollback();
      handleJobException(job, e);
      if (e instanceof SQLiteInterruptedException && type == READ_ROLLBACK) {
        clearContext = false;
      }
    } finally {
      if (!success) {
        job.setFinished(false);
      }
      //noinspection CatchGenericClass
/*
      try {
        Throwable error = job.getError();
        SQLiteException sqliteError = job.getSqliteError();
        if (error != null || sqliteError != null) {
          warnJobError(job, error, sqliteError);
        }
      } catch (Exception e) {
        Log.error(e);
        // ignore
      }
*/
      if (clearContext) {
        clearSessionContext();
      }
    }
  }

  private void clearSessionContext() {
    Log.debug(this + ": clearing context");
    SessionContext context = mySessionContext;
    if (context != null) {
      context.clear();
    }
  }

  private void warnJobError(DatabaseJob job, Throwable error, SQLiteException sqliteError) {
    if (sqliteError != null) {
      Log.warn(this + " " + job, sqliteError);
    } else {
      Log.warn(this + " " + job, error);
    }
  }

  private void commit() throws SQLiteException {
    disposeContext();
    try {
      myConnection.exec("COMMIT");
    } catch (SQLiteException e) {
      if (myConnection.getAutoCommit()) {
        assert false : this;
      } else {
        throw e;
      }
    }
  }

  private void begin(boolean writeLock) throws SQLiteException {
    assert myCurrentTransaction == null : myCurrentTransaction;
    String sql = writeLock ? "BEGIN IMMEDIATE" : "BEGIN DEFERRED";
    try {
      myConnection.exec(sql);
    } catch (SQLiteException e) {
      if (!myConnection.getAutoCommit()) {
        assert false : this;
        rollback();
        myConnection.exec(sql);
      }
    }
    myCurrentTransaction = new TransactionContext(myConnection, mySessionContext, writeLock);
    myCurrentTransactionStartTime = myCurrentTransaction.getTransactionTime();
  }

  private void handleJobException(DatabaseJob job, Throwable e) throws SQLiteException {
    if (e instanceof DBOperationCancelledException) {
      Log.debug(this + ": job " + job + " cancelled via DBOCE");
      return;
    }
    Log.error("exception when running job " + job, e);
    // detect fatal
    if (e instanceof ThreadDeath)
      throw (ThreadDeath) e;
  }

  private void initConnection(SQLiteConnection db) throws SQLiteException {
    try {
      db.open();
    } catch (SQLiteException e) {
      Log.debug("cannot open " + myConnection, e);
      throw e;
    }
    safeExec(db, "pragma page_size=" + getParameter(PAGE_SIZE, DEFAULT_PAGE_SIZE));
    safeExec(db, "pragma cache_size=" + getParameter(CACHE_SIZE, DEFAULT_CACHE_SIZE));
//    safeExec(db, "pragma synchronous = OFF");
    safeExec(db, "PRAGMA legacy_file_format = OFF");
    safeExec(db, "PRAGMA journal_mode = PERSIST");
    try {
      db.setBusyTimeout(getParameter(BUSY_TIMEOUT, DEFAULT_BUSY_TIMEOUT));
    } catch (SQLiteException e) {
      Log.debug(e);
    }
  }

  private static void safeExec(SQLiteConnection db, String sql) {
    try {
      db.exec(sql);
    } catch (SQLiteException e) {
      Log.debug(e);
    }
  }

  private void threadStopped() {
    assert Thread.currentThread() == myThread;
    mySessionContext = null;
    myCurrentTransaction = null;
    myCurrentTransactionStartTime = 0;
    SQLiteConnection connection = myConnection;
    if (connection != null) {
      myConnection = null;
      try {
        connection.dispose();
      } catch (Exception e) {
        Log.error(e);
      }
    }
    boolean reincarnate;
    DatabaseJob currentJob;
    List<DatabaseJob> droppedJobs = null;
    synchronized (myLock) {
      currentJob = clearCurrentJob();
      reincarnate = myStopState == STOP_NOT_REQUESTED;
      if (reincarnate && myDatabaseFile == null) {
        Log.error(this + " stopped abnormally, reincarnation is not possible for in-memory database");
        reincarnate = false;
      }
      if (!reincarnate) {
        droppedJobs = Collections15.arrayList(myJobs.values());
        myJobs.clear();
      }
      myThread = null;
    }
    if (!reincarnate) {
      finishJobs(currentJob, droppedJobs);
      Log.debug(this + " stopped");
    } else {
      long rt = getParameter(REINCARNATE_TIMEOUT, DEFAULT_REINCARNATE_TIMEOUT);
      Log.warn(this + " stopped abnormally, reincarnating in " + rt + "ms");
      reincarnate(rt);
    }
  }

  private void reincarnate(final long reincarnateTimeout) {
    Thread reincarnator = ThreadFactory.create("reincarnate " + myName, new Runnable() {
      public void run() {
        try {
          Thread.sleep(reincarnateTimeout);
          synchronized (myLock) {
            if (myStopState != STOP_NOT_REQUESTED)
              return;
          }
          DatabaseQueue.this.start();
        } catch (InterruptedException e) {
          Log.debug(myName + " not reincarnated", e);
        }
      }
    });
    reincarnator.start();
  }

  public void execute(DatabaseJob job) {
    if (job == null)
      throw new NullPointerException();
    job.setListener(myJobListener);
    boolean abort = false;
    synchronized (myLock) {
      if (myThread == null || myStopState != STOP_NOT_REQUESTED) {
        Log.debug("job not executed " + job);
        abort = true;
      } else {
        myJobs.put(job.getIdentity(), job);
        myLock.notify();
      }
    }
    if (abort) {
      job.setListener(null);
    }
    checkJobs();
  }

  private void checkJobs() {
    DatabaseJob current;
    boolean hurry = false;
    synchronized (myLock) {
      current = myCurrentJob;
      if (myThread == null || current == null || current.isHurried())
        return;
      DatabaseJob best = selectJob(false);
      if (best != null) {
        hurry = (best.getPriority() > current.getPriority() && !Util.equals(best.getIdentity(), current.getIdentity()));
      }
    }
    if (hurry) {
      current.hurry();
    }
  }

  public boolean isQueueThread() {
    return myThread == Thread.currentThread();
  }

  @NotNull
  public static DatabaseQueue start(File dbFile, String threadName, DatabaseContext databaseContext)
    throws SQLiteException
  {
    DatabaseQueue db = new DatabaseQueue(dbFile, threadName, databaseContext);
    db.start();
    return db;
  }

  public SQLiteProfiler getProfiler() {
    return myProfiler;
  }
}