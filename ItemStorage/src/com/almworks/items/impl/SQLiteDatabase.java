package com.almworks.items.impl;

import com.almworks.items.api.*;
import com.almworks.items.impl.sqlite.DatabaseContext;
import com.almworks.items.impl.sqlite.DatabaseManager;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Threads;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.concurrent.SynchronizedInt;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class SQLiteDatabase extends BaseDatabase {
  private static final int STATE_NOT_STARTED = 0;
  private static final int STATE_STARTING = 1;
  private static final int STATE_STARTED = 2;
  private static final int STATE_STOPPING = 3;
  private static final int STATE_STOPPED = 4;

  @Nullable
  private final File myDatabaseFile;
  @Nullable
  private final File mySQLiteTempDir;

  @NotNull
  private final DatabaseContext myDatabaseContext;

  private final SynchronizedInt myState = new SynchronizedInt(STATE_NOT_STARTED);
  private final InconsistentReader myInconsistentReader = new InconsistentReader(this);
  private final DatabaseEventSource myEventSource = new DatabaseEventSource();
  private final LiveQueryManager myLiveQueryManager = new LiveQueryManager();
  private final DatabaseHouseKeeper myHousekeeper = new DatabaseHouseKeeper();

  private DatabaseManager myConnection = null;

  public SQLiteDatabase(@Nullable File databaseFile, @Nullable File tempDir) {
    this(databaseFile, tempDir, null);
  }

  public SQLiteDatabase(@Nullable File databaseFile, @Nullable File tempDir, @Nullable DBConfiguration configuration) {
    myDatabaseFile = databaseFile;
    mySQLiteTempDir = tempDir;
    if (configuration == null) {
      configuration = DBConfiguration.createDefault(databaseFile);
    }
    myDatabaseContext = new DatabaseContext(configuration);
  }

  @Override
  public String toString() {
    return "DB[" + (myDatabaseFile == null ? "" : myDatabaseFile.getAbsolutePath()) + ']';
  }

  public void start() {
    Threads.assertLongOperationsAllowed();
    if (!myState.commit(STATE_NOT_STARTED, STATE_STARTING)) {
      Log.warn(this + " has been started already");
      return;
    }
    boolean success = false;
    DatabaseManager c = null;
    try {
      c = startConnection();
      if (c == null) {
        Log.error(this + " did not start a db connection");
        throw new DatabaseLifecycleException(this + " did not start a db connection");
      }
      DBMigrator.runMigrations(c, myDatabaseContext);
      synchronized (myState) {
        myConnection = c;
      }

      myEventSource.start(c);
      myLiveQueryManager.start(c);
      myHousekeeper.start(c);
      materializeSystemAttributes(c);

      myState.commit(STATE_STARTING, STATE_STARTED);
      Log.debug(this + " started");
      success = true;
    } finally {
      if (!success) {
        myState.set(STATE_STOPPED);
        if (c != null) {
          try {
            c.stop();
          } catch (Throwable e) {
            Log.warn(e);
          }
        }
        synchronized (myState) {
          myConnection = null;
        }
      }
    }
  }

  private static void materializeSystemAttributes(DatabaseManager c) {
    final DBIdentifiedObject[] initialized = {
      DBAttribute.ID, DBAttribute.NAME, DBAttribute.TYPE, DBItemType.TYPE, DBItemType.ATTRIBUTE};

    WriteHandle<Object> handle = new WriteHandle<Object>(new WriteTransaction<Object>() {
      public Object transaction(DBWriter writer) {
        ((DBWriterImpl) writer).getContext().disablePropagation();
        for (DBIdentifiedObject object : initialized) {
          writer.materialize(object);
        }
        return null;
      }
    });
    c.write(DBPriority.BACKGROUND, handle);
    handle.waitForCompletion();
    if (!handle.isSuccessful()) {
      throw new DBException("error materializing system attributes", handle.getError());
    }
  }

  private DatabaseManager startConnection() {
    boolean success = false;
    DatabaseManager connection = null;
    try {
      connection = DatabaseManager.open(myDatabaseFile, mySQLiteTempDir, myDatabaseContext);
      success = true;
      return connection;
    } catch (InterruptedException e) {
      Log.warn(this + " start interrupted");
      throw new RuntimeInterruptedException(e);
    } catch (SQLiteException e) {
      Log.warn(this + " start problem", e);
      throw new DBException(e);
    } finally {
      if (!success) {
        try {
          if (connection != null)
            connection.stop();
        } catch (Throwable e) {
          Log.error(e);
        }
        myState.set(STATE_STOPPED);
      }
    }
  }

  public void stop() {
    Threads.assertLongOperationsAllowed();
    DatabaseManager c;
    synchronized (myState) {
      if (!myState.commit(STATE_STARTED, STATE_STOPPING)) {
        Log.debug(this + " is not started");
        return;
      }
      c = myConnection;
      myConnection = null;
    }
    try {
      c.stop();
    } catch (InterruptedException e) {
      Log.warn(this + " stopping interrupted");
      throw new RuntimeInterruptedException(e);
    } finally {
      myState.commit(STATE_STOPPING, STATE_STOPPED);
    }
    Log.debug(this + " has stopped");
  }

  @Override
  public <T> DBResult<T> read(DBPriority priority, ReadTransaction<T> transaction) {
    try {
      DatabaseManager c = getConnection();
      ReadHandle<T> r = new ReadHandle<T>(transaction);
      c.read(priority, r);
      return r;
    } catch (DatabaseLifecycleException e) {
      int state = myState.get();
      if (state == STATE_STOPPING || state == STATE_STOPPED) {
        // graceful stopping
        return new DatabaseStoppingFailedResult<T>(e);
      }
      throw e;
    }
  }

  @Override
  public <T> DBResult<T> write(DBPriority priority, WriteTransaction<T> transaction) {
    try {
      DatabaseManager c = getConnection();
      WriteHandle<T> r = new WriteHandle<T>(transaction);
      c.write(priority, r);
      return r;
    } catch (DatabaseLifecycleException e) {
      int state = myState.get();
      if (state == STATE_STOPPING || state == STATE_STOPPED) {
        // graceful stopping
        return new DatabaseStoppingFailedResult<T>(e);
      }
      throw e;
    }
  }

  private DatabaseManager getConnection() {
    DatabaseManager c;
    synchronized (myState) {
      int state = myState.get();
      if (state != STATE_STARTED) {
        throw new DatabaseLifecycleException(this + " is not started");
      }
      c = myConnection;
    }
    return c;
  }

  public DBReader getInconsistentReader() {
    DBReader current = InconsistentReader.RUNNING_READER.get();
    return current == null ? myInconsistentReader : current;
  }


  @Override
  public void addListener(Lifespan lifespan, DBListener listener) {
    myEventSource.addListener(lifespan, listener);
  }

  @Override
  public DBLiveQuery liveQuery(Lifespan lifespan, BoolExpr<DP> expr, DBLiveQuery.Listener listener) {
    return myLiveQueryManager.attach(expr, lifespan, listener);
  }

  @Override
  public void dump(final PrintStream writer) throws IOException {
    read(DBPriority.FOREGROUND, new ReadTransaction<Object>() {
      public Object transaction(DBReader reader) {
        new HighLevelDumper(reader).dump(writer);
        return null;
      }
    }).waitForCompletion();
  }

  @Override
  public void setLongHousekeepingAllowed(boolean housekeepingAllowed) {
    myHousekeeper.setEnabled(housekeepingAllowed);
  }

  @Override
  public void registerTrigger(final DBTrigger trigger) {
    final DBTriggerCounterpart counterpart = new DBTriggerCounterpart(trigger);
    final WriteTransaction init = new WriteTransaction() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        counterpart.initialize(writer);
        return null;
      }
    };
    final Procedure register = new Procedure() { @Override public void invoke(Object _) {
      myDatabaseContext.getConfiguration().addTrigger(counterpart);
    }};
    Procedure retry = new Procedure() {
      volatile int nAttempts = 3;
      @Override public void invoke(Object _) {
        if (nAttempts-- == 0) {
          Log.error("Failed to add trigger " + trigger);
        } else {
          writeBackground(init).onSuccess(ThreadGate.STRAIGHT, register).onFailure(ThreadGate.STRAIGHT, this);
        }
      }
    };
    writeBackground(init).onSuccess(ThreadGate.STRAIGHT, register).onFailure(ThreadGate.STRAIGHT, retry);
  }

  @Override
  public UserDataHolder getUserData() {
    return myDatabaseContext.getUserData();
  }

  @Override
  public boolean isDbThread() {
    return myConnection == null ? false : myConnection.isDbThread();
  }

  private class DatabaseStoppingFailedResult<T> extends AbstractHandle<T> {
    public DatabaseStoppingFailedResult(Throwable error) {
      addError(error);
      finished(false);
    }
  }
}
