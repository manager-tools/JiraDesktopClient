package com.almworks.jira.provider3.services.upload;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBResult;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.*;
import com.almworks.jira.provider3.sync.download2.process.DBIssueWrite;
import com.almworks.jira.provider3.sync.download2.process.util.DownloadIssueUtil;
import com.almworks.recentitems.RecentItemsService;
import com.almworks.recentitems.RecordType;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.LongSet;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectProcedure;

import java.util.Collection;
import java.util.List;

class FinishUploadTransaction implements DownloadProcedure<UploadDrain> {
  private final TLongObjectHashMap<PostUploadContext.Done> myDone;
  private final EntityTransaction myTransaction;
  private final UploadContextImpl myContext;
  private final RecentItemsService myRecentService;

  public FinishUploadTransaction(TLongObjectHashMap<PostUploadContext.Done> done, EntityTransaction transaction, UploadContextImpl context, RecentItemsService recentService) {
    myDone = done;
    myTransaction = transaction;
    myContext = context;
    myRecentService = recentService;
  }

  @Override
  public void write(final UploadDrain drain) throws DBOperationCancelledException {
    Pair<LongList, LongList> createChanged = selectCreateChange(drain);
    writeDoneUpload(drain);
    writeIssues(drain);
    for (LongIterator cursor : createChanged.getFirst()) myRecentService.addRecord(cursor.value(), RecordType.NEW_UPLOAD);
    for (LongIterator cursor : createChanged.getSecond()) myRecentService.addRecord(cursor.value(), RecordType.EDIT_UPLOAD);
  }

  private Pair<LongList, LongList> selectCreateChange(VersionSource source) {
    LongArray created = new LongArray();
    LongArray updated = new LongArray();
    for (ItemVersion item : source.readItems(myContext.getUploadedPrimary())) (item.getSyncState() == SyncState.NEW ? created : updated).add(item.getItem());
    created.sortUnique();
    updated.sortUnique();
    return Pair.<LongList, LongList>create(created, updated);
  }

  private void writeIssues(UploadDrain drain) {
    EntityWriter writer = DownloadIssueUtil.prepareWrite(myTransaction, drain);
    DBIssueWrite.beforeWrite(writer);
    writer.write();
  }

  private void writeDoneUpload(final UploadDrain drain) {
    final LongSet locked = LongSet.copy(drain.getLockedForUpload());
    myDone.forEachEntry(new TLongObjectProcedure<PostUploadContext.Done>() {
      @Override
      public boolean execute(long item, PostUploadContext.Done result) {
        Collection<DBAttribute<?>> state = result.getAttributes();
        int history = result.getStepsCount();
        if (!locked.contains(item))
          LogHelper.error("not during upload", drain.forItem(item), state, history);
        else {
          locked.remove(item);
          List<Pair<UploadUnit,UploadProblem>> conflicts = myContext.getConflicts(item);
          if (conflicts != null) {
            if (history != 0) {
              LogHelper.error("Some history uploaded for conflict item", item, history, conflicts, result.getHistoryUnits());
              drain.finishUpload(item, state, history); // Mark upload result if something has been actually uploaded
            } else drain.cancelUpload(item);
          } else {
            ItemVersionCreator creator = drain.finishUpload(item, state, history);
            LogHelper.assertError(creator != null, "Failed to complete upload", item, state, history);
          }
        }
        return true;
      }
    });
    for (int i = 0; i < locked.size(); i++) drain.cancelUpload(locked.get(i));
  }

  @Override
  public void onFinished(DBResult<?> result) {
  }
}
