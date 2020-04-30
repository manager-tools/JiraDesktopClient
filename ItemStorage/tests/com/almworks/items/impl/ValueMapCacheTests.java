package com.almworks.items.impl;

import com.almworks.items.api.*;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.dp.DPNotNull;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.tests.CollectionsCompare;

public class ValueMapCacheTests extends MemoryDatabaseFixture {
  private static final DBItemType TYPE = new DBItemType(":t:type");
  private static final DBAttribute<String> KEY = DBAttribute.String(":a:key", "key");
  private static final DBAttribute<Boolean> REMOVED = DBAttribute.Bool("a:removed", "removed");
  private static final BoolExpr<DP> EXPR = DPEqualsIdentified.create(DBAttribute.TYPE, TYPE).and(DPNotNull.create(REMOVED).negate());

  private final CollectionsCompare compare = new CollectionsCompare();

  public void test() {
    final long[] items = new long[6];
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        check(writer, 0, 0, 0, 0, 0, 0);
        for (int i = 0; i < 4; ++i) {
          writeItem(writer, items, i);
        }
        check(writer, items, 0, 4);
      }
    });
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        check(writer, items, 0, 4);

        writeItem(writer, items, 4);
        check(writer, items, 0, 5);

        writer.setValue(items[0], REMOVED, true);
        check(writer, items, 1, 5);
      }
    });
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        check(writer, items, 1, 5);

        writer.setValue(items[3], REMOVED, true);
        writeItem(writer, items, 5);
        check(writer, 0, items[1], items[2], 0, items[4], items[5]);

        writer.setValue(items[0], REMOVED, null);
        writer.setValue(items[1], REMOVED, true);
        writer.setValue(items[5], REMOVED, true);
        check(writer, items[0], 0, items[2], 0, items[4], 0);
      }
    });
  }

  private void check(DBWriter writer, long... expectedItems) {
    check(writer, expectedItems, 0, expectedItems.length);
  }

  private void check(DBWriter writer, long[] allItems, int from, int to) {
    long[] actualItems = new long[allItems.length];
    DBQuery query = writer.query(EXPR);
    for (int i = 0; i < allItems.length; ++i) {
      String key = "I" + i;
      actualItems[i] = query.getItemByKey(KEY, key);
    }
    long[] expectedItems = new long[allItems.length];
    System.arraycopy(allItems, from, expectedItems, from, to - from);
    compare.order(actualItems, expectedItems);
  }

  private long writeItem(DBWriter writer, long[] items, int i) {
    long item = writer.nextItem();
    writeItem(writer, i, item);
    items[i] = item;
    return item;
  }

  private void writeItem(DBWriter writer, int i, long item) {
    writer.setValue(item, DBAttribute.TYPE, writer.materialize(TYPE));
    writer.setValue(item, KEY, "I" + i);
    writer.setValue(item, REMOVED, null);
  }

  public void testMany() {
    final long[] allItems = new long[100];
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        for (int i = 0; i < allItems.length; ++i) {
          allItems[i] = writer.nextItem();
        }
      }
    });
    final long[] curItems = new long[allItems.length];
    for (int i = 0; i < 20; ++i) {
      writeNoFail(new Procedure<DBWriter>() {
        @Override
        public void invoke(DBWriter writer) {
          addMany(allItems, curItems, writer);
          removeMany(allItems, curItems, writer);
          check(writer, curItems);
        }
      });
    }
    flushWriteQueue();
  }

  private void addMany(long[] all, long[] cur, DBWriter writer) {
    for (int i = 0; i < 50; ++i) {
      int index = getRandom().nextInt(all.length);
      cur[index] = all[index];
      writeItem(writer, index, all[index]);
    }
  }

  private void removeMany(long[] all, long[] cur, DBWriter writer) {
    for (int i = 0; i < 50; ++i) {
      int idx = getRandom().nextInt(cur.length);
      cur[idx] = 0;
      writer.setValue(all[idx], REMOVED, true);
    }
  }
}
