package com.almworks.jira.provider3.sync.download2.process;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.Connection;
import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBResult;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.attachments.upload.PrepareAttachmentsUpload;
import com.almworks.jira.provider3.comments.PrepareCommentUpload;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.remotedata.issue.edit.PrepareIssueUpload;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.meta.ResolutionProblems;
import com.almworks.jira.provider3.sync.download2.process.util.DownloadIssueUtil;
import com.almworks.jira.provider3.sync.download2.process.util.EntityDBUpdate;
import com.almworks.jira.provider3.sync.schema.*;
import com.almworks.jira.provider3.worklogs.PrepareWorklogsUpload;
import com.almworks.restconnector.RestSession;
import com.almworks.spi.provider.util.ServerSyncPoint;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.events.FireEventSupport;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DBIssueWrite {
  private final ServerInfo myServerInfo;
  private final SyncPointHolder mySyncPoint = new SyncPointHolder();
  private long myLastICN = 0;
  private final RestSession mySession;
  private final RemoteMetaConfig myConversion;
  @SuppressWarnings("unchecked")
  private final FireEventSupport<Procedure<EntityTransaction>> myPostTransaction = (FireEventSupport)FireEventSupport.create(Procedure.class);

  public DBIssueWrite(RestSession session, ServerInfo serverInfo, RemoteMetaConfig conversion) {
    myServerInfo = serverInfo;
    mySession = session;
    myConversion = conversion;
  }

  public RemoteMetaConfig getMetaConfig() {
    return myConversion;
  }

  @NotNull
  public SyncManager getSyncManager() {
    return myServerInfo.getSyncManager();
  }

  public Connection getConnection() {
    return myServerInfo.getConnection();
  }

  /**
   * Allows to execute a procedure for each processed transaction.<br>
   * The passed transaction is already written to DB.<br>
   * The procedure should not modify transaction, since it may be passes to one more procedure.
   */
  public void addPostTransaction(Procedure<EntityTransaction> procedure) {
    myPostTransaction.addStraightListener(Lifespan.FOREVER, procedure);
  }

  public void writeTransaction(EntityTransaction transaction) throws ConnectorException {
    DBResult<?> result = updateDB(transaction, myServerInfo, mySession, mySyncPoint, myConversion);
    long icn = result.isSuccessful() ? result.getCommitIcn() : 0;
    if (icn > 0) {
      LogHelper.assertError(myLastICN < icn, myLastICN, icn);
      myLastICN = icn;
      myPostTransaction.getDispatcher().invoke(transaction);
      List<EntityHolder> issues = transaction.getAllEntities(ServerIssue.TYPE);
      mySyncPoint.processIssues(issues);
    } else LogHelper.error("DB update failed", result.getErrors());
  }

  public EntityTransaction createTransaction() {
    return myServerInfo.createTransaction();
  }

  public static DBResult<?> updateDB(EntityTransaction transaction, ServerInfo serverInfo, RestSession session, @Nullable SyncPointHolder syncPoint, RemoteMetaConfig metaConfig) throws ConnectorException {
    EntityDBUpdate update = new MyDBUpdate(transaction, syncPoint, metaConfig);
    DBResult<?> result = serverInfo.getSyncManager().writeDownloaded(update);
    result.waitForCompletion();
    Collection<EntityHolder> problems = update.getProblems();
    if (problems != null) {
      ResolutionProblems resolutionProblems = new ResolutionProblems(session, serverInfo, metaConfig);
      resolutionProblems.addAll(problems);
      resolutionProblems.resolve();
      FinalWrite procedure = new FinalWrite(transaction, metaConfig);
      result = serverInfo.getSyncManager().writeDownloaded(procedure);
      result.waitForCompletion();
      return result;

    }
    return result;
  }

  @Nullable
  public synchronized ServerSyncPoint getLatestUpdate() {
    return mySyncPoint.getServerSyncPoint();
  }

  public long getLastICN() {
    return myLastICN;
  }

  static class MyDBUpdate extends EntityDBUpdate {
    @Nullable
    private final SyncPointHolder mySyncPoint;


    public MyDBUpdate(EntityTransaction transaction, SyncPointHolder syncPoint, RemoteMetaConfig metaConfig) {
      super(transaction, metaConfig);
      mySyncPoint = syncPoint;
    }

    @Override
    protected void beforeResolve(EntityWriter writer) {
      if (mySyncPoint != null) mySyncPoint.ensureLastCreatedRead(writer.getConnection());
      beforeWrite(writer);
    }
  }

  public static void beforeWrite(EntityWriter writer) {
    writer.ensureResolved();
    PrepareWorklogsUpload.findFailedUploads(writer);
    PrepareAttachmentsUpload.findFailedUploads(writer);
    PrepareCommentUpload.findFailedUploads(writer);
    PrepareIssueUpload.findFailedUploads(writer);

    DBReader reader = writer.getReader();
    LongArray fields = CustomField.queryKnownKey(writer.getConnection());
    Set<DBAttribute<?>> custom = Collections15.<DBAttribute<?>>hashSet(CustomField.ATTRIBUTE2.collectValues(reader, fields));
    custom.remove(null);
    if (custom.isEmpty()) return;
    for (EntityHolder issue : DownloadStageMark.filterOutDummy(writer.getAllEntities(ServerIssue.TYPE))) writer.clearNoValue(issue, custom);
  }

  private static class FinalWrite implements DownloadProcedure<DBDrain> {
    private final EntityTransaction myTransaction;
    private final RemoteMetaConfig myMetaConfig;

    public FinalWrite(EntityTransaction transaction, RemoteMetaConfig metaConfig) {
      myTransaction = transaction;
      myMetaConfig = metaConfig;
    }

    @Override
    public void write(DBDrain drain) throws DBOperationCancelledException {
      EntityWriter writer = DownloadIssueUtil.prepareWrite(myTransaction, drain);
      Collection<EntityHolder> problems = writer.getUncreatable();
      for (EntityHolder problem : problems) {
        Entity type = problem.getItemType();
        if (ServerProject.TYPE.equals(type)) {
          problem.allowSearchCreate(writer);
          LogHelper.warning("About to create fake project", problem.getScalarValue(ServerProject.KEY));
        } else if (ServerVersion.TYPE.equals(type)) {
          problem.allowSearchCreate(writer);
          LogHelper.warning("About to create fake version", problem.getScalarValue(ServerVersion.NAME));
        } else if (ServerComponent.TYPE.equals(type)) {
          problem.allowSearchCreate(writer);
          LogHelper.warning("About to create fake component", problem.getScalarValue(ServerComponent.NAME));
        } else if (ServerProjectRole.TYPE.equals(type)) {
          problem.allowSearchCreate(writer);
          LogHelper.warning("About to create fake project role", problem.getScalarValue(ServerProjectRole.NAME));
        } else LogHelper.error("Unknown problem type", type, problem);
      }
      problems = writer.getUncreatable();
      if (!problems.isEmpty()) {
        LogHelper.error("Still has problems:", problems);
        throw new DBOperationCancelledException();
      }
      writer.write();
      DownloadIssueUtil.finishDownload(drain, myTransaction, myMetaConfig);
    }

    @Override
    public void onFinished(DBResult<?> result) {
    }
  }
}
