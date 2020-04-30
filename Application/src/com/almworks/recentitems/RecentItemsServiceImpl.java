package com.almworks.recentitems;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPCompare;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.wrapper.DatabaseUnwrapper;
import com.almworks.util.bool.BoolExpr;

public class RecentItemsServiceImpl implements RecentItemsService {
  private static final int MAX_LENGTH = 20;

  private final Database myDatabase;

  public RecentItemsServiceImpl(Database database) {
    myDatabase = database;
  }

  @Override
  public void addRecord(final long item, final RecordType type) {
    final long timestamp = System.currentTimeMillis();
    myDatabase.writeForeground(new WriteTransaction<Void>() {
      @Override
      public Void transaction(DBWriter writer) throws DBOperationCancelledException {
        doAddRecord(item, timestamp, type, writer);
        return null;
      }
    });
  }

  private void doAddRecord(long master, long timestamp, RecordType type, DBWriter writer) {
    // clear earlier duplicates
    final BoolExpr<DP> exprDupes = BoolExpr.and(EXPR_RECORDS,
      DPEquals.create(ATTR_MASTER, master),
      DPEquals.create(ATTR_REC_TYPE, type.getId()),
      DPCompare.less(ATTR_TIMESTAMP, timestamp, true));

    final LongArray dupes = writer.query(exprDupes).copyItemsSorted();
    for(final LongIterator it = dupes.iterator(); it.hasNext();) {
      DatabaseUnwrapper.clearItem(writer, it.nextValue());
    }

    // add new record
    final long newRec = writer.nextItem();
    writer.setValue(newRec, DBAttribute.TYPE, writer.materialize(TYPE_RECORD));
    writer.setValue(newRec, ATTR_MASTER, master);
    writer.setValue(newRec, ATTR_TIMESTAMP, timestamp);
    writer.setValue(newRec, ATTR_REC_TYPE, type.getId());

    // remove the oldest record if too many
    final DBQuery query = writer.query(EXPR_RECORDS);
    if(query.count() > MAX_LENGTH) {
      long oldestRec = 0L;
      long oldestTime = Long.MAX_VALUE;
      final LongArray records = query.copyItemsSorted();
      for(final LongIterator it = records.iterator(); it.hasNext();) {
        final long record = it.nextValue();
        final long ts = writer.getValue(record, ATTR_TIMESTAMP);
        if(ts < oldestTime) {
          oldestTime = ts;
          oldestRec = record;
        }
      }
      if(oldestRec > 0L) {
        DatabaseUnwrapper.clearItem(writer, oldestRec);
      }
    }
  }
}
