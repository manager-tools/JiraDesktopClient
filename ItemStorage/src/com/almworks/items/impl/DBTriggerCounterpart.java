package com.almworks.items.impl;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongCollections;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DBTrigger;
import com.almworks.items.api.DBWriter;
import com.almworks.items.impl.sqlite.ExtractionProcessor;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import org.almworks.util.Log;
import org.almworks.util.Util;

public class DBTriggerCounterpart {
  private final DBTrigger myTrigger;

  public DBTriggerCounterpart(DBTrigger trigger) {
    myTrigger = trigger;
  }

  public void initialize(DBWriter writer) {
    long start = System.currentTimeMillis();
    Log.debug("trigger initialize " + myTrigger);
    LongArray resultSet = writer.query(myTrigger.getExpr()).copyItemsSorted();
    long loaded = System.currentTimeMillis();
    Log.debug("trigger init load "+ (loaded - start) + "ms/" + resultSet.size() + "count " + myTrigger);
    myTrigger.apply(resultSet, writer);
    long applied = System.currentTimeMillis();
    Log.debug("trigger applied " + (applied - loaded) + " total time: " + (applied - start) + "ms " + myTrigger);
    long triggerItem = writer.materialize(myTrigger);
    writer.setValue(triggerItem, DBTrigger.LAST_RESULT_SET, resultSet);
    DBTrigger.TRIGGER_UPDATE_ICN.setValue(writer, triggerItem, writer.getTransactionIcn());
  }

  public void apply(TransactionContext context) throws SQLiteException {
    DBWriterImpl writer = new DBWriterImpl(context, null);
    long triggerItem = writer.materialize(myTrigger);
    long lastIcn = Util.NN(DBTrigger.TRIGGER_UPDATE_ICN.getValue(triggerItem, writer), 0L);
    if (lastIcn >= context.getIcn()) {
      Log.warn(myTrigger + " has incorrect ICN " + lastIcn + ", resetting");
      lastIcn = 0;
    }
    Log.debug("trigger maintenance start " + myTrigger);
    long start = System.currentTimeMillis();
    LongList set = selectChangedItems(writer, myTrigger, triggerItem, context, lastIcn);
    long loaded = System.currentTimeMillis() - start;
    Log.debug("trigger maintenance " + loaded + "ms/" + set.size() + "count " + myTrigger);
    if (!set.isEmpty()) {
      myTrigger.apply(set, writer);
      long duration = System.currentTimeMillis() - start;
      Log.debug("trigger maintenance done total: " + duration + "ms " + myTrigger);
    }
    DBTrigger.TRIGGER_UPDATE_ICN.setValue(writer, triggerItem, context.getIcn());
  }

  private LongList selectChangedItems(DBWriterImpl writer, DBTrigger trigger, long triggerItem, TransactionContext context, long lastIcn) throws SQLiteException {
    LongSetBuilder newSetBuilder = new LongSetBuilder();
    ExtractionProcessor.create(trigger.getExpr(), context).loadItems(context, newSetBuilder);
    LongList newSet;

    LongList oldSet = writer.getValue(triggerItem, DBTrigger.LAST_RESULT_SET);

    LongList changedForTrigger;
    if (lastIcn <= 0) {
      if (oldSet != null) {
        Log.error(trigger + ": DB state inconsistent (no last ICN, last result set: " + oldSet + ")");
        newSetBuilder.mergeFromSortedCollection(oldSet);
      }
      newSet = newSetBuilder.commitToArray();
      changedForTrigger = newSet;
    } else {
      newSet = newSetBuilder.commitToArray();
      if (oldSet == null) {
        Log.error(trigger + ": DB state inconsistent (no last result set, last ICN:" + lastIcn + ")");
        oldSet = LongList.EMPTY;
      }
      // icn + 1 because it is the last processed transaction - we need to process newer
      LongList allChanged = context.getChangedItemsSorted(lastIcn + 1);
      changedForTrigger = LongCollections.uniteTwoLengthySortedSetsAndIntersectWithThirdShort(newSet, oldSet, allChanged);
    }

    writer.setValue(triggerItem, DBTrigger.LAST_RESULT_SET, newSet);
    return changedForTrigger;
  }

  @Override
  public String toString() {
    return "DBTC[" + myTrigger + ']';
  }
}
