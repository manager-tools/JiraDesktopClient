package com.almworks.jira.provider3.app.sync;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.engine.util.SyncNotAllowedException;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.spi.provider.AbstractConnectorTask;
import com.almworks.spi.provider.ConnectionNotConfiguredException;
import com.almworks.util.commons.Procedure;
import com.almworks.util.progress.Progress;
import org.jetbrains.annotations.Nullable;

public abstract class JiraIssueSyncTask extends AbstractConnectorTask {
  private final JiraConnection3 myConnection;

  public JiraIssueSyncTask(String queryName, JiraConnection3 connection, Procedure<SyncTask> runFinally) {
    super(connection.getContainer(), queryName, runFinally);
    myConnection = connection;
  }

  @Override
  protected long doLongLoad(Progress progress) throws ConnectorException, ConnectionNotConfiguredException, SyncNotAllowedException {
    DBConnectorOperation operation = createOperation(progress);
    myConnection.getIntegration().synchronousDownload(operation);
    return operation.getLastIcn();
  }

  public JiraConnection3 getConnection() {
    return myConnection;
  }

  @Override
  public SpecificItemActivity getSpecificActivityForItem(long itemId, @Nullable Integer serverId) {
    return SpecificItemActivity.OTHER;
  }

  protected abstract DBConnectorOperation createOperation(Progress progress);
}
