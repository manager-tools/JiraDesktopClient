package com.almworks.jira.provider3.app.sync;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.details.RestIssueProcessor;
import com.almworks.jira.provider3.sync.download2.details.RestQueryPager;
import com.almworks.jira.provider3.sync.download2.process.DBIssueWrite;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.JRIssue;
import com.almworks.jira.provider3.sync.download2.rest.JqlSearch;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.jql.JQLCompareConstraint;
import com.almworks.restconnector.jql.JQLConstraint;
import com.almworks.restconnector.jql.JqlQuery;
import com.almworks.spi.provider.util.ServerSyncPoint;
import com.almworks.util.LogHelper;
import org.json.simple.JSONObject;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

class RestDownloadUpdatedIssues extends BaseOperation {
  private final ServerInfo myServerInfo;
  private final ConnectionSyncInfo mySyncInfo;
  private final RemoteMetaConfig myMetaConfig;
  private ServerSyncPoint myNewSyncPoint;

  public RestDownloadUpdatedIssues(ServerInfo serverInfo, ConnectionSyncInfo syncInfo, ProgressInfo progress, RemoteMetaConfig metaConfig) {
    super(progress);
    myServerInfo = serverInfo;
    mySyncInfo = syncInfo;
    myMetaConfig = metaConfig;
  }

  @Override
  public void perform(RestSession session) throws ConnectorException {
    myProgress.startActivity("Loading updated issues");
    boolean notSynchronized = mySyncInfo.getSyncDate() == null;
    JqlQuery jql = buildJql();
    try {
      if (notSynchronized) firstSync(session, jql, myProgress.spawnAll());
      else {
        if (isNothingChanged(session, jql, myProgress.spawn(0.1))) return;
        DBIssueWrite write = new DBIssueWrite(session, myServerInfo, myMetaConfig);
        RestIssueProcessor.loadQuery(session, jql, write, myProgress.spawnAll());
        myNewSyncPoint = write.getLatestUpdate();
      }
    } finally {
      myProgress.setDone();
    }
  }

  private void firstSync(RestSession session, JqlQuery jql, ProgressInfo progress) throws ConnectorException {
    jql = jql.orderBy("ORDER BY updated DESC");
    DBIssueWrite write = new DBIssueWrite(session, myServerInfo, myMetaConfig);
    RestQueryPager pager = RestQueryPager.allFields(jql);
    pager.setMaxResult(1);
    RestIssueProcessor processor = RestIssueProcessor.loadQuery(session, write, progress, pager);
    if (processor.getIssueCount() == 0) {
      LogHelper.warning("No one issue loaded on first sync");
      return;
    }
    myNewSyncPoint = write.getLatestUpdate();
  }

  private boolean isNothingChanged(RestSession session, JqlQuery jql, ProgressInfo progress) throws ConnectorException {
    try {
      progress.startActivity("Detecting changes on server");
      JSONObject issue = new JqlSearch(jql.orderBy("ORDER BY updated DESC")).addFields("updated").querySingle(session);
      if (issue == null) return true;   // Last updated issue is deleted, no more changes
      ServerSyncPoint syncPoint = mySyncInfo.getSyncPoint();
      Integer id = JRIssue.ID.getValue(issue);
      Date updated = JRIssue.UPDATED.getValue(issue);
      return id != null && updated != null && id.equals(syncPoint.getLatestIssueId()) && updated.getTime() == syncPoint.getSyncTime();
    } finally {
      progress.setDone();
    }
  }

  private JqlQuery buildJql() {
    Set<Integer> projectIds = mySyncInfo.getProjectFilterIds();
    JQLConstraint projects = null;
    if (projectIds != null && !projectIds.isEmpty()) {
      projects = JQLCompareConstraint.in(
              "project",
              projectIds.stream().map(Object::toString).collect(Collectors.toList()),
              false, "Configured projects");
    }

    Date syncDate = mySyncInfo.getSyncDate();
    JQLConstraint updated = null;
    if (syncDate != null) updated = new JQLCompareConstraint.Single("updated", String.valueOf(syncDate.getTime()), ">=", "Updated");
    return new JqlQuery(CompositeConstraint.Simple.and(projects, updated));
  }

  public ServerSyncPoint getNewSyncPoint() throws ConnectorException {
    maybeThrowError();
    return Boolean.TRUE.equals(isSuccessful()) ? myNewSyncPoint : null;
  }
}
