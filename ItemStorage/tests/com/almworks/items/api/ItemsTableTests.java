package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.impl.DBReaderImpl;
import com.almworks.items.impl.DBWriterImpl;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;

public class ItemsTableTests extends MemoryDatabaseFixture {
  public void testItemsInserted() {
    final long[] icn = {0};
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        ((DBWriterImpl)writer).itemChanged(TestData.ITEM1);
        icn[0] = writer.getTransactionIcn();
        return null;
      }
    }).waitForCompletion();
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        try {
          TransactionContext context = ((DBReaderImpl) reader).getContext();
          LongList list = context.getChangedItemsSorted(icn[0]);
          assertEquals(LongArray.create(TestData.ITEM1), list);
        } catch (SQLiteException e) {
          fail(e.getMessage());
        }
        return null;
      }
    }).waitForCompletion();
  }
}
