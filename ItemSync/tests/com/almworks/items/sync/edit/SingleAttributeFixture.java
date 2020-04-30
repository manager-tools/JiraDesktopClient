package com.almworks.items.sync.edit;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.util.AttributeMap;

import java.util.concurrent.ExecutionException;

public abstract class SingleAttributeFixture extends SyncFixture {
  public static final DBAttribute<String> TEXT = DBAttribute.String("test.text", "Text");
  public static final DBAttribute<Integer> ID = DBAttribute.Int("test.id", "ID");

  static {
    SyncSchema.markShadowable(TEXT);
  }

  protected LongList queryByText(final String text) {
    return db.readBackground(new ReadTransaction<LongList>() {
      @Override
      public LongList transaction(DBReader reader) throws DBOperationCancelledException {
        return reader.query(DPEquals.create(TEXT, text)).copyItemsSorted();
      }
    }).waitForCompletion();
  }

  protected long createNew(String text) throws InterruptedException, ExecutionException {
    long item = CommitItemEdit.createNew(myManager, createTextOnly(text));
    checkBase(item, SyncSchema.INVISIBLE, true);
    checkTrunk(item, TEXT, text);
    checkSyncState(item, SyncState.NEW);
    return item;
  }

  protected void finishUpload(long item, TestUploader uploader, Integer id, String text) throws InterruptedException {
    mySelector.clear();
    AttributeMap uploaded = createValues(id, text);
    uploader.finishUpload(item, uploaded);
    flushWriteQueue();
  }

  public AttributeMap createValues(Integer id, String text) {
    AttributeMap values = new AttributeMap();
    values.put(TEXT, text);
    values.put(ID, id);
    return values;
  }

  public AttributeMap createTextOnly(String text) {
    return AttributeMap.singleton(TEXT, text);
  }

  protected void downloadUpdated(long item, String text) throws InterruptedException, ExecutionException {
    Integer id = readAttribute(item, ID);
    assertNotNull(id);
    download(item, createValues(id, text));
  }

  protected void checkWholeTrunk(long item, SyncState syncState, Integer id, String text) throws ExecutionException, InterruptedException {
    checkSyncState(item, syncState);
    checkTrunk(item, ID, id);
    checkTrunk(item, TEXT, text);
  }

  protected void editItem(long item, String text, String expectedBaseText) throws InterruptedException, ExecutionException {
    AttributeMap values = new AttributeMap();
    values.put(TEXT, text);
    CommitItemEdit.commitLockedAndWait(myManager, item, values);
    checkSyncState(item, SyncState.EDITED);
    checkBase(item, TEXT, expectedBaseText);
    checkTrunk(item, TEXT, text);
  }

  protected long downloadItem(int id, String text) throws InterruptedException, ExecutionException {
    long item = downloadNew(createValues(id, text));
    mySelector.checkNotProcessed(item);
    checkSyncState(item, SyncState.SYNC);
    checkTrunk(item, SyncSchema.BASE, null);
    checkTrunk(item, ID, id);
    checkTrunk(item, TEXT, text);
    return item;
  }
}
