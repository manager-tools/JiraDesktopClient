package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import org.jetbrains.annotations.Nullable;

public class UploadUnitUtils {
  /**
   * @return true iff post-edit unit has to wait for edit<br>
   *   false means that edit is already failed or done - unit should decide if it can upload when edit unit is failed
   */
  public static boolean waitPostEdit(UploadContext context, EditIssue edit) {
    return !edit.isDone() && !context.isFailed(edit);
  }

  /**
   * @return true iff pre-edit unit sure cannot perform because of submit issue is failed or conflict detected<br>
   *   false does not mean that the unit may perform right now because of the issue may be not submitted yet, if the issue is submitted - then the pre-edit unit may proceed
   */
  public static boolean isPreEditFailed(UploadContext context, EditIssue edit) {
    return edit.isConflictDetected() || context.isFailed(edit.getCreate());
  }

  /**
   * Utility method for units which wait till edit is complete
   * @return null if an issue is created and edit has already been done or failed.<br>
   *   nou null is a not-now problem
   */
  @Nullable("When edit is already done")
  public static UploadProblem checkEditNotDoneYet(CreateIssueUnit issue, UploadContext context) {
    Integer issueId = issue.getIssueId();
    if (issueId == null) return UploadProblem.notNow("Edit is not submitted yet");
    EditIssue edit = issue.getEdit();
    if (edit != null && UploadUnitUtils.waitPostEdit(context, edit)) return UploadProblem.notNow("Edit is not done yet");
    return null;
  }
}
