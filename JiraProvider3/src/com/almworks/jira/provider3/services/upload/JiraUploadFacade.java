package com.almworks.jira.provider3.services.upload;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.SyncProblem;
import com.almworks.api.engine.SyncTask;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBItemType;
import com.almworks.items.sync.ItemUploader;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.UploadProcess;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.app.sync.BaseOperation;
import com.almworks.jira.provider3.services.upload.queue.UploadQueue;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.restconnector.RestSession;
import com.almworks.spi.provider.ConnectionNotConfiguredException;
import com.almworks.util.LogHelper;
import com.almworks.util.model.SetHolderModel;
import org.almworks.util.Collections15;

import java.util.List;
import java.util.Map;

public class JiraUploadFacade {
  private final ProgressInfo myProgress;
  private final SetHolderModel<SyncProblem> myProblems;
  private final UploadQueue myUploadQueue;
  private final SyncManager mySyncManager;
  private final JiraConnection3 myConnection;
  private volatile SyncTask.State myResult = SyncTask.State.NEVER_HAPPENED;

  JiraUploadFacade(ProgressInfo progress, SetHolderModel<SyncProblem> problems, UploadQueue uploadQueue, SyncManager syncManager, JiraConnection3 connection) {
    myProgress = progress;
    myProblems = problems;
    myUploadQueue = uploadQueue;
    mySyncManager = syncManager;
    myConnection = connection;
  }

  public SyncTask.State getResult() {
    return myResult;
  }

  public void start(final LongList items, final Map<DBItemType, UploadUnit.Factory> factories) throws InterruptedException {
    myResult = SyncTask.State.WORKING;
    mySyncManager.syncUpload(new ItemUploader() {
      private final List<UploadUnit> myUnits = Collections15.arrayList();
      private UploadContextImpl myConfig;

      @Override
      public void prepare(UploadPrepare prepare) {
        boolean success = false;
        try {
          myConfig = UploadContextImpl.prepare(prepare.getTrunk().getReader(), factories, myConnection, myProblems, items);
          LongList collected = new CollectUploadContext(prepare, items, myConfig).perform();
          myUnits.addAll(new LoadUploadContext(prepare, myConfig).perform(collected));
          success = true;
        } finally {
          if (!success) myResult = SyncTask.State.FAILED;
        }
      }

      @Override
      public void doUpload(UploadProcess process) throws InterruptedException {
        if (myResult == SyncTask.State.FAILED) return;
        boolean success = false;
        try {
          myConnection.getIntegration().synchronousUpload(new UploadOperation(JiraUploadFacade.this.myProgress.spawn(0.9), myUnits, myConfig, myUploadQueue, process));
          success = true;
        } catch (ConnectionNotConfiguredException e) {
          LogHelper.warning(e);
          myResult = SyncTask.State.FAILED;
        } finally {
          if (!success) myResult = SyncTask.State.FAILED;
        }
      }
    });
  }

  private static class UploadOperation extends BaseOperation {
    private final List<UploadUnit> myUnits;
    private final UploadContextImpl myConfig;
    private final UploadQueue myUploadQueue;
    private final UploadProcess myProcess;

    public UploadOperation(ProgressInfo progress, List<UploadUnit> units, UploadContextImpl config, UploadQueue uploadQueue, UploadProcess process) {
      super(progress);
      myUnits = units;
      myConfig = config;
      myUploadQueue = uploadQueue;
      myProcess = process;
    }

    @Override
    public void perform(RestSession session) throws ConnectorException {
      new JiraUploadContext(myProgress.spawn(0.7), session, myUnits, myConfig).perform();
      FinishUploadTransaction transaction = new PostUploadContext(myProgress.spawn(0.5), session, myUnits, myConfig).perform();
      myProcess.writeUploadState(transaction).waitForCompletion();
      myUploadQueue.retryConflicts(myConfig.getMandatoryConflicts());
      myProgress.setDone();
    }
  }
}
