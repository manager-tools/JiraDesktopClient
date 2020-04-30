package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.restconnector.operations.RestServerInfo;

public abstract class BaseValue implements IssueFormValue {
  private final boolean mySupportsEdit;
  private boolean myDone = false;

  protected BaseValue(boolean supportsEdit) {
    mySupportsEdit = supportsEdit;
  }

  public boolean isDone() {
    return myDone;
  }

  @Override
  public void uploadDone(boolean success) {
    if (success) myDone = true;
  }

  @Override
  public boolean needsUpload(RestServerInfo serverInfo) {
    return !myDone && isChanged() && mySupportsEdit;
  }

  @Override
  public void finishUpload(long issueItem, EntityHolder issue, PostUploadContext context) {
    doFinishUpload(issueItem, issue, context);
  }

  protected abstract void doFinishUpload(long issueItem, EntityHolder issue, PostUploadContext context);

  @Override
  public String getDisplayName() {
    IssueFieldDescriptor descriptor = getDescriptor();
    return descriptor.hasDisplayName() ? descriptor.getDisplayName() + " (" + descriptor.getFieldId() + ")" : descriptor.getFieldId();
  }
}
