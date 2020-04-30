package com.almworks.jira.provider3.app.sync;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.SyncTask;
import com.almworks.integers.IntArray;
import com.almworks.integers.IntList;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.details.CustomFieldsSchema;
import com.almworks.jira.provider3.sync.download2.details.RestIssueProcessor;
import com.almworks.jira.provider3.sync.download2.process.DBIssueWrite;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.progress.Progress;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.almworks.util.ExceptionUtil.maybeThrow;

class DownloadIssuesByKey extends BaseOperation implements DBConnectorOperation {
  private final List<String> myKeys;
  private final ServerInfo myServerInfo;
  private final RemoteMetaConfig myMetaConfig;
  private final AtomicReference<IntList> myActualIssueIds = new AtomicReference<IntList>(null);
  private final AtomicReference<Boolean> mySuccessfullyDone = new AtomicReference<Boolean>(null);
  private long myLastIcn = 0;
  private ConnectorException myError;

  public DownloadIssuesByKey(List<String> keys, ScalarModel<Boolean> cancelFlag, ServerInfo serverInfo, Progress progress,
    RemoteMetaConfig metaConfig) {
    super(new ProgressInfo(progress, cancelFlag));
    myKeys = keys;
    myServerInfo = serverInfo;
    myMetaConfig = metaConfig;
  }

  @Override
  public long getLastIcn() throws ConnectorException {
    maybeThrow(myError);
    return myLastIcn;
  }

  @Override
  public void onCancelled() {
    myProgress.addError("Cancelled");
    mySuccessfullyDone.compareAndSet(null, false);
  }

  @Override
  public void onCompleted(SyncTask.State result) {
    myProgress.setDone();
    mySuccessfullyDone.compareAndSet(null, result.isSuccessful());
  }

  @Override
  public void onError(ConnectorException e) {
    myProgress.addError(e.getMediumDescription());
    myError = e;
    LogHelper.warning(e);
    mySuccessfullyDone.compareAndSet(null, false);
  }

  @Override
  public void perform(final RestSession session) throws ConnectorException {
    myServerInfo.ensureHasData(session);
    DBIssueWrite write = new DBIssueWrite(session, myServerInfo, myMetaConfig);
    final IntArray issueIds = new IntArray();
    write.addPostTransaction(new Procedure<EntityTransaction>() {
      @Override
      public void invoke(EntityTransaction transaction) {
        for (EntityHolder issue : DownloadStageMark.getNotDummy(transaction, ServerIssue.TYPE)) {
          String key = issue.getScalarValue(ServerIssue.KEY);
          if (key == null || !myKeys.contains(key)) continue;
          Integer id = issue.getScalarValue(ServerIssue.ID);
          if (id != null) issueIds.add(id);
        }
      }
    });
    List<String> keys = myKeys;
    downloadKeys(write, keys, myProgress.spawnAll(), session);
    myActualIssueIds.set(issueIds);
    myLastIcn = write.getLastICN();
  }

  public static void downloadKeys(DBIssueWrite issueWrite, List<String> keys, ProgressInfo wholeProgress, RestSession session) throws ConnectorException {
    try {
      CustomFieldsSchema schema = CustomFieldsSchema.loadFromDB(issueWrite.getSyncManager(), issueWrite.getMetaConfig().getFieldKinds(), issueWrite.getConnection());
      RestIssueProcessor loadIssues = new RestIssueProcessor(schema, issueWrite);
      ProgressInfo[] progressInfos = wholeProgress.spawn(0.9).split(keys.size());
      for (int i = 0, keysSize = keys.size(); i < keysSize; i++) {
        ProgressInfo progress = progressInfos[i];
        progress.startActivity(RestIssueProcessor.PROGRESS_LOAD_NEXT.formatMessage(String.valueOf(i), String.valueOf(keysSize)));
        String key = keys.get(i);
        String path = "api/2/issue/" + key;
        RestResponse response = session.restGet(path, RequestPolicy.SAFE_TO_RETRY);
        progress.setDone();
        int code = response.getStatusCode();
        if (code == 404) {
          loadIssues.getCurrentTransaction().addBagScalar(ServerIssue.TYPE, ServerIssue.KEY, key).delete();
          continue;
        }
        try {
          response.parseJSON(loadIssues.toHandler());
        } catch (Exception e) {
          LogHelper.warning(e);
        }
      }
      loadIssues.finishTransaction();
    } finally {
      wholeProgress.setDone();
    }
  }

  public IntList getActualIds() {
    return myActualIssueIds.get();
  }
}
