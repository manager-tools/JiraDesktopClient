package com.almworks.items.api;

import com.almworks.items.impl.SQLiteDatabase;

import java.util.concurrent.TimeUnit;

public class StartStopTests extends DatabaseFixture {
  public void setUp() throws Exception {
    super.setUp();
    enableSqlite4JavaLogging(false);
  }

  public void testMemStartStop() {
    SQLiteDatabase db = createSQLiteMemoryDatabase();
    db.stop();
  }

  public void testFileStartStop() {
    SQLiteDatabase db = createFileDatabase();
    db.stop();
  }

  public void testStopNotification() throws Exception {
    SQLiteDatabase db = createFileDatabase();
    DBResult<Object> r1 = db.readBackground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        return null;
      }
    });
    DBResult<Object> r2 = db.readBackground(new Empty());
    DBResult<Object> r3 = db.readBackground(new Empty());
    DBResult<Object> r4 = db.readBackground(new Empty());
    db.stop();
    r1.get(1200, TimeUnit.MILLISECONDS);
    r2.get(50, TimeUnit.MILLISECONDS);
    r3.get(50, TimeUnit.MILLISECONDS);
    r4.get(50, TimeUnit.MILLISECONDS);
  }

  private static class Empty implements ReadTransaction<Object> {
    @Override
    public Object transaction(DBReader reader) throws DBOperationCancelledException {
      return null;
    }
  }
}
