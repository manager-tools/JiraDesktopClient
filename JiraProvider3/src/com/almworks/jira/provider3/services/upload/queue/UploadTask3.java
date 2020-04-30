package com.almworks.jira.provider3.services.upload.queue;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.ProgressComponentWrapper;
import com.almworks.api.engine.util.SynchronizationProgress;
import com.almworks.integers.LongList;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.services.upload.JiraUploadComponent;
import com.almworks.jira.provider3.services.upload.JiraUploadFacade;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.spi.provider.AbstractSyncTask;
import com.almworks.util.LogHelper;
import com.almworks.util.threads.Threads;
import org.jetbrains.annotations.Nullable;

class UploadTask3 extends AbstractSyncTask {
  private volatile LongList myCurrentItems = null;
  private final UploadQueue myUploadQueue;
  private final ProblemsMaintenance myProblemsMaintenance;
  private ProgressComponentWrapper myProgressComponent;

  public UploadTask3(ComponentContainer container, UploadQueue uploadQueue) {
    super(container);
    myUploadQueue = uploadQueue;
    myProblemsMaintenance = new ProblemsMaintenance(myProblems, getConnection(), getManager());
  }


  @Override
  public ProgressComponentWrapper getProgressComponentWrapper() {
    Threads.assertAWTThread();
    if(myProgressComponent == null) {
      myProgressComponent = new SynchronizationProgress(myProgress, myState);
    }
    return myProgressComponent;
  }

  @Override
  public void listenConnectionState() {
    super.listenConnectionState();
  }

  @Override
  protected void executeTask() {
    LogHelper.error("Should not happen");
  }

  @Override
  public JiraConnection3 getConnection() {
    return (JiraConnection3) super.getConnection();
  }

  public void performUpload(LongList items) {
    myProblemsMaintenance.ensureStarted();
    detach();
    init();
    boolean success = false;
    try {
      myCurrentItems = items;
      myState.setValue(State.WORKING);
      doUpload(items);
      success = true;
    } finally {
      myCurrentItems = null;
      myProgress.setDone();
      myState.commitValue(State.WORKING, success ? State.DONE : State.FAILED);
    }
  }

  private void doUpload(LongList items) {
    if (items == null || items.isEmpty()) return;
//    IssueUploadProcess upload;
    State result;
    try {
      JiraConnection3 connection = getConnection();
      JiraUploadComponent uploadComponent = connection.getActor(JiraUploadComponent.ROLE);
      JiraUploadFacade upload = uploadComponent.startUpload(new ProgressInfo(myProgress, myCancelFlag), myProblems, myUploadQueue, getConnection(), items);
      result = upload.getResult();
//      RemoteMetaConfig remoteMeta = connection.getCustomFields().createIssueConversion();
//      upload = new IssueUploadProcess(items, getIntegration(), new UploadDescriptor(IssueState.createIssueLoader(remoteMeta.getUploadLoader())),
//        myCancelFlag, myProblems, myUploadQueue, new Progress(), remoteMeta);
//      getManager().syncUpload(upload);
    } catch (InterruptedException e) {
      myState.setValue(State.CANCELLED);
      return;
    } finally {
      myProgress.setDone();
    }
//    result = upload.getResult();
    if (result == null || result == State.NEVER_HAPPENED) {
      LogHelper.error("Missing result", result);
      result = State.FAILED;
    }
    myState.setValue(result);
  }

  @Override
  public String getTaskName() {
    return "Issue upload";
  }

  @Override
  public SpecificItemActivity getSpecificActivityForItem(long itemId, @Nullable Integer serverId) {
    LongList items = myCurrentItems;
    return items != null && items.contains(itemId) ? SpecificItemActivity.UPLOAD : SpecificItemActivity.OTHER;
  }

  public SyncManager getManager() {
    return getActor(SyncManager.ROLE);
  }

  @Override
  public void checkCancelled() throws CancelledException {
    super.checkCancelled();
  }

  public void stop() {
    myProblemsMaintenance.stop();
  }
}
