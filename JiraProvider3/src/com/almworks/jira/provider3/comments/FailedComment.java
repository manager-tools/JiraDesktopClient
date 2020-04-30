package com.almworks.jira.provider3.comments;

import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.jira.provider3.schema.Comment;
import com.almworks.jira.provider3.sync.download2.process.util.SlaveUploadFailures;
import com.almworks.jira.provider3.sync.schema.ServerComment;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.ArrayList;
import java.util.List;

class FailedComment {
  private final long myItem;
  private final String myText;

  private FailedComment(long item, String text) {
    myItem = item;
    myText = text;
  }

  public static void findFailedUploads(EntityWriter writer) {
    new SlaveUploadFailures<FailedComment>(ServerComment.TYPE, ServerComment.ISSUE) {
      @Override
      protected long getItem(FailedComment failure) {
        return failure.getItem();
      }

      @Override
      protected boolean matches(FailedComment failure, EntityHolder slave) {
        return failure.matches(slave);
      }

      @Override
      protected List<FailedComment> loadFailures(DBReader reader, long issueItem) {
        ArrayList<FailedComment> failures = Collections15.arrayList();
        BranchSource trunk = BranchSource.trunk(reader);
        for (ItemVersion comment : trunk.readItems( reader.query(DPEquals.create(Comment.ISSUE, issueItem).and(DPNotNull.create(SyncSchema.UPLOAD_ATTEMPT))).copyItemsSorted())) {
          if (comment.getSyncState() != SyncState.NEW) continue;
          String text = Comment.loadHumanText(comment);
          if (text.isEmpty()) continue;
          failures.add(new FailedComment(comment.getItem(), text));
        }
        return failures;
      }
    }.perform(writer);
  }

  public long getItem() {
    return myItem;
  }

  public boolean matches(EntityHolder entity) {
    Integer id = entity.getScalarValue(ServerComment.ID);
    if (id == null) return false;
    String text = entity.getScalarValue(ServerComment.TEXT);
    return Util.equals(text, myText);
  }
}
