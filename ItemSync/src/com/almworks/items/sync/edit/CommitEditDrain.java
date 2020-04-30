package com.almworks.items.sync.edit;

import com.almworks.integers.LongCollector;
import com.almworks.integers.LongList;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditorLock;
import com.almworks.items.util.AttributeMap;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.NotNull;

class CommitEditDrain extends BaseEditDrain {
  private final boolean myReleaseAfterCommit;
  private final CommitCounterpart myCounterpart;
  private final LongList myWereLocked;

  public CommitEditDrain(SyncManagerImpl manager, @NotNull CommitCounterpart counterpart, EditCommit commit,
    EditorLock lock, boolean releaseAfterCommit) {
    super(manager, lock, commit);
    myCounterpart = counterpart;
    myReleaseAfterCommit = releaseAfterCommit;
    myWereLocked = lock != null ? lock.getItems() : LongList.EMPTY;
  }

  protected TLongObjectHashMap<AttributeMap> getBases() {
    return myCounterpart.prepareCommit(getWriter());
  }

  @Override
  protected void collectToMerge(LongCollector target) {
    super.collectToMerge(target);
    target.addAll(myWereLocked);
  }

  @Override
  protected void onFinish(EditCommit commit, boolean success) {
    if (myCounterpart != null) myCounterpart.commitFinished(commit, success, success && myReleaseAfterCommit);
  }
}
