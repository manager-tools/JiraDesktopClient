package com.almworks.items.api;

import com.almworks.items.impl.DBReaderImpl;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

public class HousekeepingTests extends MemoryDatabaseFixture {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    enableSqlite4JavaLogging(false);
  }

  public void testHousekeepingStartsOnFlag() throws InterruptedException {
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        writer.setValue(100, DBAttribute.String("a1", "a1"), "a1");
        writer.setValue(100, DBAttribute.String("a2", "a2"), "a2");
        return null;
      }
    }).waitForCompletion();
    assertFalse(detectAnalyzeResult());
    db.setLongHousekeepingAllowed(true);
    Thread.sleep(100);
    for (int i = 0; i < 10; i++) {
      if (detectAnalyzeResult())
        break;
      Thread.sleep(100);
    }
    assertTrue(detectAnalyzeResult());
  }

  private boolean detectAnalyzeResult() {
    return db.readForeground(new ReadTransaction<Boolean>() {
      @Override
      public Boolean transaction(DBReader reader) throws DBOperationCancelledException {
        SQLiteConnection c = ((DBReaderImpl) reader).getContext().getConnection();
        return tableExists(c, "sqlite_stat") || tableExists(c, "sqlite_stat1") || tableExists(c, "sqlite_stat2");
      }
    }).waitForCompletion();
  }

  private boolean tableExists(SQLiteConnection c, String name) {
    try {
      SQLiteStatement st = c.prepare("select * from " + name);
      st.dispose();
      return true;
    } catch (SQLiteException e) {
      return false;
    }
  }
}
