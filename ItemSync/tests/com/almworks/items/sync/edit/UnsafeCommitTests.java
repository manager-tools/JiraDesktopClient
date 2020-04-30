package com.almworks.items.sync.edit;

import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.util.AggregatingEditCommit;

import java.util.concurrent.ExecutionException;

public class UnsafeCommitTests extends SingleAttributeFixture {
  public void testEdit() throws ExecutionException, InterruptedException {
    long item1 = downloadItem(1, "abc");
    long item2 = downloadItem(2, "qwe");
    TestEditFactory.MyEditor editor = myEditFactory.edit(item1);
    assertSame(editor.getLock(), myManager.findLock(item1));

    AggregatingEditCommit unsafe = new AggregatingEditCommit();
    WaitCommit wait = WaitCommit.addTo(unsafe);
    unsafe.updateValues(item1, createTextOnly("123"));
    unsafe.updateValues(item2, createTextOnly("456"));
    myManager.unsafeCommitEdit(unsafe);
    assertTrue(wait.waitForAllDone());
    checkWholeTrunk(item1, SyncState.EDITED, 1, "123");
    checkWholeTrunk(item2, SyncState.EDITED, 2, "456");
    editor.checkChange(item1, TEXT, "123");
    editor.checkChanges(item2, null);
    editor.checkNoMoreChanges();

    editor.commit(TEXT, "xyz").waitReleased();
    checkWholeTrunk(item1, SyncState.EDITED, 1, "xyz");
    checkBase(item1, TEXT, "abc");
  }

  public void testAutoMerge() throws ExecutionException, InterruptedException {
    long item1 = downloadItem(1, "abc");
    long item2 = downloadItem(2, "qwe");
    editItem(item1, "123", "abc");
    editItem(item2, "XXX", "qwe");
    TestEditFactory.MyEditor editor = myEditFactory.edit(item2);
    assertSame(editor.getLock(), myManager.findLock(item2));

    AggregatingEditCommit unsafe = new AggregatingEditCommit();
    WaitCommit wait = WaitCommit.addTo(unsafe);
    unsafe.updateValues(item1, createTextOnly("abc"));
    unsafe.updateValues(item2, createTextOnly("qwe"));
    myManager.unsafeCommitEdit(unsafe);
    wait.waitForAllDone();
    checkWholeTrunk(item1, SyncState.SYNC, 1, "abc");
    checkWholeTrunk(item2, SyncState.EDITED, 2, "qwe");
    editor.checkChange(item2, TEXT, "qwe");
    editor.checkNoMoreChanges();
    editor.cancel();
    flushWriteQueue();
    checkWholeTrunk(item2, SyncState.SYNC, 2, "qwe");
  }
}
