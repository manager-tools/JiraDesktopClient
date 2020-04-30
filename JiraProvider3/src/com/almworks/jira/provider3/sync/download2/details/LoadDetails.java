package com.almworks.jira.provider3.sync.download2.details;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.CannotParseException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.jira.connector2.JiraCredentialsRequiredException;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.jira.provider3.sync.ConnectorManager;
import com.almworks.jira.provider3.sync.download2.details.fields.AttachmentsField;
import com.almworks.jira.provider3.sync.download2.details.fields.JiraIssueJsonFields;
import com.almworks.jira.provider3.sync.download2.details.fields.WorklogsField;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.jira.provider3.workflow.LoadTransitions;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.sax.*;
import com.almworks.util.LogHelper;
import com.almworks.util.Trio;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Map;

public class LoadDetails {
  private static final LocalizedAccessor.Value M_LOADING_VOTERS = ConnectorManager.LOCAL.getFactory("progress.message.loadVoters");
  private static final LocalizedAccessor.Value M_LOADING_WATCHERS = ConnectorManager.LOCAL.getFactory("progress.message.loadWatchers");
  private final EntityTransaction myTransaction;
  private final CustomFieldsSchema.RestLoader mySchemaLoader;
  /**
   * Holds reference to loaded issue
   */
  private EntityHolder myLoadedIssue;

  public LoadDetails(EntityTransaction transaction, CustomFieldsSchema.RestLoader schemaLoader) {
    myTransaction = transaction;
    mySchemaLoader = schemaLoader;
  }

  /**
   * Loads issue details and returns loaded issue and deleted flag
   * @return entity - the loaded issue. If the issue is deleted returns null
   */
  @Nullable
  public static EntityHolder loadDetails(EntityTransaction transaction, RestSession session, ProgressInfo progress, int issueId, Map<String, FieldKind> customFields,
    String connectionId) throws ConnectorException {
    try {
      LoadDetails loader = new LoadDetails(transaction, new CustomFieldsSchema.RestLoader(customFields, connectionId));
      String path = "api/2/issue/" + issueId + "?expand=names%2Cschema";
      progress.checkCancelled();
      RestResponse response = session.restGet(path, RequestPolicy.SAFE_TO_RETRY);
      progress.spawn(0.8).setDone();
      int code = response.getStatusCode();
      if (code == 404 || code == 403) { // Issue is delete (404) or not visible (403) due to security restrictions
        loader.myTransaction.addBagScalar(ServerIssue.TYPE, ServerIssue.ID, issueId).delete();
        return null;
      }
      if (code == 401) {
        RestResponse.ErrorResponse error = response.createErrorResponse();
        throw new JiraCredentialsRequiredException(error.toException(), error.getFullMessage());
      }
      response.ensureSuccessful();
      response.parseJSON(loader.createHandler(String.valueOf(issueId)));
      EntityHolder issue = loader.myLoadedIssue;
      if (issue == null) throw new JiraInternalException("Failed to load issue " + issueId);
      maybeLoadAdditional(issue, session, progress);
      return issue;
    } finally {
      progress.setDone();
    }
  }

  private static void maybeLoadAdditional(EntityHolder issue, RestSession session, ProgressInfo progress) throws ConnectorException {
    WorklogsField.INSTANCE.maybeLoadAdditional(issue, session, progress.spawn(0.5));
    AttachmentsField.INSTANCE.maybeLoadAdditional(issue, session, progress.spawnAll());
  }

  public static EntityHolder loadAllDetails(EntityTransaction transaction, RestSession session, ProgressInfo progress, int issueId,
    Map<String, FieldKind> customFields, JiraConnection3 connection)
    throws ConnectorException
  {
    try {
      EntityHolder issue = loadDetails(transaction, session, progress.spawn(0.2), issueId, customFields, connection.getConnectionID());
      if (issue == null) return null;
      connection.getActor(IssuePermissions.ROLE).loadIssuePermissions(transaction, session, issueId);
      new LoadTransitions(session, issue, connection.getCustomFields()).perform(progress.spawn(0.2));
      loadWatchersVotes(transaction, session, progress.spawnAll(), issueId);
      return issue;
    } finally {
      progress.setDone();
    }
  }

  @Nullable("When server returned failure status code")
  private static Trio<Boolean, Integer, ServerUser.CollectFromJson> loadUsers(final EntityTransaction transaction, RestSession session, ProgressInfo progress, String path,
    String isKey, String countKey,
    String usersKey) throws ConnectorException {
    RestResponse response = session.restGet(path, RequestPolicy.SAFE_TO_RETRY);
    if (!response.isSuccessful()) {
      int statusCode = response.getStatusCode();
      if (statusCode != 404) { // 404 means issue is not accessible
        LogHelper.error("Unexpected status code", statusCode);
        progress.addHttpStatusError(response.getLastUrl(), statusCode, response.getStatusText());
      }
      return null;
    }
    ServerUser.CollectFromJson users = new ServerUser.CollectFromJson(transaction);
    if (response.getHttpResponse().getContentLength() == 0) { // Vote/watching is disabled on JIRA
      return Trio.create(false, null, users);
    }
    LocationHandler parseUsers = PeekArrayElement.entryArray(usersKey, JSONCollector.objectConsumer(users));
    JSONCollector isWatching = new JSONCollector(null);
    JSONCollector count = new JSONCollector(null);
    try {
      response.parseJSON(new CompositeHandler(parseUsers, PeekObjectEntry.objectEntry(isKey, isWatching), PeekObjectEntry.objectEntry(countKey, count)));
    } catch (CannotParseException e) {
      LogHelper.warning("Failed to parse", path, e.getMessage());
      return null;
    }
    return Trio.create(
      isWatching.cast(Boolean.class),
      count.getInteger(),
      users
    );
  }

