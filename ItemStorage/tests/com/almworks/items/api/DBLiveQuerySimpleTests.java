package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.items.dp.DPEquals;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;
import util.concurrent.SynchronizedBoolean;

public class DBLiveQuerySimpleTests extends MemoryDatabaseFixture {
  public static final DBAttribute<Boolean> RELEVANT = DBAttribute.Bool("relevant", "Relevant?");
  public static final BoolExpr<DP> RELEVANT_EXPR = DPEquals.create(RELEVANT, true);
  private DetachComposite myLife;
  private final static CollectionsCompare compare = new CollectionsCompare();

  private final LongArray added = new LongArray();
  private final LongArray changed = new LongArray();
  private final LongArray removed = new LongArray();
  private final int N_ITEMS = 100;
  private final int N_ADD = 25;
  private final int N_CHANGE = 25;
  private final int N_REMOVE = 25;
  private final long[] allItems = new long[N_ITEMS];


  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setWriteToStdout(true);
    myLife = new DetachComposite();
  }

  @Override
  protected void tearDown() throws Exception {
    myLife.detach();
    super.tearDown();
  }

  public void test() throws InterruptedException {
    prepare();
    final SynchronizedBoolean finishedCheck = new SynchronizedBoolean(false);
    db.liveQuery(myLife, RELEVANT_EXPR, new DBLiveQuery.Listener() {
      @Override
      public void onICNPassed(long icn) {
      }

      @Override
      public void onDatabaseChanged(DBEvent event, DBReader reader) {
        try {
          compare.order(added, event.getAddedSorted());
          // todo proper contract for changed -- currently test fails because there are some "extra" items from previous transactions
//          compare.order(changed, event.getChangedSorted());
          compare.order(removed, event.getRemovedSorted());
        } finally {
          finishedCheck.set(true);
        }
      }
    });
    for (int i = 0; i < 20; ++i) {
      finishedCheck.waitForValue(true);
      Log.debug("i = " + i);
      finishedCheck.set(false);
      added.clear();
      changed.clear();
      removed.clear();
      commitChange();
    }
  }

  private void prepare() {
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        for (int i = 0; i < N_ITEMS; ++i) {
          long item = writer.nextItem();
          if (i < 10) {
            writer.setValue(item, RELEVANT, true);
            changed.add(item);
          }
          allItems[i] = item;
        }
      }
    });
  }

  private void commitChange() {
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        LongArray rel = writer.query(RELEVANT_EXPR).copyItemsSorted();
        LongArray notRel = writer.query(RELEVANT_EXPR.negate()).copyItemsSorted();
        int szRel = rel.size();
        int szNotRel = notRel.size();
        for (int i = 0; i < N_ADD; ++i) {
          long item = notRel.get(getRandom().nextInt(szNotRel));
          writer.setValue(item, RELEVANT, true);
          added.add(item);
        }
        added.sortUnique();
        for (int i = 0; i < N_CHANGE; ++i) {
          long item = rel.get(getRandom().nextInt(szRel));
          writer.setValue(item, DBAttribute.NAME, "i" + getRandom().nextInt());
          changed.add(item);
        }
        changed.sortUnique();
        for (int i = 0; i < N_REMOVE; ++i) {
          long item = rel.get(getRandom().nextInt(szRel));
          writer.setValue(item, RELEVANT, false);
          removed.add(item);
          changed.remove(item);
        }
        removed.sortUnique();
      }
    });
  }
}
