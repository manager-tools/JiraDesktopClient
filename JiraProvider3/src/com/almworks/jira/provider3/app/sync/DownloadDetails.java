package com.almworks.jira.provider3.app.sync;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.SyncProblem;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.engine.SyncType;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBPriority;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.sync.ConnectorManager;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.process.DBIssueWrite;
import com.almworks.jira.provider3.sync.download2.process.util.DownloadIssueUtil;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Trio;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.SetHolderModel;
import com.almworks.util.progress.Progress;
import org.almworks.util.Collections15;

import java.util.List;
import java.util.Map;

class DownloadDetails extends BaseOperation {
  private static final LocalizedAccessor.Value M_UPDATING_DB = ConnectorManager.LOCAL.getFactory("progress.message.updatingDB");

  private final SetHolderModel<SyncProblem> myProblems;
  private final List<Trio<Long, Integer, String>> myTask;
  private final ServerInfo myServerInfo;
  private final RemoteMetaConfig myMetaConfig;

  private DownloadDetails(Progress progress, ServerInfo serverInfo, ScalarModel<Boolean> cancelFlag,
    SetHolderModel<SyncProblem> problems, List<Trio<Long, Integer, String>> task, RemoteMetaConfig metaConfig)
  {
    super(new ProgressInfo(progress, cancelFlag));
    myProblems = problems;
    myTask = task;
    myServerInfo = serverInfo;
    myMetaConfig = metaConfig;
  }
  
  public static DownloadDetails prepare(Progress progress, ServerInfo serverInfo, ScalarModel<Boolean> cancelFlag,
    SetHolderModel<SyncProblem> problems, final Map<Long, SyncType> items, RemoteMetaConfig metaConfig) {
    SyncManager syncManager = serverInfo.getSyncManager();
    List<Trio<Long, Integer, String>> task = syncManager.enquireRead(DBPriority.BACKGROUND, new ReadTransaction<List<Trio<Long, Integer, String>>>() {
      @Override
      public List<Trio<Long, Integer, String>> transaction(DBReader reader) throws DBOperationCancelledException {
        List<Trio<Long, Integer, String>> task = Collections15.arrayList();
        for (Map.Entry<Long, SyncType> entry : items.entrySet()) {
          Long item = entry.getKey();
          String key = Issue.KEY.getValue(item, reader);
          Integer id = Issue.ID.getValue(item, reader);
          task.add(Trio.create(item, id, key));
        }
        return task;
      }
    }).waitForCompletion();
    return new DownloadDetails(progress, serverInfo, cancelFlag, problems, task, metaConfig);
  }

  @Override
  public void perform(RestSession session) throws ConnectorException {
    myServerInfo.ensureHasData(session);
    ProgressInfo[] progresses = myProgress.split(myTask.size());
    int index = 0;
    for (Trio<Long, Integer, String> trio : myTask) {
      ProgressInfo progress = progresses[index];
      index++;
      long item = trio.getFirst();
      String key = trio.getThird();
      Integer id = trio.getSecond();
      progress.startActivity("Downloading details for " + key);
      try {
        downloadDetails(session, id, progress);
      } catch (ConnectorException e) {
        LogHelper.warning("Download details problem", e);
        myProblems.add(new ExceptionItemProblem(item, key, e, myServerInfo.getConnection(), JiraSynchronizer.getCause(e)));
      } finally {
        progress.setDone();
      }
    }
  }

  private void downloadDetails(RestSession session, int issueId, ProgressInfo progress) throws ConnectorException {
    EntityTransaction transaction = myServerInfo.createTransaction();
    DownloadIssueUtil.downloadDetails(session, transaction, progress.spawn(0.9), issueId, myMetaConfig, myServerInfo.getConnection());
    progress.startActivity(M_UPDATING_DB.create());
    DBIssueWrite.updateDB(transaction, myServerInfo, session, null, myMetaConfig);
  }

  @Override
  public void onCancelled() {
    myProgress.addError("Cancelled");
  }

  @Override
  public void onCompleted(SyncTask.State result) {
    myProgress.setDone();
  }

  @Override
  public void onError(ConnectorException e) {
    myProgress.addError(e.getMediumDescription());
    LogHelper.warning(e);
  }
}
