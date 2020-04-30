package com.almworks.items.api;

import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.impl.SQLiteDatabase;
import com.almworks.util.bool.BoolExpr;

/**
 * All tests should finish in several seconds
 */
public class PerformanceTests extends DatabaseFixture {
  private static final DBItemType TYPE = new DBItemType("testType", "test type");

  public PerformanceTests() {
    super(40000);
  }

  public void testCreation() throws Exception {
    SQLiteDatabase db = createFileDatabase();
    db.start();
    for (int i = 0; i < 1; i++) {
      db.writeForeground(new WriteTransaction<Object>() {
        @Override
        public Object transaction(DBWriter writer) throws DBOperationCancelledException {
          for (int i = 0; i < 5000; i++) {
            long item = writer.nextItem();
            writer.setValue(item, TestData.STRING, "string");
            writer.setValue(item, DBAttribute.TYPE, writer.materialize(TYPE));
          }
          return null;
        }
      }).waitForCompletion();
    }
  }


  private final static BoolExpr<DP> ITEMS = DPEqualsIdentified.create(DBAttribute.TYPE, TYPE);

  public void testMapByKey() throws Exception {
    SQLiteDatabase db = createFileDatabase();
    db.start();
    final int COUNT = 4000;
    int created = getOrCreateKeyed(db, COUNT);
    assertEquals(COUNT, created);
    created = getOrCreateKeyed(db, COUNT + 100);
    assertEquals(100, created);
  }

  private int getOrCreateKeyed(SQLiteDatabase db, final int COUNT) {
    return db.writeForeground(new WriteTransaction<Integer>() {
      @Override
      public Integer transaction(DBWriter writer) throws DBOperationCancelledException {
        int created = 0;
        DBQuery q = writer.query(ITEMS);
        for (int i = 0; i < COUNT; i++) {
          String key = "I" + i;
          long item = q.getItemByKey(TestData.STRING, key);
          if (item == 0) {
            created++;
            item = writer.nextItem();
            writer.setValue(item, TestData.STRING, key);
            writer.setValue(item, DBAttribute.TYPE, writer.materialize(TYPE));
          }
        }
        return created;
      }
    }).waitForCompletion();
  }
}
