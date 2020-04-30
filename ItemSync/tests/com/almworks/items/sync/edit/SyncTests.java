package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.*;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.items.sync.util.TestReference;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.SlaveUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.TimeGuard;
import org.almworks.util.Collections15;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class SyncTests extends SyncFixture {
  public static final DBAttribute<String> TEXT = DBAttribute.String("test.text", "Text");
  public static final DBAttribute<Integer> VAL = DBAttribute.Int("test.val", "Val");
  public static final DBAttribute<Integer> ID = DBAttribute.Int("test.id", "ID");
  public static final DBAttribute<Long> MASTER = SlaveUtils.masterReference("test.master", "Master");
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  static {
    SyncSchema.markShadowable(TEXT);
    SyncSchema.markShadowable(VAL);
  }


  public void testEdit() throws ExecutionException, InterruptedException {
    long item = download(1, "abc");
    checkSyncState(item, SyncState.SYNC);

    myNotifications.reset();
    EditControl control = myManager.prepareEdit(item);
    assertNotNull(control);
    assertNull(myManager.findLock(item));
    myNotifications.checkEmpty();
    control.start(myEditFactory);
    myNotifications.checkNotEmptyAndReset();
    TestEditFactory.MyEditor editor = myEditFactory.waitForEditor();
    assertSame(control, myManager.findLock(item));
    checkSyncState(item, SyncState.SYNC);

    myNotifications.reset();
    editor.commit(TEXT, "abc1");
    editor.waitReleased();
    myNotifications.checkNotEmptyAndReset();
    assertNull(myManager.findLock(item));
    mySelector.waitProcessed(item);
    flushWriteQueue();
    checkSyncState(item, SyncState.EDITED);
    AttributeMap base = readAttribute(item, SyncSchema.BASE);
    assertNotNull(base);
    assertEquals("abc", base.get(TEXT));
    CHECK.unordered(base.keySet().toArray(), TEXT);
    assertEquals("abc1", readAttribute(item, TEXT));
  }

  public void testDeleteNoShadowables() throws InterruptedException, ExecutionException {
    long item = download(1, (String) null);
    checkSyncState(item, SyncState.SYNC);
    checkTrunk(item, SyncSchema.INVISIBLE, null);

    boolean started = myManager.commitEdit(LongArray.create(item), new AggregatingEditCommit().addDelete(item));
    assertTrue(started);
    flushWriteQueue();
    checkTrunk(item, SyncSchema.INVISIBLE, true);
    checkSyncState(item, SyncState.LOCAL_DELETE);
  }

  public void testDownload() throws InterruptedException, ExecutionException {
    long item = download(1, "abc");
    mySelector.checkNotProcessed(item);
    checkSyncState(item, SyncState.SYNC);
    assertNull(readAttribute(item, SyncSchema.BASE));
    assertEquals(Integer.valueOf(1), readAttribute(item, ID));
    assertEquals("abc", readAttribute(item, TEXT));

    AttributeMap update = new AttributeMap();
    update.put(ID, 1);
    update.put(TEXT, "abc1");
    update.put(VAL, 10);
    download(item, update);
    assertEquals("abc1", readAttribute(item, TEXT));
    assertEquals(Integer.valueOf(10), readAttribute(item, VAL));
    mySelector.checkNotProcessed(item);
    checkSyncState(item, SyncState.SYNC);

    TestEditFactory.MyEditor editor = myEditFactory.edit(item);

    update.put(TEXT, "abcX");
    update.put(VAL, 20);
    download(item, update);
    checkTrunk(item, TEXT, "abc1");
    checkTrunk(item, VAL, 10);
    checkShadow(item, SyncSchema.DOWNLOAD, TEXT, "abcX");
    checkShadow(item, SyncSchema.DOWNLOAD, VAL, 20);
    mySelector.checkNotProcessed(item);
    checkSyncState(item, SyncState.SYNC);

    editor.commit(TEXT, "abcE");
    editor.waitReleased();
    mySelector.checkProcessed(item);
    checkBase(item, TEXT, "abc1");
    checkBase(item, VAL, 10);
    checkConflict(item, TEXT, "abcX");
    checkConflict(item, VAL, 20);
    checkTrunk(item, TEXT, "abcE");
    checkTrunk(item, VAL, 10);
    checkSyncState(item, SyncState.CONFLICT);

    update.put(TEXT, "abcE");
    download(item, update);
    checkTrunk(item, SyncSchema.CONFLICT, null);
    checkTrunk(item, SyncSchema.BASE, null);
    checkTrunk(item, TEXT, "abcE");
    checkTrunk(item, VAL, 20);
    checkSyncState(item, SyncState.SYNC);
  }

  public void testCreateNewAndUpload() throws InterruptedException, ExecutionException {
    AttributeMap create = new AttributeMap();
    create.put(TEXT, "abc");
    create.put(VAL, 1);
    long item = CommitItemEdit.createNew(myManager, create);
    checkBase(item, SyncSchema.INVISIBLE, true);
    checkSyncState(item, SyncState.NEW);

    TestUploader uploader = TestUploader.beginUpload(myManager, item);
    assertNotNull(uploader);
    assertTrue(uploader.waitStarted());
    checkShadow(item, SyncSchema.UPLOAD_TASK, TEXT, "abc");
    checkShadow(item, SyncSchema.UPLOAD_TASK, VAL, 1);

    performEdit(item, TEXT, "123");
    checkSyncState(item, SyncState.NEW);
    checkTrunk(item, TEXT, "123");
    checkTrunk(item, VAL, 1);
    checkBase(item, TEXT, null);
    checkBase(item, VAL, null);

    AttributeMap uploaded = new AttributeMap();
    uploaded.put(TEXT, "abc");
    uploaded.put(VAL, 2);
    uploader.finishUpload(item, uploaded).waitDone();
    mySelector.waitProcessed(item);

    checkSyncState(item, SyncState.EDITED);
    checkTrunk(item, TEXT, "123");
    checkTrunk(item, VAL, 2);
    checkBase(item, TEXT, "abc");
    checkBase(item, VAL, 2);
    checkTrunk(item, SyncSchema.UPLOAD_TASK, null);
    checkTrunk(item, SyncSchema.DONE_UPLOAD, null);
    checkTrunk(item, SyncSchema.DONE_UPLOAD_HISTORY, null);
    checkTrunk(item, SyncSchema.DOWNLOAD, null);
  }

  public void testSimplifiedEditWithLock() throws InterruptedException, ExecutionException {
    long item = CommitItemEdit.createNew(myManager, editText("1"));
    assertNull(myManager.findLock(item));
    checkTrunk(item, TEXT, "1");
    checkSyncState(item, SyncState.NEW);

    assertTrue(CommitItemEdit.commitLockedAndWait(myManager, item, editText("2")));
    checkTrunk(item, TEXT, "2");
    checkSyncState(item, SyncState.NEW);

    TestUploader.uploadItem(myManager, item);
    checkTrunk(item, TEXT, "2");
    checkTrunk(item, SyncSchema.BASE, null);
    checkSyncState(item, SyncState.SYNC);

    AttributeMap values = editText("3");
    TestEditFactory.MyEditor lock = myEditFactory.edit(item);
    assertNull(CommitItemEdit.commitLocked(myManager, item, values));
    assertSame(lock.getLock(), myManager.findLock(item));
    checkTrunk(item, TEXT, "2");

    lock.cancel();
    assertNull(myManager.findLock(item));
    assertTrue(CommitItemEdit.commitLockedAndWait(myManager, item, values));
    checkTrunk(item, TEXT, "3");
    checkSyncState(item, SyncState.EDITED);
    checkBase(item, TEXT, "2");
  }

  public void testGenericEditWhileSimplified() throws InterruptedException, ExecutionException {
    AttributeMap values = new AttributeMap();
    values.put(VAL, 0);
    long item = CommitItemEdit.createNew(myManager, values);
    checkTrunk(item, VAL, 0);

    for (int i = 1; i < 5; i += 2) {
      values.put(VAL, i);
      CommitItemEdit commit = CommitItemEdit.commitLocked(myManager, item, values);
      assertNotNull(commit);
      assertNotNull(myEditFactory.startEdit(item));
      TestEditFactory.MyEditor editor = myEditFactory.waitForEditor();
      commit.checkDone();
      assertNotNull(editor);
      editor.checkInitialValue(VAL, i);
      values.put(VAL, i + 1);
      editor.commit(values).waitReleased();
      checkTrunk(item, VAL, i + 1);
    }
  }

  public void testEditMoreItems() throws InterruptedException, ExecutionException {
    long item1 = download(1, "abc");
    long item2 = download(2, "cde");
    EditControl control = myManager.prepareEdit(item1);
    assertNotNull(control);
    control.start(myEditFactory);
    TestEditFactory.MyEditor editor = myEditFactory.waitForEditor();
    MyEditMore editMore = new MyEditMore();
    assertTrue(control.include(new LongList.Single(item2), editMore));
    CHECK.order(editMore.waitForAdditional(), item2);
    Map<Long, AttributeMap> commit = Collections15.hashMap();
    commit.put(item1, editText("abc1"));
    commit.put(item2, editText("cde1"));
    editor.commit(commit).waitReleased();
    checkTrunk(item1, TEXT, "abc1");
    checkTrunk(item2, TEXT, "cde1");
    checkSyncState(item1, SyncState.EDITED);
    checkSyncState(item2, SyncState.EDITED);
    checkBase(item1, TEXT, "abc");
    checkBase(item2, TEXT, "cde");
  }

  public void testCreateDeleteSlave() throws InterruptedException, ExecutionException {
    long item = download(1, "abc");
    long slave = createNewSlave(item, 2, "xxx");

    CHECK.order(getSlaves(item, MASTER), slave);
    AttributeMap map = new AttributeMap();
    map.put(SyncSchema.INVISIBLE, true);
    CommitItemEdit.commitLockedAndWait(myManager, slave, map);
    CHECK.order(getSlaves(item, MASTER));
  }

  public void testRevive() throws InterruptedException, ExecutionException {
    AttributeMap create = new AttributeMap();
    create.put(TEXT, "abc");
    create.put(VAL, 1);
    final long item = CommitItemEdit.createNew(myManager, create);
    checkBase(item, SyncSchema.INVISIBLE, true);
    checkSyncState(item, SyncState.NEW);

    AttributeMap delete = new AttributeMap();
    delete.put(SyncSchema.INVISIBLE, true);
    CommitItemEdit.commitLockedAndWait(myManager, item, delete);
    checkTrunk(item, SyncAttributes.EXISTING, null);
    checkTrunk(item, SyncSchema.INVISIBLE, true);

    final TestReference<Boolean> reviveDone = new TestReference<Boolean>();
    myManager.commitEdit(new EditCommit() {
      @Override
      public void performCommit(EditDrain drain) throws DBOperationCancelledException {
        ItemVersionCreator creator = drain.changeItem(item);
        assertFalse(creator.isAlive());
        creator.setAlive();
      }

      @Override
      public void onCommitFinished(boolean success) {
        assertTrue(reviveDone.deferValue(success));
        assertTrue(reviveDone.publishValue());
      }
    });
    assertTrue(reviveDone.waitForPublished());
    checkTrunk(item, SyncAttributes.EXISTING, true);
    checkBase(item, SyncSchema.INVISIBLE, true);
    checkSyncState(item, SyncState.NEW);
  }

  private long createNewSlave(long master, int id, String text) throws InterruptedException {
    AttributeMap map = new AttributeMap();
    map.put(ID, id);
    map.put(TEXT, text);
    map.put(MASTER, master);
    return CommitItemEdit.createNew(myManager, map);
  }

  private long download(int id, String text) throws InterruptedException {
    AttributeMap fresh = new AttributeMap();
    fresh.put(ID, id);
    fresh.put(TEXT, text);
    return downloadNew(fresh);
  }

  private AttributeMap editText(String value) {
    AttributeMap values = new AttributeMap();
    values.put(TEXT, value);
    return values;
  }

  private static class MyEditMore implements EditorFactory, Procedure<TimeGuard<LongList>> {
    private final TimeGuard<LongList> myWait = new TimeGuard<LongList>(this);
    private final AtomicReference<LongList> myLocked = new AtomicReference<LongList>(null);

    @Override
    public ItemEditor prepareEdit(DBReader reader, EditPrepare prepare) throws DBOperationCancelledException {
      LongArray copy = LongArray.copy(prepare.getItems());
      myLocked.set(copy);
      return ItemEditor.STUB;
    }

    @Override
    public void invoke(TimeGuard<LongList> arg) {
      LongList list = myLocked.get();
      if (list != null) arg.setResult(list);
    }

    @Override
    public void editCancelled() {
      LogHelper.error("Not implemented yet");
    }

    public LongList waitForAdditional() throws InterruptedException {
      return myWait.waitAndGet();
    }
  }
}
