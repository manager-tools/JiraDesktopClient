package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.services.upload.LoadUploadContext;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import org.jetbrains.annotations.Nullable;

public interface StepLoader {
  /**
   * @throws UploadUnit.CantUploadException if the issue step cannot upload in this upload. The whole issue upload should be stopped in the case because it surely cannot succeed
   * without all steps uploaded.
   */
  @Nullable("When error occurred")
  UploadUnit loadStep(ItemVersion trunk, HistoryRecord record, CreateIssueUnit create, LoadUploadContext context, @Nullable UploadUnit prevStep, int stepIndex)
    throws UploadUnit.CantUploadException;
}
