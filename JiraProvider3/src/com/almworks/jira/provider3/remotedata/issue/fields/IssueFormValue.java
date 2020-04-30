package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.restconnector.operations.RestServerInfo;
import org.jetbrains.annotations.NotNull;

public interface IssueFormValue extends IssueFieldValue {
  IssueFieldDescriptor getDescriptor();

  /**
   * Text presentation of current value. Returns new value if it is changed or old server value otherwise.
   * @return text acceptable to be send to server via form submit
   */
  @NotNull
  String[] getFormValue(RestServerInfo serverInfo);

  /**
   * @return true if the value is updated
   */
  boolean isChanged();
}
