package com.almworks.jira.provider3.app.sync;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.SyncParameter;
import com.almworks.api.engine.SyncParameters;
import com.almworks.api.engine.SyncType;
import com.almworks.api.engine.util.SyncNotAllowedException;
import com.almworks.jira.connector2.JiraException;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.sync.download2.meta.LoadRestMeta;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.spi.provider.AbstractConnectionSynchronizer;
import com.almworks.spi.provider.ConnectionNotConfiguredException;
import com.almworks.spi.provider.util.ServerSyncPoint;
import com.almworks.util.Pair;
import com.almworks.util.properties.Role;
import org.almworks.util.Const;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class JiraSynchronizer extends AbstractConnectionSynchronizer {
  public static final Role<JiraSynchronizer> ROLE = Role.role(JiraSynchronizer.class);

  private final JiraConnection3 myConnection;
  private final CustomFieldsComponent myCustomFields;

  public JiraSynchronizer(JiraConnection3 connection, ComponentContainer container, CustomFieldsComponent customFields) {
    super(container);
    myConnection = connection;
    myCustomFields = customFields;
  }

  @Override
  @Nullable
  protected Pair<Boolean, String> longSynchronize(SyncParameters parameters)
    throws ConnectionNotConfiguredException, ConnectorException, InterruptedException
  {
    if (!myConnection.getState().getValue().isReady()) {
      return null;
    }

    final Map<Long, SyncType> items = parameters.get(SyncParameter.EXACT_ITEMS);
    RemoteMetaConfig metaConfig = myCustomFields.createIssueConversion();
    if (items != null) {
      synchronizeArtifacts(items, metaConfig);
      return Pair.create(true, null);
    }

    ProgressInfo progress = new ProgressInfo(myProgress.createDelegate(), myCancelFlag);

    if (parameters.shouldLoadMeta()) {
      loadEnums(progress.spawn(0.8, "sync.meta"), metaConfig);
    }

    loadUpdatedIssues(parameters, metaConfig, progress.spawnAll());
    progress.setDone();
    return Pair.create(true, null);
  }

  private void loadUpdatedIssues(SyncParameters parameters, RemoteMetaConfig metaConfig, ProgressInfo progress) throws ConnectionNotConfiguredException, ConnectorException {
    ConnectionSyncInfo syncInfo = new ConnectionSyncInfo(myConnection.getSyncState(), myConnection.getProjectsFilter(), !parameters.shouldLoadMeta());
    RestDownloadUpdatedIssues operation = new RestDownloadUpdatedIssues(myConnection.getServerInfo(), syncInfo, progress, metaConfig);
    myConnection.getIntegration().synchronousDownload(operation);
    ServerSyncPoint syncPoint = operation.getNewSyncPoint();
    if (syncPoint != null && syncPoint.getSyncTime() > Const.DAY) myConnection.updateSyncState(syncPoint);
  }

  private void loadEnums(ProgressInfo progress, RemoteMetaConfig metaConfig) throws ConnectionNotConfiguredException, CancelledException, SyncNotAllowedException {
    myConnection.getIntegration().synchronousDownload(new LoadRestMeta(myConnection.getServerInfo(), progress, metaConfig));
  }

  private void synchronizeArtifacts(final Map<Long, SyncType> items, RemoteMetaConfig metaConfig) throws ConnectionNotConfiguredException, CancelledException, SyncNotAllowedException {
    DownloadDetails downloadDetails =
      DownloadDetails.prepare(myProgress.createDelegate("downloadDetails"), myConnection.getServerInfo(), myCancelFlag, myProblems, items, metaConfig);
    myConnection.getIntegration().synchronousDownload(downloadDetails);
  }

  public void longStart() {
    ServerSyncPoint lastSync = myConnection.getSyncState();
    if (!lastSync.isUnsynchronized()) {
      myState.commitValue(State.NEVER_HAPPENED, State.DONE);
    }
  }

  @Nullable
  public static ItemSyncProblem.Cause getCause(ConnectorException e) {
    JiraException.JiraCause cause = getJiraCause(e);
    return cause != null ? convertCause(cause) : null;
  }

  public static JiraException.JiraCause getJiraCause(ConnectorException e) {
    JiraException.JiraCause cause;
    if (e instanceof JiraException) {
      cause = ((JiraException) e).getJiraCause();
    } else
      return null;
    if (cause == null)
      return null;
    return cause;
  }

  private static ItemSyncProblem.Cause convertCause(JiraException.JiraCause cause) {
    switch (cause) {
      case ATTACHMENTS_UPLOAD:
        return ItemSyncProblem.Cause.GENERIC_UPLOAD_FAILURE;
      case COMPATIBILITY:
        return ItemSyncProblem.Cause.COMPATIBILITY;
      case CONFLICT:
        return ItemSyncProblem.Cause.UPLOAD_CONFLICT;
      case GENERIC_UPDATE:
        return ItemSyncProblem.Cause.GENERIC_UPLOAD_FAILURE;
      case ACCESS_DENIED:
        return ItemSyncProblem.Cause.ACCESS_DENIED;
      case MISSING_DATA:
        return ItemSyncProblem.Cause.ILLEGAL_DATA;
      case REPETITIVE_UPLOAD:
        return ItemSyncProblem.Cause.REPETITIVE_UPLOAD;
      case INVALID_DATA:
        return ItemSyncProblem.Cause.ILLEGAL_DATA;
      case ISSUE_NOT_FOUND:
        return ItemSyncProblem.Cause.REMOTE_NOT_FOUND;
      case MOVE_FAILED:
        return ItemSyncProblem.Cause.GENERIC_UPLOAD_FAILURE;
      default:
        assert false : cause;
      }
    return null;
  }
}