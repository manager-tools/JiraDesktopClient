package com.almworks.items.sync.edit;

import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.impl.SQLiteDatabase;
import com.almworks.items.sync.*;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.sync.util.TestReference;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.BadUtil;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.wrapper.DatabaseWrapper;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.TimeGuard;
import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * Utility methods:<br><br>
 * <b>perform download of new or single item</b><br>
 * {@link #download(long, com.almworks.items.util.AttributeMap)}<br>
 * {@link #downloadNew(com.almworks.items.util.AttributeMap)} <br><br>
 * <b>edit single attribute of the existing item</b><br>
 * {@link #performEdit(long, com.almworks.items.api.DBAttribute, Object)}<br><br>
 * <b>check item state</b><br>
 * {@link #checkTrunk(long, com.almworks.items.api.DBAttribute, Object)}<br>
 * {@link #checkShadow(long, com.almworks.items.api.DBAttribute, com.almworks.items.api.DBAttribute, Object)}<br>
 * {@link #checkSyncState(long, com.almworks.items.sync.SyncState)}
 */
public abstract class SyncFixture extends MemoryDatabaseFixture {
  protected static final CollectionsCompare CHECK = new CollectionsCompare();

  protected SyncManager myManager;
  protected TestEditFactory myEditFactory;
  protected ChangeNotificationLogger myNotifications;
  protected final TestSelector mySelector = new TestSelector();
  public static final DBNamespace TEST_NS = DBNamespace.moduleNs("test");

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    SyncManagerImpl syncManager = new SyncManagerImpl(db, mySelector);
    myManager = syncManager;
    myEditFactory = new TestEditFactory(myManager);
    myNotifications = ChangeNotificationLogger.listen(myManager.getModifiable());
    syncManager.runStartup();
  }

  @Override
  protected Database createMemoryDatabase() {
    DB db = new DB(null, null);
    db.start();
    databases.add(db);
    return new DatabaseWrapper<DB>(db);
  }

  protected SyncManagerImpl getManagerImpl() {
    return (SyncManagerImpl) myManager;
  }

  protected final void commitAndWait(EditCommit commit) throws InterruptedException {
    final CountDownLatch commitOk = new CountDownLatch(1);
    myManager.commitEdit(AggregatingEditCommit.toAggregating(commit).addProcedure(null, new EditCommit.Adapter() {
      @Override
      public void onCommitFinished(boolean success) {
        assertTrue(success);
        commitOk.countDown();
      }
    }));
    commitOk.await();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  protected DownloadValues createDownload(AttributeMap values) {
    return new DownloadValues(0, values);
  }

  protected DownloadValues createDownload(long item, AttributeMap values) {
    return new DownloadValues(item, values);
  }

  protected long download(long item, AttributeMap values) throws InterruptedException {
    return createDownload(item, values).perform(myManager);
  }

  protected long downloadNew(AttributeMap values) throws InterruptedException {
    return createDownload(values).perform(myManager);
  }

  protected <T> void performEdit(long item, DBAttribute<T> attribute, T value) throws InterruptedException {
    myEditFactory.edit(item).commit(attribute, value).waitReleased();
  }

  protected <T> T readAttribute(final long item, final DBAttribute<T> attribute)
    throws ExecutionException, InterruptedException
  {
    return db.readForeground(new ReadTransaction<T>() {
      @Override
      public T transaction(DBReader reader) throws DBOperationCancelledException {
        return reader.getValue(item, attribute);
      }
    }).get();
  }

  protected DBAttribute<?> loadAttribute(final long attributeItem) {
    return db.readForeground(new ReadTransaction<DBAttribute<?>>() {
      @Override
      public DBAttribute<?> transaction(DBReader reader) throws DBOperationCancelledException {
        return BadUtil.getAttribute(reader, attributeItem);
      }
    }).waitForCompletion();
  }

  protected <T> void checkTrunk(long item, DBAttribute<T> attribute, @Nullable T expected) throws ExecutionException, InterruptedException {
    assertEquals(expected, readAttribute(item, attribute));
  }

  protected <T> void checkShadow(long item, DBAttribute<AttributeMap> shadow, DBAttribute<T> attribute, T value)
    throws ExecutionException, InterruptedException
  {
    AttributeMap map = readAttribute(item, shadow);
    assertNotNull("Missing shadow " + shadow.toString(), map);
    assertEquals(value, map.get(attribute));
  }

  protected void checkSyncState(long item, SyncState state) throws ExecutionException, InterruptedException {
    assertEquals(state, getSyncState(item));
  }

  private SyncState getSyncState(final long item) throws ExecutionException, InterruptedException {
    return db.readBackground(new ReadTransaction<SyncState>() {
      @Override
      public SyncState transaction(DBReader reader) throws DBOperationCancelledException {
        ItemVersion trunk = SyncUtils.readTrunk(reader, item);
        ItemVersion base = SyncUtils.readBaseIfExists(reader, item);
        ItemVersion conflict = SyncUtils.readConflictIfExists(reader, item);
        SyncState state = trunk.getSyncState();
        if (base != null) assertEquals(state, base.getSyncState());
        if (conflict != null) assertEquals(state, conflict.getSyncState());
        return state;
      }
    }).get();
  }

  protected <T> void checkBase(long item, DBAttribute<T> attribute, T value) throws ExecutionException, InterruptedException {
    checkShadow(item, SyncSchema.BASE, attribute, value);
  }

  protected <T> void checkConflict(long item, DBAttribute<T> attribute, T value) throws ExecutionException, InterruptedException {
    checkShadow(item, SyncSchema.CONFLICT, attribute, value);
  }

  protected void waitForCanUpload(final long item) throws InterruptedException {
    TimeGuard.waitFor(new Procedure<TimeGuard<Object>>() {
      @Override
      public void invoke(TimeGuard<Object> arg) {
        if (myManager.canUpload(item)) arg.setResult(null);
      }
    });
  }

  protected long[] getSlaves(final long item, final DBAttribute<Long> master) throws ExecutionException, InterruptedException {
    return db.readForeground(new ReadTransaction<long[]>() {
      @Override
      public long[] transaction(DBReader reader) throws DBOperationCancelledException {
        return SyncUtils.readTrunk(reader, item).getSlaves(master).toNativeArray();
      }
    }).get();
  }

  protected long findMaterialized(final DBIdentifiedObject obj) {
    Long item = db.readBackground(new ReadTransaction<Long>() {
      @Override
      public Long transaction(DBReader reader) throws DBOperationCancelledException {
        return reader.findMaterialized(obj);
      }
    }).waitForCompletion();
    return item != null ? item : 0;
  }


  public long materialize(final ItemProxy proxy) {
    final long[] item = new long[1];
    myManager.writeDownloaded(new DownloadProcedure<DBDrain>() {
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        item[0] = drain.materialize(proxy);
      }

      @Override
      public void onFinished(DBResult<?> result) {
      }
    }).waitForCompletion();
    assertTrue(item[0] > 0);
    return item[0];
  }

  public long materialize(final DBIdentifiedObject object) {
    final long[] item = new long[1];
    myManager.writeDownloaded(new DownloadProcedure<DBDrain>() {
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        item[0] = drain.materialize(object);
      }

      @Override
      public void onFinished(DBResult<?> result) {
      }
    }).waitForCompletion();
    assertTrue(item[0] > 0);
    return item[0];
  }

  protected long querySingle(BoolExpr<DP> query) {
    LongList items = query(query);
    assertEquals(1, items.size());
    return items.get(0);
  }

  protected LongList query(final BoolExpr<DP> query) {
    return db.readBackground(new ReadTransaction<LongList>() {
      @Override
      public LongList transaction(DBReader reader) throws DBOperationCancelledException {
        return reader.query(query).copyItemsSorted();
      }
    }).waitForCompletion();
  }

  public static class DownloadValues implements DownloadProcedure<DBDrain> {
    private final long myItem;
    private final AttributeMap myValues;
    private final TestReference<Long> myDoneItem = new TestReference<Long>();

    public DownloadValues(long item, AttributeMap values) {
      myItem = item;
      myValues = values;
    }

    @Override
    public void onFinished(DBResult<?> result) {
      boolean successful = result.isSuccessful();
      Assert.assertTrue(successful);
      if (!successful) assertTrue(myDoneItem.deferValue(null));
      else assertNotNull(myDoneItem.getDeferredValue());
      myDoneItem.publishValue();
    }

    @Override
    public void write(DBDrain drain) throws DBOperationCancelledException {
      ItemVersionCreator creator = myItem > 0 ? drain.changeItem(myItem) : drain.createItem();
      creator.setAlive();
      SyncUtils.copyValues(creator, myValues);
      assertNull(myDoneItem.getDeferredValue());
      myDoneItem.deferValue(creator.getItem());
    }

    public long perform(SyncManager manager) throws InterruptedException {
      manager.writeDownloaded(this);
      return waitDone();
    }

    public long waitDone() throws InterruptedException {
      return myDoneItem.waitForPublished();
    }
  }

  private static class DB extends SQLiteDatabase implements Startable {
    private DB(@Nullable File databaseFile, @Nullable File tempDir) {
      super(databaseFile, tempDir);
    }
  }
}
