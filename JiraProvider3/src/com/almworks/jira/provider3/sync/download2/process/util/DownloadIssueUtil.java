package com.almworks.jira.provider3.sync.download2.process.util;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.links.structure.IssuesLinkTreeLayout;
import com.almworks.jira.provider3.remotedata.issue.ParsedIssueFields;
import com.almworks.jira.provider3.sync.ConnectorManager;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.details.LoadDetails;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class DownloadIssueUtil {
  private static final LocalizedAccessor.Value M_LOADING_EDIT_FIELDS = ConnectorManager.LOCAL.getFactory("progress.message.loadEditFields");

  public static EntityWriter prepareWrite(EntityTransaction transaction, DBDrain drain) {
    DBIdentity connection = ServerInfo.getConnection(transaction);
    return transaction.prepareWrite(drain, ServerJira.NS, connection);
  }

  public static void downloadDetails(RestSession session, EntityTransaction transaction, ProgressInfo progress, int issueId, @NotNull RemoteMetaConfig metaConfig,
    JiraConnection3 connection) throws ConnectorException {
    EntityHolder issue = LoadDetails.loadAllDetails(transaction, session, progress.spawn(0.7), issueId, metaConfig.getFieldKinds(), connection);
    if (issue == null) return;
    loadEditFields(session, issue, progress.spawnAll());
    DownloadStageMark.FULL.setTo(issue);
    progress.setDone();
  }

  private static void loadEditFields(RestSession session, EntityHolder issue, ProgressInfo progress) throws CancelledException {
    try {
      Integer issueId = issue.getScalarValue(ServerIssue.ID);
      if (issueId == null) return;
      progress.startActivity(M_LOADING_EDIT_FIELDS.create());
      List<EntityHolder> editFields;
      try {
        RestResponse response = ParsedIssueFields.restGetEditMeta(session, issueId);
        if (!response.isSuccessful()) {
          int errorCode = response.getStatusCode();
          if (errorCode == 404) editFields = Collections.emptyList();
          else {
            LogHelper.debug("Failed to load fields for edit", errorCode, issueId);
            return;
          }
        } else {
          ParsedIssueFields issueFields = ParsedIssueFields.parseEditMeta(response);
          editFields = issueFields.getAllFields(issue.getTransaction());
        }
      } catch (ConnectorException e) {
        progress.addError(e.getLongDescription());
        Log.debug("cannot download custom fields for " + issue + ": " + e);
        return;
      }
      issue.setReferenceCollection(ServerIssue.FIELDS_FOR_EDIT, editFields);
    } finally {
      progress.setDone();
    }
  }

  public static void finishDownload(DBDrain drain, EntityTransaction transaction, RemoteMetaConfig metaConfig) {
    DBIdentity connection = ServerInfo.getConnection(transaction);
    metaConfig.updateMetaInfo(drain, connection);
    IssuesLinkTreeLayout.update(drain, connection);
  }
}
