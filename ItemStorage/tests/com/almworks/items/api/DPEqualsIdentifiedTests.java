package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.util.bool.BoolExpr;

import static com.almworks.items.api.TestData.ITEM1;
import static com.almworks.items.api.TestData.LINK;

public class DPEqualsIdentifiedTests extends MemoryDatabaseFixture {
  private static final DBIdentifiedObject XXX = new DBIdentifiedObject("XXX");
  private static final BoolExpr<DP> EXPR = DPEqualsIdentified.create(LINK, XXX);

  public void testEqualsIdentified() {
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        assertEquals(LongArray.create(), reader.query(EXPR).copyItemsSorted());
        return true;
      }
    }).waitForCompletion();
    final long[] m = {0};
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        assertEquals(LongArray.create(), writer.query(EXPR).copyItemsSorted());
        m[0] = writer.materialize(XXX);
        writer.setValue(ITEM1, LINK, m[0]);
        return null;
      }
    }).waitForCompletion();
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        assertEquals(LongArray.create(ITEM1), writer.query(EXPR).copyItemsSorted());
        writer.setValue(ITEM1, LINK, m[0] + 1);
        assertEquals(LongArray.create(), writer.query(EXPR).copyItemsSorted());
        writer.setValue(ITEM1, LINK, null);
        assertEquals(LongArray.create(), writer.query(EXPR).copyItemsSorted());
        writer.setValue(ITEM1, LINK, m[0]);
        assertEquals(LongArray.create(ITEM1), writer.query(EXPR).copyItemsSorted());
        return null;
      }
    }).waitForCompletion();
  }

  public void testIdentifiedChanges() {
    final long[] m = {0};
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        m[0] = writer.materialize(XXX);
        writer.setValue(ITEM1, LINK, m[0]);
        assertEquals(LongArray.create(ITEM1), writer.query(EXPR).copyItemsSorted());
        return null;
      }
    }).waitForCompletion();
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        writer.clearItem(m[0]);
        assertEquals(0, writer.findMaterialized(XXX));
        assertEquals(LongArray.create(), writer.query(EXPR).copyItemsSorted());
        m[0] = writer.materialize(XXX);
        assertEquals(LongArray.create(), writer.query(EXPR).copyItemsSorted());
        writer.setValue(ITEM1, LINK, m[0]);
        assertEquals(LongArray.create(ITEM1), writer.query(EXPR).copyItemsSorted());
        return null;
      }
    }).waitForCompletion();
  }

  public void testEquals() {
    BoolExpr<DP> copy = DPEqualsIdentified.create(copyAttribute(LINK), new DBIdentifiedObject("XXX"));
    assertEquals(EXPR, copy);
    assertEquals(EXPR.hashCode(), copy.hashCode());
  }
}
