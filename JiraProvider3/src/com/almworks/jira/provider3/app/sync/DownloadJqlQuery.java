package com.almworks.jira.provider3.app.sync;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.engine.SyncTask;
import com.almworks.integers.IntArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.Database;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.details.RestIssueProcessor;
import com.almworks.jira.provider3.sync.download2.process.DBIssueWrite;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.jql.impl.JqlQueryBuilder;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.jql.JqlQuery;
import com.almworks.util.commons.Procedure;
import com.almworks.util.progress.Progress;

import java.util.List;

class DownloadJqlQuery extends BaseOperation implements DBConnectorOperation {
  private final ServerInfo myServerInfo;
  private final RemoteMetaConfig myMetaConfig;
  private final JqlQuery myJql;
  private final AdditionalDownload myAdditional;
  private long myLastICN = 0;

  public DownloadJqlQuery(ProgressInfo progress, ServerInfo serverInfo, RemoteMetaConfig metaConfig, JqlQuery jql, AdditionalDownload additional) {
    super(progress);
    myServerInfo = serverInfo;
    myMetaConfig = metaConfig;
    myJql = jql;
    myAdditional = additional;
  }

  @Override
  public void perform(RestSession session) throws ConnectorException {
    if (myJql == null) return;
    myAdditional.ensureHasLocalResult();
    DBIssueWrite write = new DBIssueWrite(session, myServerInfo, myMetaConfig);
    final IntArray issueIds = new IntArray();
    write.addPostTransaction(new Procedure<EntityTransaction>() {
      @Override
      public void invoke(EntityTransaction transaction) {
        for (EntityHolder issue : DownloadStageMark.getNotDummy(transaction, ServerIssue.TYPE)) {
          Integer id = issue.getScalarValue(ServerIssue.ID);
          if (id != null) issueIds.add(id);
        }
      }
    });
    RestIssueProcessor.loadQuery(session, myJql, write, myProgress.spawn(0.9));
    myLastICN = write.getLastICN();
    List<String> keys = myAdditional.loadLeftoverKeys(issueIds, myProgress.getCancelFlag());
    if (keys == null) throw new CancelledException();
    DownloadIssuesByKey.downloadKeys(write, keys, myProgress.spawnAll(), session);
  }

  @Override
  public long getLastIcn() throws ConnectorException {
    maybeThrowError();
    return myLastICN;
  }

  public static class Task extends JiraIssueSyncTask {
    private final Constraint myConstraint;
    private final AdditionalDownload myAdditional;

    public Task(String queryName, JiraConnection3 connection, Procedure<SyncTask> runFinally, Constraint constraint, DBFilter view, LongList localResult) {
      super(queryName, connection, runFinally);
      myConstraint = constraint;
      myAdditional = new AdditionalDownload(view, localResult);
    }

    @Override
    protected DBConnectorOperation createOperation(Progress progress) {
      JiraConnection3 connection = getConnection();
      JqlQuery jql = JqlQueryBuilder.buildJQL(getActor(Database.ROLE), myConstraint, connection);
      return new DownloadJqlQuery(new ProgressInfo(progress, myCancelFlag), connection.getServerInfo(), connection.getCustomFields().createIssueConversion(), jql, myAdditional);
    }
  }
}
