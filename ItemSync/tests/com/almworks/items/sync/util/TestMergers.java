package com.almworks.items.sync.util;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.edit.SyncFixture;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.merge.CopyRemoteOperation;
import com.almworks.items.util.AttributeMap;

import java.util.concurrent.ExecutionException;

public class TestMergers extends SyncFixture {
  private static final DBAttribute<String> TEXT_1 = DBAttribute.String("test.text.1", "Text1");
  private static final DBAttribute<String> TEXT_2 = DBAttribute.String("test.text.2", "Text2");
  static {
    SyncSchema.markShadowable(TEXT_1);
    SyncSchema.markShadowable(TEXT_2);
  }

  public void testCopyRemote() throws InterruptedException, ExecutionException {
    mySelector.delegateAll(new CopyRemoteOperation(TEXT_1));
    AttributeMap remote = new AttributeMap();
    remote.put(TEXT_1, "a");
    remote.put(TEXT_2, "1");
    long item = downloadNew(remote);

    performEdit(item, TEXT_1, "b");
    checkTrunk(item, TEXT_1, "a"); // Rolled back since nothing else is changed
    performEdit(item, TEXT_2, "2");
    checkTrunk(item, TEXT_2, "2");

    performEdit(item, TEXT_1, "b");
    checkTrunk(item, TEXT_1, "b");
    checkBase(item, TEXT_1, "a");
    checkBase(item, TEXT_2, "1");

    remote.put(TEXT_2, "2");
    download(item, remote);
    checkTrunk(item, SyncSchema.BASE, null);
    checkTrunk(item, TEXT_1, "a");
    checkTrunk(item, TEXT_2, "2");

    performEdit(item, TEXT_1, "b");
    remote.put(TEXT_1, "c");
    download(item, remote);
    checkTrunk(item, TEXT_1, "c");
    checkTrunk(item, SyncSchema.BASE, null);
    checkSyncState(item, SyncState.SYNC);

    AttributeMap edit = new AttributeMap();
    edit.put(TEXT_1, "b");
    edit.put(TEXT_2, "1");
    myEditFactory.edit(item).commit(edit).waitReleased();
    checkTrunk(item, TEXT_1, "b");
    checkTrunk(item, TEXT_2, "1");
    checkBase(item, TEXT_1, "c");
    checkBase(item, TEXT_2, "2");
    checkSyncState(item, SyncState.EDITED);

    remote.put(TEXT_2, "3");
    download(item, remote);
    checkTrunk(item, TEXT_2, "1");
    checkConflict(item, TEXT_1, "c");
    checkConflict(item, TEXT_2, "3");
    checkSyncState(item, SyncState.CONFLICT);

    remote.put(TEXT_2, "1");
    download(item, remote);
    checkTrunk(item, TEXT_1, "c");
    checkTrunk(item, TEXT_2, "1");
    checkTrunk(item, SyncSchema.BASE, null);
    checkTrunk(item, SyncSchema.CONFLICT, null);
    checkSyncState(item, SyncState.SYNC);
  }
}
