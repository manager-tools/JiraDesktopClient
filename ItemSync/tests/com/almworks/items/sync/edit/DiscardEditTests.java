package com.almworks.items.sync.edit;

import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.tests.CollectionsCompare;

import java.util.concurrent.ExecutionException;

public class DiscardEditTests extends SingleAttributeFixture {
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  public void testNew() throws InterruptedException, ExecutionException {
    long item = createNew("abc");
    checkTrunk(item, SyncAttributes.EXISTING, true);
    CHECK.singleElement(item, queryByText("abc").toNativeArray());

    CommitItemEdit.commitLockedAndWait(myManager, item, null);
    checkTrunk(item, SyncAttributes.EXISTING, null);
    checkBase(item, SyncSchema.INVISIBLE, true);
    checkTrunk(item, TEXT, "abc");
    checkTrunk(item, SyncSchema.INVISIBLE, null);
    CHECK.empty(queryByText("abc"));
  }


  public void testEdited() throws InterruptedException, ExecutionException {
    long item = downloadItem(1, "abc");
    editItem(item, "123", "abc");

    CommitItemEdit.commitLockedAndWait(myManager, item, null);
    checkTrunk(item, SyncSchema.BASE, null);
    checkWholeTrunk(item, SyncState.SYNC, 1, "abc");
  }

  public void testConflict() throws ExecutionException, InterruptedException {
    long item = downloadItem(1, "abc");
    editItem(item, "123", "abc");

    downloadUpdated(item, "xyz");
    checkWholeTrunk(item, SyncState.CONFLICT, 1, "123");
    checkConflict(item, TEXT, "xyz");
    checkBase(item, TEXT, "abc");

    CommitItemEdit.commitLockedAndWait(myManager, item, null);

    checkWholeTrunk(item, SyncState.SYNC, 1, "xyz");
    checkTrunk(item, SyncSchema.CONFLICT, null);
    checkTrunk(item, SyncSchema.BASE, null);
  }

  // todo: make it pass
  public void _testSuccessfulUpload() throws ExecutionException, InterruptedException {
    long item = downloadItem(1, "abc");
    editItem(item, "123", "abc");

    myNotifications.reset();
    TestUploader uploader = TestUploader.beginUpload(myManager, item);
    myNotifications.checkNotEmptyAndReset();
    assertTrue(uploader.waitStarted());
    checkShadow(item, SyncSchema.UPLOAD_TASK, TEXT, "123");

    CommitItemEdit.commitLockedAndWait(myManager, item, null); // Discard during upload
    checkWholeTrunk(item, SyncState.EDITED, 1, "abc");

    myNotifications.reset();
    finishUpload(item, uploader, 1, "123");
    myNotifications.checkNotEmptyAndReset();

    checkWholeTrunk(item, SyncState.EDITED, 1, "abc");
    checkBase(item, TEXT, "123");
  }

  // todo: make it pass
  public void _testSuccessfulUploadNew() throws ExecutionException, InterruptedException {
    long item = createNew("abc");
    myNotifications.reset();
    TestUploader uploader = TestUploader.beginUpload(myManager, item);
    myNotifications.checkNotEmptyAndReset();
    assertTrue(uploader.waitStarted());
    checkShadow(item, SyncSchema.UPLOAD_TASK, TEXT, "abc");

    CommitItemEdit.commitLockedAndWait(myManager, item, null); // Discard during upload
    checkTrunk(item, SyncSchema.INVISIBLE, true);
    checkWholeTrunk(item, SyncState.NEW, null, "abc");

    myNotifications.reset();
    finishUpload(item, uploader, 1, "abc");
    myNotifications.checkNotEmptyAndReset();

    checkWholeTrunk(item, SyncState.DELETE_MODIFIED, 1, "abc");
    checkBase(item, TEXT, null);
    checkBase(item, SyncSchema.INVISIBLE, true);
    checkConflict(item, TEXT, "abc");
    checkConflict(item, SyncSchema.INVISIBLE, null);
  }
}
