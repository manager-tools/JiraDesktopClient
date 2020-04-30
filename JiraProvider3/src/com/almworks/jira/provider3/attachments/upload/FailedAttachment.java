package com.almworks.jira.provider3.attachments.upload;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.jira.provider3.schema.Attachment;
import com.almworks.jira.provider3.sync.download2.process.util.SlaveUploadFailures;
import com.almworks.jira.provider3.sync.schema.ServerAttachment;
import com.almworks.jira.provider3.sync.schema.ServerWorklog;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.Collections15;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class FailedAttachment {
  private final long myItem;
  private final File myFile;
  private final Date myAttempt;

  public FailedAttachment(long item, File file, Date attempt) {
    myItem = item;
    myFile = file;
    myAttempt = attempt;
  }

  public static void findFailedUploads(EntityWriter writer) {
    new SlaveUploadFailures<FailedAttachment>(ServerWorklog.TYPE, ServerWorklog.ISSUE) {
      @Override
      protected long getItem(FailedAttachment failure) {
        return failure.myItem;
      }

      @Override
      protected boolean matches(FailedAttachment failure, EntityHolder slave) {
        return failure.matches(slave);
      }

      @Override
      protected List<FailedAttachment> loadFailures(DBReader reader, long issueItem) {
        ItemVersion issue = BranchSource.trunk(reader).forItem(issueItem);
        LongArray failures = reader.query(DPEquals.create(Attachment.ISSUE, issue.getItem()).and(DPNotNull.create(SyncSchema.UPLOAD_ATTEMPT))).copyItemsSorted();
        ArrayList<FailedAttachment> result = Collections15.arrayList();
        for (ItemVersion attachment : BranchSource.trunk(reader).readItems(failures)) {
          if (attachment.getSyncState() != SyncState.NEW) continue;
          String localFile = attachment.getValue(Attachment.LOCAL_FILE);
          Date attempt = getUploadAttempt(attachment);
          if (localFile == null) LogHelper.error("Missing local file for attachment", attachment);
          else result.add(new FailedAttachment(attachment.getItem(), new File(localFile), attempt));
        }
        return result;
      }
    }.perform(writer);
  }

  private static Date getUploadAttempt(ItemVersion attachment) {
    byte[] attempt = attachment.getValue(SyncSchema.UPLOAD_ATTEMPT);
    if (attempt == null) return null;
    else {
      ByteArray.Stream stream = new ByteArray.Stream(attempt);
      long time = stream.nextLong();
      if (stream.isSuccessfullyAtEnd()) return new Date(time);
      else {
        LogHelper.error("Failed to read attach attempt", stream);
        return null;
      }
    }
  }

  public long getItem() {
    return myItem;
  }

  public boolean matches(EntityHolder entity) {
    Date created = entity.getScalarValue(ServerAttachment.DATE);
    if (myAttempt != null && created != null && created.before(myAttempt)) return false;
    String name = entity.getScalarValue(ServerAttachment.FILE_NAME);
    return name != null && myFile.getName().equals(name);
  }

}
