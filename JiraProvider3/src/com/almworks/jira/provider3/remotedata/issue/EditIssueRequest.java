package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.restconnector.operations.RestServerInfo;
import org.json.simple.JSONArray;

public abstract class EditIssueRequest {
  private final RestServerInfo myServerInfo;

  public EditIssueRequest(RestServerInfo serverInfo) {
    myServerInfo = serverInfo;
  }

  public abstract boolean isCreate();

  public abstract void addEdit(IssueFieldValue value, String fieldId, JSONArray change);

  public abstract boolean hasOperation(String fieldId, String operation);

  public boolean needsUpload(String fieldId, String operation, boolean hasUpload) {
    ParsedIssueFields.Info info = getFieldInfo(fieldId);
    return info != null && info.hasOperation(operation) && hasUpload;
  }

  public abstract ParsedIssueFields.Info getFieldInfo(String fieldId);

  public RestServerInfo getServerInfo() {
    return myServerInfo;
  }
}
