package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.dp.DPCompare;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;

import java.util.Set;

public class DBTriggerTests extends MemoryDatabaseFixture {
  public static DBAttribute<Integer> KEY = DBAttribute.Int("triggerTest:a:key", "Key");
  public static DBAttribute<Set<Long>> ITEMS_CACHE = DBAttribute.LinkSet("triggerTest:a:itemsCache", "Items Cache", false);
  public static final int N_ITEMS = 20;
  public static final int MAX_CHANGED_ITEMS = 10;


  public static final int KEY_THRESH = 1000;
  public static final int MAX_KEY = 2000;
  public static BoolExpr<DP> CACHE_EXPR = DPCompare.less(KEY, KEY_THRESH, false);

  public static CollectionsCompare COMPARE = new CollectionsCompare();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setWriteToStdout(true);
  }

  public void test() {
    final long[] cacheItem = new long[1];
    final long[] items = new long[N_ITEMS];
    final LongArray changedItems = new LongArray();

    prepareItems(cacheItem, items, changedItems);

    db.registerTrigger(new TestDBTrigger(changedItems, cacheItem));
    flushWriteQueue();

    for (int i = 0; i < 50; ++i) {
      Log.debug("iteration " + i);
      changeItems(items, changedItems);
      check(cacheItem[0]);
    }
  }

  private void prepareItems(final long[] cacheItem, final long[] items, final LongArray changedItems) {
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        for (int i = 0; i < N_ITEMS; ++i) {
          long item = writer.nextItem();
          items[i] = item;
          int key = getRandom().nextInt(MAX_KEY);
          KEY.setValue(writer, item, key);
          if (key < KEY_THRESH) changedItems.add(item);
        }
        cacheItem[0] = writer.nextItem();
      }
    });
  }

  private void changeItems(final long[] items, final LongArray changedItems) {
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        changedItems.clear();
        int nItemsToChange = getRandom().nextInt(MAX_CHANGED_ITEMS) + 1;
        LongSetBuilder sItemsToChange = new LongSetBuilder();
        for (int i = 0; i < nItemsToChange; ++i) {
          sItemsToChange.add(items[getRandom().nextInt(N_ITEMS)]);
        }
        for (LongListIterator i = sItemsToChange.commitToArray().iterator(); i.hasNext();) {
          long itemToChange = i.nextValue();
          int newKey = getRandom().nextInt(MAX_KEY);
          int oldKey = writer.getValue(itemToChange, KEY);
          writer.setValue(itemToChange, KEY, newKey);
          if (!(oldKey >= KEY_THRESH && newKey >= KEY_THRESH))
            changedItems.add(itemToChange);
        }
        changedItems.sortUnique();
      }
    });
  }

  private void check(final long cacheItem) {
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(final DBReader reader) throws DBOperationCancelledException {
        COMPARE.unordered(reader.query(CACHE_EXPR).copyItemsSorted().toList(), reader.getValue(cacheItem, ITEMS_CACHE));
        return null;
      }
    }).waitForCompletion();
  }

  private static class TestDBTrigger extends DBTrigger {
    private final LongArray myChangedItems;
    private final long[] myCacheItem;

    public TestDBTrigger(LongArray changedItems, long[] cacheItem) {
      super("triggerTest:o:trigger", CACHE_EXPR);
      myChangedItems = changedItems;
      myCacheItem = cacheItem;
    }

    @Override
    public void apply(LongList itemsSorted, DBWriter writer) {
      COMPARE.order(myChangedItems, itemsSorted);
      Set<Long> keys = Util.NN(writer.getValue(myCacheItem[0], ITEMS_CACHE), Collections15.<Long>hashSet());
      for (LongListIterator i = itemsSorted.iterator(); i.hasNext();) {
        long item = i.nextValue();
        if (writer.getValue(item, KEY) < KEY_THRESH) keys.add(item);
        else keys.remove(item);
      }
      writer.setValue(myCacheItem[0], ITEMS_CACHE, keys);
    }
  }
}