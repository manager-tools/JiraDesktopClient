package com.almworks.items.dp;

import com.almworks.items.api.*;
import com.almworks.items.util.SlaveUtils;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.tests.CollectionsCompare;

import static org.almworks.util.Collections15.arrayList;

public class DPReferredByTests extends MemoryDatabaseFixture {
  public static final DBItemType SLAVES_A = new DBItemType("slavesA");
  public static final DBItemType SLAVES_B = new DBItemType("slavesB");
  public static final DBItemType MASTERS = new DBItemType("masters");
  public static final DBAttribute<Long> MASTER_ATTR = SlaveUtils.masterReference("master", "master");
  private static final BoolExpr<DP> MASTER_EXPR = DPEqualsIdentified.create(DBAttribute.TYPE, MASTERS);

  public void setUp() throws Exception {
    super.setUp();
    setWriteToStdout(true);
  }

  public void testNegation() {
    final long[] masters = new long[2];
    final long[] slavesA = new long[2];
    final long[] slavesB = new long[2];
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        masters[0] = writer.nextItem();
        masters[1] = writer.nextItem();
        long mastersType = writer.materialize(MASTERS);
        writer.setValue(masters[0], DBAttribute.TYPE, mastersType);
        writer.setValue(masters[1], DBAttribute.TYPE, mastersType);

        slavesA[0] = writer.nextItem();
        writer.setValue(slavesA[0], MASTER_ATTR, masters[0]);
        writer.setValue(slavesA[0], DBAttribute.TYPE, writer.materialize(SLAVES_A));
        slavesA[1] = writer.nextItem();
        writer.setValue(slavesA[1], DBAttribute.TYPE, writer.materialize(SLAVES_A));

        slavesB[0] = writer.nextItem();
        writer.setValue(slavesB[0], MASTER_ATTR, masters[0]);
        writer.setValue(slavesB[0], DBAttribute.TYPE, writer.materialize(SLAVES_B));
        slavesB[1] = writer.nextItem();
        writer.setValue(slavesB[1], MASTER_ATTR, masters[1]);
        writer.setValue(slavesB[1], DBAttribute.TYPE, writer.materialize(SLAVES_B));
      }
    });
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        CollectionsCompare compare = new CollectionsCompare();

        BoolExpr<DP> eA = DPReferredBy.create(MASTER_ATTR, DPEqualsIdentified.create(DBAttribute.TYPE, SLAVES_A));
        compare.order(reader.query(eA).copyItemsSorted(), masters[0]);
        compare.order(reader.query(eA.negate().and(MASTER_EXPR)).copyItemsSorted(), masters[1]);

        BoolExpr<DP> eB = DPReferredBy.create(MASTER_ATTR, DPEqualsIdentified.create(DBAttribute.TYPE, SLAVES_B));
        compare.order(reader.query(eB).copyItemsSorted(), masters[0], masters[1]);
        compare.order(reader.query(eB.negate().and(MASTER_EXPR)).copyItemsSorted());
        return null;
      }
    }).waitForCompletion();
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        writer.setValue(slavesB[1], MASTER_ATTR, null);
      }
    });
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        CollectionsCompare compare = new CollectionsCompare();
        // The point of this test is to check negation of DPRefBy in case when referee query isn't convertable to SQL
        BoolExpr<DP> e = DPReferredBy.create(MASTER_ATTR, DPEquals.equalOneOf(DBAttribute.TYPE, arrayList(reader.findMaterialized(SLAVES_A), reader.findMaterialized(SLAVES_B))));
        compare.order(reader.query(e).copyItemsSorted(), masters[0]);
        compare.order(reader.query(e.negate().and(MASTER_EXPR)).copyItemsSorted(), masters[1]);
        return null;
      }
    }).waitForCompletion();
  }
}
