package com.almworks.jira.provider3.sync.download2.process;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.spi.provider.util.ServerSyncPoint;

import java.util.Date;
import java.util.List;

public class SyncPointHolder {
  private int myNewestDateIssueId = 0;
  private String myNewestDateIssueMtime = null;
  private boolean myHasError = false;
  private Date myNewestDate = null;
  private Date myLastCreatedTime = null;
  private volatile boolean myLastCreatedRead = false;

  public ServerSyncPoint getServerSyncPoint() {
    if (myHasError || myNewestDate == null || myLastCreatedTime == null) return null;
    return new ServerSyncPoint(myNewestDate.getTime(), myNewestDateIssueId, myNewestDateIssueMtime, myLastCreatedTime.getTime());
  }

  public void loadLastCreated(ItemVersion connection) {
    Date lastCreated = new Date(ServerSyncPoint.loadSyncPoint(connection).getLastCreatedTime());
    if (myLastCreatedTime == null || lastCreated.after(myLastCreatedTime)) myLastCreatedTime = lastCreated;
    myLastCreatedRead = true;
  }

  public void processIssues(List<EntityHolder> issues) {
    for (EntityHolder issue : issues) {
      Date updated = issue.getScalarValue(ServerIssue.UPDATED);
      if (updated == null) continue; // Linked issue has ID and KEY only
      Date created = issue.getScalarValue(ServerIssue.CREATED);
      if (created != null && (myLastCreatedTime == null || created.after(myLastCreatedTime)))
        myLastCreatedTime = created;
      if (!myHasError && (myNewestDate == null || updated.after(myNewestDate))) {
        myNewestDate = updated;
        Integer issueId = issue.getScalarValue(ServerIssue.ID);
        String updatedString = issue.getScalarValue(ServerIssue.UPDATED_STRING);
        if (issueId != null && updatedString != null) {
          myNewestDateIssueId = issueId;
          myNewestDateIssueMtime = updatedString;
        }
      }
    }
  }


  public void markError() {
    myHasError = true;
  }

  public void ensureLastCreatedRead(ItemVersion connection) {
    if (!myLastCreatedRead) loadLastCreated(connection);
  }
}