  private static void loadWatchersVotes(final EntityTransaction transaction, RestSession session, ProgressInfo progress, int issueId) throws ConnectorException {
    EntityHolder issue = ServerIssue.create(transaction, issueId, null);
    if (issue == null) return;
    progress.startActivity(M_LOADING_WATCHERS.create());
    Trio<Boolean, Integer,ServerUser.CollectFromJson> trio = loadUsers(transaction, session, progress.spawn(0.5), "api/2/issue/" + issueId + "/watchers", "isWatching", "watchCount", "watchers");
    if (trio != null) {
      issue.setReferenceCollection(ServerIssue.WATCHERS, trio.getThird().getUsers());
      issue.setNNValue(ServerIssue.WATCHERS_COUNT, trio.getSecond());
//      issue.setNNValue(ServerIssue.WATCHING, trio.getFirst());  // set this via general issue download (to avoid different values in the transaction)
    }
    progress.startActivity(M_LOADING_VOTERS.create());
    trio = loadUsers(transaction, session, progress.spawnAll(), "api/2/issue/" + issueId + "/votes", "hasVoted", "votes", "voters");
    if (trio != null) {
      issue.setReferenceCollection(ServerIssue.VOTERS, trio.getThird().getUsers());
      issue.setNNValue(ServerIssue.VOTES_COUNT, trio.getSecond());
//      issue.setNNValue(ServerIssue.VOTED, trio.getFirst());  // set this via general issue download (to avoid different values in the transaction)
    } else {
      issue.setReferenceCollection(ServerIssue.VOTERS, null);
    }
    progress.setDone();
  }

  CompositeHandler createHandler(String debugName) {
    return MyIssueHandler.createHandler(this, debugName, mySchemaLoader);
  }

  private static class MyIssueHandler implements LocationHandler {
    private final LoadDetails myLoader;
    private final String mDebugInfo;
    private final JSONCollector myGetId = new JSONCollector(null);
    private final JSONCollector myGetKey = new JSONCollector(null);
    private final JSONCollector myGetFields = new JSONCollector(null);
    private final CustomFieldsSchema.RestLoader mySchemaLoader;

    public MyIssueHandler(LoadDetails loader, String debugInfo, CustomFieldsSchema.RestLoader schemaLoader) {
      myLoader = loader;
      mDebugInfo = debugInfo;
      mySchemaLoader = schemaLoader;
    }

    private static CompositeHandler createHandler(LoadDetails loader, String debugName, CustomFieldsSchema.RestLoader schemaLoader) {
      MyIssueHandler handler = new MyIssueHandler(loader, debugName, schemaLoader);
      return new CompositeHandler(handler.myGetId.peekObjectEntry("id"),
        handler.myGetKey.peekObjectEntry("key"),
        handler.myGetFields.peekObjectEntry("fields"),
        PeekObjectEntry.objectEntry("schema", schemaLoader.getSchemaHandler()),
        PeekObjectEntry.objectEntry("names", schemaLoader.getNamesHandler()),
        handler);
    }

    @Override
    public void visit(Location what, boolean start, @Nullable String k, @Nullable Object v) throws ParseException, IOException {
      if (!start && what == Location.TOP) {
        Integer id = myGetId.getInteger();
        String key = myGetKey.getString();
        JSONObject fields = myGetFields.getJsonObject();
        if (id == null || key == null || fields == null) {
          LogHelper.error("Missing data", mDebugInfo, id, key, fields != null);
          throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION);
        }
        EntityHolder issue = myLoader.myTransaction.addEntity(ServerIssue.TYPE, ServerIssue.ID, id);
        if (issue == null) {
          LogHelper.error("Failed to create issue", id, key);
          return;
        }
        LogHelper.assertError(myLoader.myLoadedIssue == null, "Already has loaded issue", myLoader.myLoadedIssue);
        myLoader.myLoadedIssue = issue;
        DownloadStageMark.FULL.setTo(issue);
        issue.setValue(ServerIssue.KEY, key);
        CustomFieldsSchema schema = mySchemaLoader.createSchema();
        schema.writeSchema(myLoader.myTransaction);
        JiraIssueJsonFields.loadIssue(issue, fields, schema);
      }
    }
  }
}
