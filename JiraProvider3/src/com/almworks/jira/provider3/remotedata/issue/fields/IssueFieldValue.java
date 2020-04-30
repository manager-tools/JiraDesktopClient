package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.remotedata.issue.EditIssueRequest;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.restconnector.operations.RestServerInfo;

public interface IssueFieldValue {
  /**
   * Check current server state for conflicts. If a conflict is detected  returns displayable description, also should log conflict explanation.
   * @return null if no conflict detected. Not null - displayable conflict description
   */
  String checkInitialState(EntityHolder issue);

  /**
   * @param edit
   * @see #uploadDone(boolean)
   * @see EditIssueRequest#addEdit(IssueFieldValue, String, org.json.simple.JSONArray)
   */
  void addChange(EditIssueRequest edit) throws UploadProblem.Thrown;

  /**
   * Notification if the upload collected via {@link #addChange(com.almworks.jira.provider3.remotedata.issue.EditIssueRequest)} was succeeded or failed.<br>
   * It is NOT guaranteed that this method be called after {@link #addChange(com.almworks.jira.provider3.remotedata.issue.EditIssueRequest) addChange}
   * this means that upload failed or was not performed at all or the value is ignored by the upload.
   * @see #addChange(com.almworks.jira.provider3.remotedata.issue.EditIssueRequest)
   * @see #needsUpload
   */
  void uploadDone(boolean success);

  /**
   * The value may be uploaded via different algorithms (depending on kind of issue change and JIRA configuration), or may be not uploadable via edit. Method is used to avoid
   * useless uploads, when anything is already uploaded during previous steps.
   * @return false if the value does not need upload - it is not changed or is uploadable via edit and has been asked to add change and then upload success is {@link #uploadDone(boolean) confirmed}.<br>
   * true if the value is changed and uploadable via edit and has never been added (or has been added, but is not confirmed)
   * @see #uploadDone(boolean)
   * @see #addChange(com.almworks.jira.provider3.remotedata.issue.EditIssueRequest)
   */
  boolean needsUpload(RestServerInfo serverInfo);

  /**
   * @see com.almworks.jira.provider3.services.upload.UploadUnit#finishUpload(com.almworks.items.entities.api.collector.transaction.EntityTransaction, com.almworks.jira.provider3.services.upload.PostUploadContext)
   */
  void finishUpload(long issueItem, EntityHolder issue, PostUploadContext context);

  /**
   * @return displayable name of the value. This name may be shown to a user to point to a particular upload value
   */
  String getDisplayName();
}
