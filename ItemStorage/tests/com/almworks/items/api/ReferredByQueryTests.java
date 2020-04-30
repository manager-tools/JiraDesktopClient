package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.dp.DPReferredBy;
import com.almworks.items.util.SlaveUtils;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;

import java.util.List;

public class ReferredByQueryTests extends MemoryDatabaseFixture {
  private static final DBAttribute<Long> ID = DBAttribute.Long("id", "ID");
  private static final DBAttribute<String> SUMMARY = DBAttribute.String("summary", "Summary");
  private static final DBAttribute<Long> PARENT = SlaveUtils.masterReference("parent", "Parent");

  public void testSimple() {
    setupData(25, 10);
    checkResult(DPReferredBy.create(PARENT, BoolExpr.<DP>TRUE()), 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
    checkResult(DPReferredBy.create(PARENT, DPEquals.create(ID, 1L)));
    checkResult(DPEquals.create(SUMMARY, "B").and(DPReferredBy.create(PARENT, DPEquals.create(SUMMARY, "C"))));
  }

  public void testNested() {
    setupData(25, 10);
    checkResult(DPReferredBy.create(PARENT, DPReferredBy.create(PARENT, BoolExpr.<DP>TRUE())), 1, 2, 3, 4, 5);
  }

  public void testNegation() {
    setupData(25, 10);
    checkResult(DPReferredBy.create(PARENT, DPNotNull.create(SUMMARY).negate()));
  }

  private void checkResult(BoolExpr<DP> expr, long... ids) {
    List<Long> actual = getIds(expr);
    List<Long> expected = Collections15.arrayList();
    for (long id : ids) {
      expected.add(id);
    }
    new CollectionsCompare().unordered(expected, actual);
  }

  private List<Long> getIds(final BoolExpr<DP> query) {
    List<Long> r = db.readForeground(new ReadTransaction<List<Long>>() {
      @Override
      public List<Long> transaction(DBReader reader) {
        DBQuery dbq = reader.query(query);
        LongArray artifacts = dbq.copyItemsSorted();
        List<Long> r = Collections15.arrayList();
        for (int i = 0; i < artifacts.size(); i++) {
          r.add(ID.getValue(artifacts.get(i), reader));
        }
        return r;
      }
    }).waitForCompletion();
    return r;
  }

  private void setupData(final int total, final int cycle) {
    db.writeForeground(new WriteTransaction<Void>() {
      public Void transaction(DBWriter writer) {
        for (int i = 1; i <= total; i++) {
          long a = writer.nextItem();
          writer.setValue(a, ID, (long) i);
          writer.setValue(a, SUMMARY, "" + (char) (((int) 'A') + (i % cycle) - 1));
          if (i > cycle) {
            writer.setValue(a, PARENT, writer.query(DPEquals.create(ID, (long) (i - cycle))).getItem());
          }
        }
        return null;
      }
    }).waitForCompletion();
  }
}
