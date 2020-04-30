package com.almworks.jira.provider3.issue.features.edit.screens;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.ConnectionException;
import com.almworks.api.http.HttpUtils;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.jira.provider3.sync.download2.meta.LoadRestMeta;
import com.almworks.jira.provider3.sync.download2.meta.core.LoadMetaContext;
import com.almworks.jira.provider3.sync.download2.meta.core.MetaOperation;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.Set;

public class LoadScreensOperation extends MetaOperation {

  public LoadScreensOperation() {
    super(1);
  }

  @Override
  public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException, JiraInternalException {
    progress.startActivity(EditIssueFeature.I18N.getString("edit.screens.loadScreens.activity.name"));
    JSONObject screens = loadScreens(session, context);
    if (screens == null) {
      LogHelper.warning("No screen info loaded");
      return;
    }
    progress.spawn(0.9).setDone();
    JiraScreens.updateScreens(transaction, screens);
    progress.setDone();
  }

  @Nullable("When no JCPL or failure")
  private static JSONObject loadScreens(RestSession session, LoadMetaContext context) {
    if (session.getCredentials().isAnonymous()) return null; // Create/edit are possible to logged-in users only
    Set<Integer> projectFilter = context.getDataOrNull(LoadRestMeta.PROJECT_FILTER);
    StringBuilder uri = new StringBuilder("jiraclient/1.0/screenConfig");
    if (projectFilter != null) HttpUtils.addGetParametersToString(uri, "project", projectFilter);
    RestResponse response;
    try {
      response = session.restGet(uri.toString(), RequestPolicy.SAFE_TO_RETRY);
    } catch (ConnectorException e) {
      LogHelper.warning(e, "Failed to load screens");
      return null;
    }
    int code = response.getStatusCode();
    switch (code) {
    case 200: break;
    case 404: return null;
    case 400:
      LogHelper.error("Wrong project filter?", projectFilter); // Generally should not happen (may happen if server permissions changed and user is not allowed to browse configured projects)
      return null;
    default:
      LogHelper.error("Load screens: Unexpected status code", code, projectFilter);
      return null;
    }
    try {
      return response.getJSONObject();
    } catch (ConnectionException e) {
      LogHelper.warning("Load screen failed", e);
      return null;
    } catch (ParseException e) {
      LogHelper.error("Failed to parse screens", e);
      return null;
    }
  }
}
