package com.almworks.items.impl.sqlite;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.util.bool.BoolExpr;

import java.util.*;

/**
 * @author Igor Sereda
 */
public class DistributionCountTests extends MemoryDatabaseFixture {

  public void test() {
    db.read(DBPriority.FOREGROUND, new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        assertTrue(reader.query(BoolExpr.<DP>TRUE()).distributionCount().isEmpty());
        return null;
      }
    });
    db.write(DBPriority.FOREGROUND, new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        int COUNT = 20;
        long p = writer.nextItem();
        for (int i = 1; i < COUNT; i++) writer.nextItem();
        for (long i = p; i < p + COUNT; i++) writer.setValue(i, TestData.BOOL, true);
        for (long i = p; i < p + 5; i++) writer.setValue(i, TestData.LONG, 50L);
        for (long i = p + 5; i < p + 20; i++) writer.setValue(i, TestData.LONG, 100L);
        for (long i = p; i < p + 10; i++) writer.setValue(i, TestData.LONG_LIST, Arrays.asList(-1L));
        for (long i = p + 10; i < p + 15; i++) writer.setValue(i, TestData.LONG_LIST, Arrays.asList(-1L, -2L, -3L));
        for (long i = p + 15; i < p + 18; i++) writer.setValue(i, TestData.LONG_LIST, Arrays.asList(-2L));
        return null;
      }
    });
    db.read(DBPriority.FOREGROUND, new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        DBQuery q = reader.query(DPEquals.create(TestData.BOOL, true));
        LongList r = q.distributionCount(TestData.LONG);
        checkDistribution(r, 1, 50L, 5, 100L, 15);
        r = q.distributionCount(TestData.LONG, TestData.LONG_LIST);
        checkDistribution(r, 2, 50L, -1, 5, 100, -1, 10, 100, -2, 8, 100, -3, 5, 100, 0, 2);
        r = q.distributionCount(TestData.LONG_LIST, TestData.LONG_SET, TestData.LONG);
        checkDistribution(r, 3, -1L, 0, 50L, 5, -1, 0, 100, 10, -2, 0, 100, 8, -3, 0, 100, 5, 0, 0, 100, 2);
        return null;
      }
    });
  }

  private void checkDistribution(LongList r, int dims, long ... expected) {
    assertEquals(createMap(LongArray.create(expected), dims), createMap(r, dims));
  }

  private Map<List<Long>, Long> createMap(LongList list, int dims) {
    HashMap<List<Long>, Long> r = new HashMap<List<Long>, Long>();
    int size = list.size();
    int i = 0;
    while (i < size) {
      ArrayList<Long> key = new ArrayList<Long>();
      for (int j = 0; j < dims; j++) {
        key.add(list.get(i++));
      }
      r.put(key, list.get(i++));
    }
    return r;
  }
}
