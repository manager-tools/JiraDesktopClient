package com.almworks.api.application;

import com.almworks.items.api.*;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.SlaveUtils;
import com.almworks.items.util.SyncAttributes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.almworks.api.application.ItemWrapper.DBStatus.*;
import static com.almworks.items.util.SyncAttributes.INVISIBLE;
import static org.almworks.util.Collections15.arrayList;

public class DBStatusKeyTests extends MemoryDatabaseFixture {
  final static DBAttribute<Long> SLAVE_1 = SlaveUtils.masterReference("slave1", "Slave 1");
  final static DBAttribute<Long> SLAVE_2 = SlaveUtils.masterReference("slave2", "Slave 2");
  final static DBAttribute<String> TEXT = DBAttribute.String("textAttr", "Text attr");

  static {
    SyncSchema.markShadowable(TEXT);
  }

  public void test() {
    final List<TestState> tests = db.write(DBPriority.FOREGROUND, new WriteTransaction<List<TestState>>() {
      @Override
      public List<TestState> transaction(DBWriter w) throws DBOperationCancelledException {
        List<TestState> res = arrayList();

        long item0 = newItem(w, ib().l(TEXT, "new").b(INVISIBLE, true));
        res.add(new TestState(item0, DB_NEW));

        long item1 = newItem(w, ib().l(TEXT, "slave is new"));
        newItem(w, ib().l(TEXT, "s1:new").b(INVISIBLE, true), SLAVE_1, item1);
        newItem(w, ib().l(TEXT, "s2:ok"), SLAVE_2, item1);
        res.add(new TestState(item1, DB_MODIFIED));

        long item2 = newItem(w, ib().l(TEXT, "ok"));
        newItem(w, ib().l(TEXT, "s1-1"), SLAVE_1, item2);
        newItem(w, ib().l(TEXT, "s1-2"), SLAVE_1, item2);
        newItem(w, ib().l(TEXT, "s2-1"), SLAVE_2, item2);
        res.add(new TestState(item2, DB_NOT_CHANGED));

        long item3 = newItem(w, ib().l(TEXT, "conflict").b(TEXT, "base").c(TEXT, "conflicting"));
        newItem(w, ib().l(TEXT, "s1:new").b(INVISIBLE, true), SLAVE_1, item3);
        newItem(w, ib().l(TEXT, "s2:ok"), SLAVE_2, item3);
        res.add(new TestState(item3, DB_CONFLICT));

        long item4 = newItem(w, ib().l(TEXT, "conflict").c(TEXT, "conflicting"));
        newItem(w, ib().l(TEXT, "s1:new").b(INVISIBLE, true), SLAVE_1, item4);
        newItem(w, ib().l(TEXT, "s2:ok"), SLAVE_2, item4);
        res.add(new TestState(item4, DB_CONFLICT));

        long item5 = newItem(w, ib().l(TEXT, "slave is changed"));
        newItem(w, ib().l(TEXT, "s1-1:ok"), SLAVE_1, item5);
        newItem(w, ib().l(TEXT, "s1-2:changed").b(TEXT, "lalala"), SLAVE_1, item5);
        newItem(w, ib().l(TEXT, "s2:ok"), SLAVE_2, item5);
        res.add(new TestState(item5, DB_MODIFIED));

        long item6 = newItem(w, ib().l(TEXT, "slave is in conflict"));
        newItem(w, ib().l(TEXT, "s1-1:ok"), SLAVE_1, item6);
        newItem(w, ib().l(TEXT, "s1-2:conflict").c(TEXT, "lalala"), SLAVE_1, item6);
        newItem(w, ib().l(TEXT, "s2:ok"), SLAVE_2, item6);
        res.add(new TestState(item6, DB_CONFLICT));

        long item7 = newItem(w, ib().l(TEXT, "one slave is changed, one in conflict"));
        newItem(w, ib().l(TEXT, "s1-1:ok"), SLAVE_1, item7);
        newItem(w, ib().l(TEXT, "s1-2:changed").b(TEXT, "lalala"), SLAVE_1, item7);
        newItem(w, ib().l(TEXT, "s2:conflict").c(TEXT, "lalala"), SLAVE_2, item7);
        res.add(new TestState(item7, DB_CONFLICT));

        long item8 = newItem(w, ib().l(TEXT, "one slave is changed, one new"));
        newItem(w, ib().l(TEXT, "s1-1:ok"), SLAVE_1, item8);
        newItem(w, ib().l(TEXT, "s1-2:changed").b(TEXT, "lalala"), SLAVE_1, item8);
        newItem(w, ib().l(TEXT, "s2:new").b(INVISIBLE, true), SLAVE_2, item8);
        res.add(new TestState(item8, DB_MODIFIED));

        long item9 = newItem(w, ib().l(TEXT, "slave of slave is changed"));
        long s1 = newItem(w, ib().l(TEXT, "s1:ok"), SLAVE_1, item9);
        long s1_s1 = newItem(w, ib().l(TEXT, "s1.s1:ok"), SLAVE_1, s1);
        long s1_s2 = newItem(w, ib().l(TEXT, "s1.s2:ok"), SLAVE_2, s1);
        long s1_s1_s1 = newItem(w, ib().l(TEXT, "s1.s1.s1:changed").b(TEXT, "lalala"), SLAVE_1, s1_s1);
        // as of now, only "first-order" slaves are taken into account
        res.add(new TestState(item9, DB_NOT_CHANGED));
        res.add(new TestState(s1_s1, DB_MODIFIED));

        return res;
      }
    }).waitForCompletion();

    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        for (TestState test : tests) {
          test.calculatedState = DBStatusKey.calcStatus(test.item, reader, null);
        }
        return null;
      }
    }).waitForCompletion();

    for (int i = 0; i < tests.size(); ++i) {
      assertEquals("test " + i, tests.get(i).state, tests.get(i).calculatedState);
    }
  }

  private static TestItemBuilder ib() {
    return new TestItemBuilder();
  }

  private long newItem(DBWriter w, TestItemBuilder builder) {
    return newItem(w, builder, null, null);
  }

  private long newItem(DBWriter w, TestItemBuilder builder, @Nullable DBAttribute<Long> slaveAttr, @Nullable Long master) {
    long item = w.nextItem();
    w.setValue(item, SyncAttributes.EXISTING, true);
    if (slaveAttr != null) {
      w.setValue(item, slaveAttr, master);
    }
    w.setValue(item, SyncSchema.BASE, builder.base);
    w.setValue(item, SyncSchema.CONFLICT, builder.conf);
    if (builder.local != null) {
      for (DBAttribute a : builder.local.keySet()) {
        w.setValue(item, a, builder.local.get(a));
      }
    }
    return item;
  }

  static class TestItemBuilder {
    AttributeMap local;
    AttributeMap base;
    AttributeMap conf;

    <T> TestItemBuilder l(DBAttribute<T> a, T v) {
      if (local == null) local = new AttributeMap();
      local.put(a, v);
      return this;
    }

    <T> TestItemBuilder b(DBAttribute<T> a, T v) {
      if (base == null) base = new AttributeMap();
      base.put(a, v);
      return this;
    }

    <T> TestItemBuilder c(DBAttribute<T> a, T v) {
      if (conf == null) conf = new AttributeMap();
      conf.put(a, v);
      return this;
    }
  }

  static class TestState {
    long item;
    ItemWrapper.DBStatus state;
    ItemWrapper.DBStatus calculatedState;

    TestState(long item, ItemWrapper.DBStatus state) {
      this.item = item;
      this.state = state;
    }
  }
}
