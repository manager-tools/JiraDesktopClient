package com.almworks.items.dp;

import com.almworks.integers.LongArray;
import com.almworks.items.api.*;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import org.jetbrains.annotations.Nullable;

public class ResolvingDPTests extends MemoryDatabaseFixture {
  public static DBAttribute<String> TEXT = DBAttribute.String("test:a:text", "text");

  public void testNot() {
    final long[] items = new long[3];
    final long[] itemType = new long[1];
    final DBItemType itemTypeObj = new DBItemType("test:t:item", "item");
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        itemType[0] = writer.materialize(itemTypeObj);
        for (int i = 0; i < items.length; ++i) {
          long item = writer.nextItem();
          writer.setValue(item, DBAttribute.TYPE, itemType[0]);
          items[i] = item;
        }
        writer.setValue(items[0], TEXT, "a");
        writer.setValue(items[1], TEXT, "a");
        writer.setValue(items[2], TEXT, "b");
      }
    });
    BoolExpr<DP> typeExpr = DPEquals.create(DBAttribute.TYPE, itemType[0]);
    final BoolExpr<DP> withNot = TrivialResolvingDP.INSTANCE.term().negate().and(typeExpr);
    final BoolExpr<DP> withoutNot = TrivialResolvingDP.INSTANCE.term().and(typeExpr);

    final LongArray withNotRes = new LongArray();
    final LongArray withoutNotRes = new LongArray();
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        withNotRes.addAll(reader.query(withNot).copyItemsSorted());
        withoutNotRes.addAll(reader.query(withoutNot).copyItemsSorted());
        return null;
      }
    }).waitForCompletion();
    assertEquals(1, withNotRes.size());
    assertEquals(2, withoutNotRes.size());
  }

  private static class TrivialResolvingDP extends DP {
    public static TrivialResolvingDP INSTANCE = new TrivialResolvingDP();

    @Override
    public boolean accept(long item, DBReader reader) {
      fail(String.valueOf(item));
      return false;
    }

    @Override
    public BoolExpr<DP> resolve(DBReader reader, @Nullable ResolutionSubscription subscription) {
      return DPEquals.create(TEXT, "a");
    }

    @Override
    protected boolean equalDP(DP other) {
      if (other == this) return true;
      if (other instanceof TrivialResolvingDP) {
        fail("there must be only once instance");
        return true;
      }
      return false;
    }

    @Override
    protected int hashCodeDP() {
      return 239;
    }
  }
}
