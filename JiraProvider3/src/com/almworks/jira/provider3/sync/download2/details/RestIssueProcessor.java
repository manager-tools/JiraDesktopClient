package com.almworks.jira.provider3.sync.download2.details;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.jira.provider3.sync.ConnectorManager;
import com.almworks.jira.provider3.sync.download2.details.fields.JiraIssueJsonFields;
import com.almworks.jira.provider3.sync.download2.process.DBIssueWrite;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.JRIssue;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.jql.JqlQuery;
import com.almworks.restconnector.json.sax.JSONCollector;
import com.almworks.restconnector.json.sax.LocationHandler;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

public class RestIssueProcessor implements Procedure<JSONObject> {
  private static final LocalizedAccessor.Value PROGRESS_LOAD_FIRST = ConnectorManager.LOCAL.getFactory("loadQuery.progress.load.first");
  public static final LocalizedAccessor.Message2 PROGRESS_LOAD_NEXT = ConnectorManager.LOCAL.message2("loadQuery.progress.load.next");

  private static final int MAX_ISSUES = 100;

  private final CustomFieldsSchema myCustomSchema;
  private final DBIssueWrite myWrite;
  private EntityTransaction myCurrentTransaction = null;
  private int myIssueCount = 0;

  public RestIssueProcessor(CustomFieldsSchema customSchema, DBIssueWrite write) {
    myCustomSchema = customSchema;
    myWrite = write;
  }

  @Override
  public void invoke(JSONObject issue) {
    Integer id = JRIssue.ID.getValue(issue);
    String key = JRIssue.KEY.getValue(issue);
    if (id == null || key == null) {
      LogHelper.warning("Missing issue identity", issue);
      return;
    }
    EntityTransaction transaction = getCurrentTransaction();
    myCustomSchema.writeSchema(transaction);
    EntityHolder holder = transaction.addEntity(ServerIssue.TYPE, ServerIssue.ID, id);
    if (holder == null) {
      LogHelper.error("Failed to store issue", id);
      return;
    }
    holder.setValue(ServerIssue.KEY, key);
    DownloadStageMark.QUICK.setTo(holder);
    JiraIssueJsonFields.loadIssue(holder, JRIssue.FIELDS.getValue(issue), myCustomSchema);
    myIssueCount++;
    if (myIssueCount >= MAX_ISSUES)
      try {
        finishTransaction();
      } catch (ConnectorException e) {
        LogHelper.error("Should not happen");  // todo JCO-1390
      }
  }

  /**
   * @return number of processed issues
   */
  public int getIssueCount() {
    return myIssueCount;
  }

  @NotNull
  public EntityTransaction getCurrentTransaction() {
    if (myCurrentTransaction == null) {
      myCurrentTransaction = myWrite.createTransaction();
      myIssueCount = 0;
    }
    return myCurrentTransaction;
  }

  public void finishTransaction() throws ConnectorException {
    if (myCurrentTransaction == null) return;
    myWrite.writeTransaction(myCurrentTransaction);
    myCurrentTransaction = null;
  }

  /**
   * Loads all issues satisfying jql query. Stores then into DB via issueWrite. Also shows progress via progress.
   * @param jql query
   * @param progress notify progress to
   * @throws ConnectorException
   */
  public static RestIssueProcessor loadQuery(RestSession session, @NotNull JqlQuery jql, DBIssueWrite issueWrite, ProgressInfo progress) throws ConnectorException {
    RestQueryPager pager = RestQueryPager.allFields(jql);
    return loadQuery(session, issueWrite, progress, pager);
  }

  /**
   * Same as {@link #loadQuery(com.almworks.restconnector.RestSession, JqlQuery, com.almworks.jira.provider3.sync.download2.process.DBIssueWrite, com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo)}
   * but allows to pre-configure pager
   * @param pager jql query with additional configuration
   */
  public static RestIssueProcessor loadQuery(RestSession session, DBIssueWrite issueWrite, ProgressInfo progress, RestQueryPager pager) throws ConnectorException {
    CustomFieldsSchema schema = CustomFieldsSchema.loadFromDB(issueWrite.getSyncManager(), issueWrite.getMetaConfig().getFieldKinds(), issueWrite.getConnection());
    RestIssueProcessor loadIssues = new RestIssueProcessor(schema, issueWrite);
    pager.loadAll(session, loadIssues.toHandler(), progress, PROGRESS_LOAD_FIRST, PROGRESS_LOAD_NEXT);
    loadIssues.finishTransaction();
    return loadIssues;
  }

  public LocationHandler toHandler() {
    return JSONCollector.objectConsumer(this);
  }
}
