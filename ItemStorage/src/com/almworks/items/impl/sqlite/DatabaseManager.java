package com.almworks.items.impl.sqlite;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBPriority;
import com.almworks.items.impl.dbadapter.CompoundValueCache;
import com.almworks.items.impl.dbadapter.DBRead;
import com.almworks.items.impl.dbadapter.DBTransaction;
import com.almworks.items.impl.sqlite.cache.ValueCacheManager;
import com.almworks.sqlite4java.*;
import com.almworks.util.Env;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Date;
import java.util.regex.Pattern;

public class DatabaseManager {
  private static final Pattern ALLOWED_TEMP_DIR_NAME = Pattern.compile("\\p{ASCII}+");

  private final String myName;
  @NotNull
  private final DatabaseQueue myMainDB;

  @Nullable
  private final DatabaseQueue myShowDB;

  @Nullable
  private final DatabaseQueue myMonitorDB;

  @Nullable
  private final DatabaseMonitor myMonitor;

  private final int myDin;

  private final DatabaseContext myDatabaseContext;

  private final ValueCacheManager myCacheManager;

  private boolean myStopped;

  // todo QPI should be replaced with filter manager 
  private final QueryProcessorImpl myQueryProcessor;
  private final OldFilterManager myFilterManager;

  private Thread myProfilerThread;

  private DatabaseManager(String name, DatabaseQueue mainDB, @Nullable DatabaseQueue showDB, @Nullable DatabaseQueue monitorDB,
    int din, @Nullable DatabaseContext databaseContext)
  {
    myName = "DM[" + name + "]";
    myMainDB = mainDB;
    myShowDB = showDB;
    myMonitorDB = monitorDB;
    myDin = din;
    myDatabaseContext = databaseContext;
    DatabaseQueue queryDB = showDB != null ? showDB : mainDB;
    myQueryProcessor = new QueryProcessorImpl(queryDB);
    myFilterManager = new SimpleOldFilterManager(myQueryProcessor, databaseContext);
    myCacheManager = new ValueCacheManager(myQueryProcessor);
    myMonitor = myMonitorDB == null ? null : new DatabaseMonitor(myMonitorDB, myQueryProcessor);
  }

  private void start() {
    createArrays(myMainDB, 4, TransactionContext.ICN_UPDATE_ARRAY);
    if (myShowDB != null) createArrays(myShowDB, 4);

    myCacheManager.attach();
    if (myMonitor != null) {
      myMonitor.start();
    }
    if (myDatabaseContext.getConfiguration().isProfilingEnabled()) {
      myProfilerThread = new Thread(new Runnable() {
        public void run() {
          dumpCycle();
        }
      });
      myProfilerThread.setName("db profiler");
      myProfilerThread.start();
    }
  }

  private void createArrays(DatabaseQueue db, final int count, final String... fixedNames) {
    db.execute(new DatabaseJob() {
      @Override
      protected void dbrun(TransactionContext context) throws Throwable {
        SQLiteLongArray[] arrays = new SQLiteLongArray[count];
        for (int i = 0; i < count; i++) {
          arrays[i] = context.getConnection().createArray();
        }
        for (SQLiteLongArray array : arrays) {
          array.dispose();
        }
        for (String name : fixedNames) {
          context.getConnection().createArray(name, true).dispose();
        }
      }

      @Override
      public TransactionType getTransactionType() {
        return TransactionType.WRITE;
      }
    });
  }

  @Override
  public String toString() {
    return myName;
  }

  private void dumpCycle() {
    try {
      long sleep = Env.getInteger("profile.sql.dump.period", 10000);
      while (!Thread.currentThread().isInterrupted()) {
        Thread.sleep(sleep);
        try {
          dump("main", myMainDB);
          dump("view", myShowDB);
          dump("monitor", myMonitorDB);
        } catch (RuntimeException e) {
          // ignore
        }
      }
    } catch (InterruptedException e) {
      // exit
    }
  }

