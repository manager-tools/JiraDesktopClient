package com.almworks.items.api;

import com.almworks.items.impl.SQLiteDatabase;
import com.almworks.util.TestLog;

public class ErrorTests extends DatabaseFixture {
//  public void setUp() {
//    enableSqlite4JavaLogging();
//  }

  public void testErrorCaught() throws Exception {
    SQLiteDatabase db = createSQLiteMemoryDatabase();
    DBResult<Object> result = db.readBackground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        throw new Error("woo");
      }
    });
    result.waitForCompletion();
    assertNull(result.get());
    assertEquals("woo", result.getErrors().get(0).getMessage());
    TestLog.getInstance().clearLog(); // otherwise test will fail because of exception
  }

  
}
