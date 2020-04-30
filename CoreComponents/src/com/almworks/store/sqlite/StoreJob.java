package com.almworks.store.sqlite;

import com.almworks.api.store.StoreFeature;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: dyoma
 * Date: Apr 29, 2010
 * Time: 9:04:26 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class StoreJob {
  private static final String TABLE = "storeTable";
  private static final String ID_COLUMN = "id";
  private static final String DATA_COLUMN = "data";
  private static final String FEAUTE_COLUMN = "feature";
  private static final SQLParts SQL_CLEAR = new SQLParts("DELETE FROM " + TABLE + " WHERE " + ID_COLUMN + "=?");
  private static final SQLParts SQL_WRITE
    = new SQLParts("INSERT OR REPLACE INTO " + TABLE + "(" + ID_COLUMN + "," + DATA_COLUMN + "," + FEAUTE_COLUMN + ") VALUES (?,?,?)");

  private final AtomicInteger myAttempts;
  private boolean myComplete = false;
  private Exception myFailure = null;

  StoreJob(int attemptCount) {
    myAttempts = new AtomicInteger(attemptCount);
  }

  public void cancelled() {
    fail(new Exception("Cancelled"));
  }

  public boolean sqliteFailed(SQLiteException e) {
    return myAttempts.decrementAndGet() > 0;
  }

  public void fail(Exception e) {
    synchronized (this) {
      if (myComplete) {
        if (myFailure == null) Log.error("Job " + this + " cannot be failed due to it is already complete");
        return;
      }
      myFailure = e;
      myComplete = true;
      notifyAll();
    }
  }

  public boolean waitDone() throws InterruptedException {
    synchronized (this) {
      while (!myComplete) wait(500);
      return myComplete && myFailure == null;
    }
  }

  public void perform(SQLiteConnection connection) throws SQLiteException {
    doPerform(connection);
    synchronized (this) {
      myComplete = true;
      notifyAll();
    }
  }

  protected abstract void doPerform(SQLiteConnection connection) throws SQLiteException;


  public Exception getFailedReason() {
    synchronized (this) {
      return myFailure;
    }
  }

  public boolean isSuccessful() {
    return myComplete && myFailure == null;
  }

  static void clearRow(SQLiteConnection connection, String id) throws SQLiteException {
    SQLiteStatement st = connection.prepare(SQL_CLEAR);
    try {
      st.bind(1, id);
      st.step();
    } finally {
      st.dispose();
    }
  }

  static void writeRow(SQLiteConnection connection, String id, byte[] data, StoreFeature[] features)
    throws SQLiteException
  {
    StoreFeature feature = features != null && features.length > 0 ? features[0] : null;
    String featureId = feature != null ? feature.getName() : null;

    SQLiteStatement st = connection.prepare(SQL_WRITE, true);
    try {
      st.bind(1, id);
      st.bind(2, data, 0, data.length);
      if (featureId != null) st.bind(3, featureId);
      else st.bindNull(3);
      st.step();
    } finally {
      st.dispose();
    }
  }

  static class Write extends StoreJob {
    private final byte[] myData;
    private final String myId;
    private final StoreFeature[] myFeatures;

    Write(String id, byte[] data, StoreFeature[] features) {
      super(1);
      myData = data;
      myId = id;
      myFeatures = features;
    }

    @Override
    protected void doPerform(SQLiteConnection connection) throws SQLiteException {
      byte[] data = myData;
      if (data != null) writeRow(connection, myId, data, myFeatures);
      else clearRow(connection, myId);
    }

    @Override
    public String toString() {
      return "WriteStoreBYtesJob: " + myId;
    }
  }

  static class Read extends StoreJob {
    private static final SQLParts SQL_READ = new SQLParts("SELECT " + DATA_COLUMN + ", " + FEAUTE_COLUMN + " FROM " + TABLE + " WHERE " + ID_COLUMN + "=?");

    private final String myId;
    private byte[] myData = null;
    private String myFeatureId = null;

    Read(String id, int attemptCount) {
      super(attemptCount);
      myId = id;
    }

    @Nullable
    public byte[] getData() {
      synchronized (this) {
        if (!isSuccessful()) return null;
        return myData;
      }
    }

    public StoreFeature[] getFeatures() {
      String featureId;
      synchronized (this) {
        featureId = myFeatureId;
      }
      if (featureId == null) return StoreFeature.EMPTY_FEATURES_ARRAY;
      StoreFeature feature = StoreFeature.findByName(featureId);
      if (feature == null) {
        Log.error("Unknown feaure " + featureId);
        return StoreFeature.EMPTY_FEATURES_ARRAY;
      }
      return new StoreFeature[]{feature};
    }

    @Override
    protected void doPerform(SQLiteConnection connection) throws SQLiteException {
      SQLiteStatement st = connection.prepare(SQL_READ);
      try {
        st.bind(1, myId);
        boolean found = st.step();
        if (!found) return;
        byte[] bytes = st.columnBlob(0);
        String feature = st.columnString(1);
        synchronized (this) {
          myData = bytes;
          myFeatureId = feature;
        }
      } finally {
        st.dispose();
      }
    }

    @Override
    public String toString() {
      return "StoreJob-read: " + myId;
    }
  }

  static final StoreJob INIT_SCHEMA = new StoreJob(1) {
    private static final String CREATE_STORE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE +
      " (" + ID_COLUMN + " TEXT UNIQUE NOT NULL, " + DATA_COLUMN + " BLOB)";
    private static final String ALTER_STORE_TABLE = "ALTER TABLE " + TABLE + " ADD COLUMN " + FEAUTE_COLUMN + " TEXT";

    @Override
    protected void doPerform(SQLiteConnection connection) throws SQLiteException {
      connection.exec(CREATE_STORE_TABLE);
      try {
        connection.exec(ALTER_STORE_TABLE);
      } catch (SQLiteException e) {
        if (!e.getMessage().contains("duplicate column") || !e.getMessage().contains(FEAUTE_COLUMN)) Log.error(e);
      }
    }
  };
}