  private void dump(String prefix, DatabaseQueue db) {
    if (db == null)
      return;
    SQLiteProfiler profiler = db.getProfiler();
    if (profiler == null)
      return;
    File profileBase = myDatabaseContext.getConfiguration().getProfileBaseFile();
    File f = new File(profileBase.getParentFile(), prefix + "-" + profileBase.getName());
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(f);
      PrintWriter out = new PrintWriter(new OutputStreamWriter(fos));
      out.println("*** dump for " + prefix + " session of [" + Schema.formatDin(myDin) + "] - " + new Date() + " ***");
      out.println();
      profiler.printReport(out);
      out.close();
    } catch (IOException e) {
      // ignore
    } finally {
      IOUtils.closeStreamIgnoreExceptions(fos);
    }
  }

  public static DatabaseManager open(@Nullable File filename, @Nullable File tempDir, DatabaseContext databaseContext)
    throws InterruptedException, SQLiteException
  {
    DatabaseQueue mainDB = null;
    DatabaseQueue showDB = null;
    DatabaseQueue monitorDB = null;
    boolean memoryDatabase = filename == null;
    int din;
    try {
      mainDB = DatabaseQueue.start(filename, "db:main", databaseContext);
      din = initDB(mainDB, tempDir);
      if (!memoryDatabase) {
        showDB = DatabaseQueue.start(filename, "db:view", databaseContext);
        monitorDB = createMonitorDB(filename, databaseContext);
      }
    } catch (SQLiteException e) {
      if (monitorDB != null)
        monitorDB.stop(false);
      if (showDB != null)
        showDB.stop(false);
      if (mainDB != null)
        mainDB.stop(false);
      throw e;
    }
    String name = filename == null ? "mem" : filename.getName();
    DatabaseManager connection = new DatabaseManager(name, mainDB, showDB, monitorDB, din, databaseContext);
    connection.start();
    return connection;
  }

  private static DatabaseQueue createMonitorDB(File filename, DatabaseContext databaseContext) throws SQLiteException {
    DatabaseQueue db = new DatabaseQueue(filename, "db:monitor", databaseContext);
    db.setParameter(DatabaseQueue.CACHE_SIZE, 4);
    db.start();
    return db;
  }

  private static int initDB(DatabaseQueue db, final File tempDir) throws InterruptedException, SQLiteException {
    final int[] din = new int[1];
    DatabaseJob job = new DatabaseJob() {
      protected void dbrun(TransactionContext context) throws Exception {
        if (tempDir != null) setTempDirectory(context.getConnection(), tempDir);
        din[0] = Schema.validate(context);
      }

      public TransactionType getTransactionType() {
        return TransactionType.WRITE;
      }
    };
    db.execute(job);
    db.flush();
    if (!job.isSuccessful()) {
      throw new SQLiteException(SQLiteConstants.WRAPPER_USER_ERROR, "cannot initialize database", job.getError());
    }
    return din[0];
  }

  private static void setTempDirectory(SQLiteConnection db, File tempDir) {
    try {
      if (tempDir.isDirectory()) {
        String path = tempDir.getCanonicalPath();
        // todo PLO-805 
        if (!ALLOWED_TEMP_DIR_NAME.matcher(path).matches()) {
          Log.warn("DatabaseManager avoiding setting temp path [" + path + "]");
          return;
        }
        Log.debug("setting temp directory '" + path + '\'');
        db.exec("PRAGMA temp_store_directory = '" + path + "'");
      }
    } catch (IOException e) {
      Log.error(e);
    } catch (SQLiteException e) {
      Log.warn("error setting temp path", e);
    }
  }


  public void stop() throws InterruptedException {
    if (myProfilerThread != null) {
      myProfilerThread.interrupt();
    }
    if (myMonitorDB != null) {
      assert myMonitor != null;
      myMonitor.stop();
      myMonitorDB.stop(false);
      myMonitorDB.waitFor();
    }
    myMainDB.stop(true);
    myMainDB.waitFor();
    if (myShowDB != null) {
      myShowDB.stop(true);
      myShowDB.waitFor();
    }
    synchronized (this) {
      myStopped = true;
    }
  }

  public DatabaseJob write(DBPriority priority, final DBTransaction transaction) {
    DBTransactionJob job = createTransactionJob(transaction);
    job.setPriority(priority.toBackgroundPriority());
    myMainDB.execute(job);
    return job;
  }

  public void waitAllProcessed() throws InterruptedException {
    myMainDB.flush();
    if (myShowDB != null)
      myShowDB.flush();
    if (myMonitorDB != null)
      myMonitorDB.flush();
  }

  private DBTransactionJob createTransactionJob(DBTransaction transaction) {
    TransactionObserver observer = myMonitor != null ? myMonitor : myQueryProcessor;
    return new DBTransactionJob(transaction, observer);
  }

  public DatabaseJob read(DBPriority priority, final DBRead read) {
    DatabaseQueue queue = priority.isForeground() ? myShowDB : myMainDB;
    if (queue == null)
      queue = myMainDB;
    DBReadJob job = new DBReadJob(read);
    job.setPriority(priority.getRelativePriority());
    queue.execute(job);
    return job;
  }

  public QueryProcessorImpl getQueryProcessor() {
    return myQueryProcessor;
  }

  public OldFilterManager getFilterManager() {
    return myFilterManager;
  }

  public Modifiable getTransactionModifiable() {
    return myQueryProcessor.getTransactionStartEvent();
  }

  public int getDin() {
    return myDin;
  }

  public CompoundValueCache createCache(Procedure<LongList> callback) {
    return myCacheManager.createCache(callback);
  }

  public Modifiable getMainQueueEmptySignal() {
    return myMainDB.getQueueEmptySignal();
  }

  public boolean isDbThread() {
    return isDbThread(myMainDB) || isDbThread(myMonitorDB) || isDbThread(myShowDB);
  }

  private static boolean isDbThread(@Nullable DatabaseQueue queue) {
    return queue == null ? false : queue.isQueueThread();
  }
}
