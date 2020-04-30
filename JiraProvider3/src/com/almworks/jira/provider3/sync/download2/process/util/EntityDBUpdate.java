package com.almworks.jira.provider3.sync.download2.process.util;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBResult;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class EntityDBUpdate implements DownloadProcedure<DBDrain> {
  private final EntityTransaction myEntities;
  private final RemoteMetaConfig myMetaConfig;
  private Collection<EntityHolder> myProblems = null;

  public EntityDBUpdate(EntityTransaction entities, RemoteMetaConfig metaConfig) {
    myEntities = entities;
    myMetaConfig = metaConfig;
    LogHelper.assertError(ServerInfo.getConnection(entities) != null);
  }

  public RemoteMetaConfig getMetaConfig() {
    return myMetaConfig;
  }

  @Override
  public void write(DBDrain drain) throws DBOperationCancelledException {
    EntityWriter writer = DownloadIssueUtil.prepareWrite(myEntities, drain);
    beforeResolve(writer);
    Collection<EntityHolder> problems = writer.getUncreatable();
    if (problems.isEmpty()) writer.write();
    else {
      myProblems = problems;
      throw new DBOperationCancelledException();
    }
    DownloadIssueUtil.finishDownload(drain, myEntities, myMetaConfig);
  }

  protected void beforeResolve(EntityWriter writer) {
  }


  @Nullable
  public Collection<EntityHolder> getProblems() {
    return myProblems;
  }

  @Override
  public void onFinished(DBResult<?> result) {
  }
}
