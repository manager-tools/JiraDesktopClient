package com.almworks.items.sync.impl;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBResult;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.edit.BaseDBDrain;
import com.almworks.items.sync.edit.SyncFixture;
import com.almworks.items.sync.edit.SyncManagerImpl;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.sync.util.TestReference;
import com.almworks.util.collections.LongSet;
import com.almworks.util.tests.CollectionsCompare;

import java.util.concurrent.atomic.AtomicLong;

public class BranchUtilTests extends SyncFixture {
  public static final DBAttribute<String> SHADOWABLE = TEST_NS.string("shadowable", "shadowable", true);
  public static final DBAttribute<String> DIRECT = TEST_NS.string("direct", "direct", false);
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  public void test() throws InterruptedException {
    final AtomicLong item = new AtomicLong();
    new TestDrain(getManagerImpl(), Branch.TRUNK) {
      @Override
      protected void performTransaction() throws DBOperationCancelledException {
        ItemVersionCreator creator = createItem();
        creator.setValue(DIRECT, "a");
        CHECK.empty(withChangedShadow());
        creator.setValue(SHADOWABLE, "b");
        CHECK.order(withChangedShadow(), creator.getItem());
        HolderCache holders = HolderCache.instance(getReader());
        assertNull(holders.getBase(creator.getItem()));
        VersionReader server = BranchUtil.instance(getReader()).read(creator.getItem(), true);
        assertEquals("b", server.getValue(SHADOWABLE));
        holders.setBase(creator.getItem(), SyncSchema.getInvisible());
        assertNull(server.getValue(SHADOWABLE));
        SyncSchema.isInvisible(holders.getBase(creator.getItem()));
        item.set(creator.getItem());
      }
    }.performSuccessful();
    new TestDrain(getManagerImpl(), Branch.SERVER) {
      @Override
      protected void performTransaction() throws DBOperationCancelledException {
        ItemVersion base = SyncUtils.readBaseIfExists(getReader(), item.get());
        assertNotNull(base);
        ItemVersion old = forItem(item.get());
        assertEquals("a", old.getValue(DIRECT));
        assertNull(old.getValue(SHADOWABLE));
        assertTrue(old.isInvisible());
        ItemVersionCreator change = changeItem(item.get());
        assertTrue(change.isInvisible());
        change.setAlive();
        assertFalse(old.isInvisible());
        change.setValue(SHADOWABLE, "bb");
        assertEquals("bb", old.getValue(SHADOWABLE));
        assertNull(base.getValue(SHADOWABLE));
        assertTrue(base.isInvisible());
      }
    }.performSuccessful();
  }

  private abstract class TestDrain extends BaseDBDrain {
    private final TestReference<Boolean> myResult = new TestReference<Boolean>();
    private final LongSet myNewShadow = new LongSet();
    private final LongSet myChangedShadow = new LongSet();

    TestDrain(SyncManagerImpl manager, Branch branch) {
      super(manager, branch);
    }

    @Override
    protected void onTransactionFinished(DBResult<?> result) {
      myResult.compareAndSet(null, result.isSuccessful());
    }

    public boolean waitFor() throws InterruptedException {
      return myResult.waitForPublished();
    }

    public boolean performSuccessful() throws InterruptedException {
      start();
      boolean result = waitFor();
      assertTrue(result);
      return result;
    }

    @Override
    public void beforeShadowableChanged(long item, boolean isNew) {
      assertFalse(myNewShadow.contains(item));
      assertFalse(myChangedShadow.contains(item));
      (isNew ? myNewShadow : myChangedShadow).add(item);
      super.beforeShadowableChanged(item, isNew);
    }

    public LongList withChangedShadow() {
      LongSet result = new LongSet();
      result.addAll(myNewShadow);
      result.addAll(myChangedShadow);
      return result;
    }
  }
}
