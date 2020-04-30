package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.sync.util.TestReference;
import com.almworks.items.util.AttributeMap;
import junit.framework.Assert;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class CommitItemEdit implements EditCommit {
  private final AttributeMap myMap;
  @Nullable
  private final EditControl myControl;
  private final long myItem;
  private final TestReference<Long> myCommitted = new TestReference<Long>();

  public CommitItemEdit(@Nullable AttributeMap map, @Nullable EditControl control, long item) {
    myMap = map;
    myControl = control;
    myItem = item;
  }

  public static long createNew(SyncManager manager, @NotNull AttributeMap values) throws InterruptedException {
    CommitItemEdit commit = new CommitItemEdit(values, null, -1);
    manager.commitEdit(commit);
    return commit.waitDone();
  }

  /**
   * @param values null for discard
   */
  @Nullable
  public static CommitItemEdit commitLocked(SyncManager manager, long item, @Nullable AttributeMap values) {
    CommitItemEdit commit = new CommitItemEdit(values, null, item);
    boolean success = manager.commitEdit(LongArray.create(item), commit);
    return success ? commit : null;
  }

  /**
   * @param values null for discard
   */
  public static boolean commitLockedAndWait(SyncManager manager, long item, @Nullable AttributeMap values)
    throws InterruptedException
  {
    CommitItemEdit commit = commitLocked(manager, item, values);
    if (commit == null) return false;
    Assert.assertEquals(item, commit.waitDone());
    return true;
  }

  private long waitDone() throws InterruptedException {
    Long item = myCommitted.waitForPublished();
    Assert.assertNotNull(item);
    return item;
  }

  @Override
  public void performCommit(EditDrain drain) throws DBOperationCancelledException {
    ItemVersionCreator creator = myItem > 0 ? drain.changeItem(myItem) : drain.createItem();
    creator.setAlive();
    if (myMap != null) SyncUtils.copyValues(creator, myMap);
    else if (myItem > 0) drain.discardChanges(myItem);
    else Log.error("Cannot discard new item");
    Assert.assertTrue(myCommitted.deferValue(creator.getItem()));
  }

  @Override
  public void onCommitFinished(boolean success) {
    if (myControl != null) myControl.release();
    Assert.assertTrue(myCommitted.publishValue());
  }

  public void checkDone() {
    Assert.assertTrue(myCommitted.isPublished());
  }
}
