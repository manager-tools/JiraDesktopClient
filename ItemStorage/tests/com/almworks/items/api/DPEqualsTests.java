package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Lifespan;

import java.lang.reflect.Method;

import static com.almworks.items.api.TestData.*;

public class DPEqualsTests extends MemoryDatabaseFixture {
  public void test() {
    assertEquals(VALUESET1.keySet(), VALUESET2.keySet());
    TestData.materializeTestItems(db);
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        writeMap(ITEM1, VALUESET1, writer);
        writeMap(ITEM2, VALUESET2, writer);

        checkEquals(writer);
        return null;
      }
    }).waitForCompletion();
    db.readBackground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        checkEquals(reader);
        return null;
      }
    }).waitForCompletion();
  }

  private void checkEquals(DBReader reader) {
    checkQuery(reader, VALUESET1, ITEM1, ITEM2);
    checkQuery(reader, VALUESET2, ITEM2, ITEM1);
  }

  private void checkQuery(DBReader reader, AttributeMap values, long matching, long notMatching) {
    for (DBAttribute attribute : values.keySet()) {
      Object value = values.get(attribute);
      BoolExpr<DP> expr = DPEquals.create(attribute, value);

      DBQuery q = reader.query(expr);
      assertEquals(attribute + " " + value, 1, q.count());

      assertTrue(attribute + " " + value, q.contains(matching));
      assertFalse(attribute + " " + value, q.contains(notMatching));

      LongArray ar = q.copyItemsSorted();
      assertEquals(attribute + " " + value, 1, ar.size());
      assertEquals(attribute + " " + value, matching, ar.get(0));

      LongArray filtered = new LongArray();
      q.filterItems(LongArray.create(matching, notMatching, ITEM3), filtered);
      assertEquals(attribute + " " + value, ar, filtered);

      assertEquals(attribute + " " + value, matching, q.getItem());
    }
  }

  public void testEqualsAndHash() {
    for (DBAttribute attribute : VALUESET1.keySet()) {
      Object value = VALUESET1.get(attribute);
      BoolExpr<DP> expr1 = DPEquals.create(attribute, value);
      BoolExpr<DP> expr2 = DPEquals.create(copyAttribute(attribute), clone(value));
      assertEquals(attribute.toString(), expr1, expr2);
      assertEquals(attribute.toString(), expr1.hashCode(), expr2.hashCode());
    }
  }

  private Object clone(Object value) {
    if (value == null) return null;
    try {
      if (value instanceof Cloneable) {
        Method method = Object.class.getDeclaredMethod("clone");
        method.setAccessible(true);
        return method.invoke(value);
      }
    } catch (Exception e) {
      throw new Error(e);
    }
    return value;
  }

  public void testLiveQueryResult() {
    TestData.materializeTestItems(db);
    final long[] icn = {0};
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        writeMap(ITEM1, VALUESET1, writer);
        writeMap(ITEM2, VALUESET2, writer);
        icn[0] = writer.getTransactionIcn();
        return null;
      }
    }).waitForCompletion();
    for (final DBAttribute attribute : VALUESET1.keySet()) {
      Object value = VALUESET1.get(attribute);
      BoolExpr<DP> expr = DPEquals.create(attribute, value);
      DBLiveQuery lq = db.liveQuery(Lifespan.FOREVER, expr, DBLiveQuery.Listener.DEAF);
      checkLiveQuery(icn, lq, ITEM1);

      update(icn, ITEM1, attribute, VALUESET2.get(attribute));
      checkLiveQuery(icn, lq);

      update(icn, ITEM1, attribute, null);
      checkLiveQuery(icn, lq);

      update(icn, ITEM1, attribute, VALUESET1.get(attribute));
      checkLiveQuery(icn, lq, ITEM1);

      update(icn, ITEM2, attribute, VALUESET1.get(attribute));
      checkLiveQuery(icn, lq, ITEM1, ITEM2);
    }
  }

  private void update(final long[] icnHolder, final long item, final DBAttribute attribute, final Object value) {
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        writer.setValue(item, attribute, value);
        icnHolder[0] = writer.getTransactionIcn();
        return null;
      }
    }).waitForCompletion();
  }

  private void checkLiveQuery(long[] icnHolder, DBLiveQuery lq, long ... expected) {
    waitIcn(lq, icnHolder[0]);
    assertEquals(lq.toString(), LongArray.create(expected), lq.copyItems());
  }

  private void waitIcn(DBLiveQuery query, long icn) {
    long start = System.currentTimeMillis();
    while (!query.isTransactionResultVisible(icn)) {
      if (System.currentTimeMillis() - start >= 1000) throw new AssertionError(query + " " + icn);
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }
  }
}
