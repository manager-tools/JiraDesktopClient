package com.almworks.jira.provider3.sync.jql.impl;

import com.almworks.api.constraint.Constraint;
import com.almworks.api.engine.QueryUrlInfo;
import com.almworks.api.http.HttpUtils;
import com.almworks.api.install.Setup;
import com.almworks.api.reduction.ConstraintTreeElement;
import com.almworks.api.reduction.ReductionUtil;
import com.almworks.api.reduction.Rule;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.schema.*;
import com.almworks.jira.provider3.sync.ConnectorManager;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.jira.provider3.sync.jql.JqlDate;
import com.almworks.jira.provider3.sync.jql.JqlEnum;
import com.almworks.jira.provider3.sync.jql.JqlSubStrings;
import com.almworks.restconnector.jql.JqlQuery;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ConvertingList;
import com.almworks.util.commons.Factory;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JqlQueryBuilder {
  private static final LocalizedAccessor.MessageStr EXCLUDED_ALL = ConnectorManager.LOCAL.messageStr("remoteQuery.exclude.all");
  private static final LocalizedAccessor.MessageStr EXCLUDED_RIGHT = ConnectorManager.LOCAL.messageStr("remoteQuery.exclude.right");
  private static final LocalizedAccessor.MessageStr DATE_WARNING = ConnectorManager.LOCAL.messageStr("remoteQuery.date");
  private static final LocalizedAccessor.Message2 TEXT_WARNING = ConnectorManager.LOCAL.message2("remoteQuery.text");
  private static final Factory<String> FALSE_QUERY = ConnectorManager.LOCAL.getFactory("remoteQuery.falseQuery");

  private final JiraConnection3 myConnection;
  private final DBReader myReader;
  private final ConnectionProperties myProperties;
  private final List<Pair<String, String>> myExcluded = Collections15.arrayList();
  private boolean mySearchableKnown = true;
  /**
   * (id, displayName) pairs
   */
  private final List<Pair<String, String>> myDates = Collections15.arrayList();
  private final List<Pair<String, String>> myTexts = Collections15.arrayList();

  private JqlQueryBuilder(JiraConnection3 connection, DBReader reader, ConnectionProperties properties) {
    myConnection = connection;
    myReader = reader;
    myProperties = properties;
  }

  private static JqlQueryBuilder create(JiraConnection3 connection, DBReader reader) {
    ConnectionProperties properties = ConnectionProperties.load(reader, connection.getConnectionItem());
    return new JqlQueryBuilder(connection, reader, properties);
  }

  public JiraConnection3 getConnection() {
    return myConnection;
  }

  public long getConnectionItem() {
    return myConnection.getConnectionItem();
  }

  @NotNull
  public List<ItemVersion> readItems(Collection<Long> items) {
    return BranchSource.trunk(myReader).readItems(LongArray.create(items));
  }

  public void addTextWarning(String fieldId, String fieldDisplayName) {
    myTexts.add(Pair.create(fieldId, fieldDisplayName));
  }

  public void addDateWarning(String fieldId, String fieldDisplayName) {
    addWarning(myDates, fieldId, fieldDisplayName);
  }

  private void addWarning(List<Pair<String, String>> target, String fieldId, String displayName) {
    if (fieldId == null) {
      LogHelper.error("Null id", displayName);
      return;
    }
    for (Pair<String, String> pair : target) if (fieldId.equals(pair.getFirst())) return;
    target.add(Pair.create(fieldId, displayName));
  }

  /**
   * Creates JQL query from given constraints
   * @return <b>not null</b> query. Empty string means "no constraint" (all issues)<br>
   *   <b>null</b> if constraint reduces to false - no issue can match the constraint
   */
  @Nullable
  private JqlQuery build(Constraint constraint) {
    Collection<JQLConvertor> convertors = collectConvertors();
    ArrayList<Rule> rules = Collections15.arrayList();
    rules.addAll(ReductionUtil.SIMPLIFY);
    rules.add(new JQLRule(convertors, this));
    ConstraintTreeElement cTree = ConstraintTreeElement.createTree(constraint);
    cTree = ReductionUtil.reduce(cTree, rules);
    List<String> searchable = myProperties.getSearchableCustomFields();
    ExcludeFieldsRule excludeCustom = new ExcludeFieldsRule(searchable);
    cTree = ReductionUtil.reduce(cTree, excludeCustom.createRules());
    myExcluded.addAll(excludeCustom.getExcluded());
    filter(myDates);
    filter(myTexts);
    mySearchableKnown = searchable != null;
    return new JqlQuery(cTree.createConstraint());
  }

  private void filter(List<Pair<String, String>> warnings) {
    if (warnings.isEmpty() || myExcluded.isEmpty()) return;
    Set<String> excludedFields = Collections15.hashSet(ConvertingList.create(myExcluded, Pair.<String>convertorGetFirst()));
    for (Iterator<Pair<String, String>> it = warnings.iterator(); it.hasNext(); ) {
      Pair<String, String> pair = it.next();
      if (excludedFields.contains(pair.getFirst())) it.remove();
    }
  }

  @Nullable
  public static JqlQuery buildJQL(Database db, final Constraint constraint, final JiraConnection3 connection) {
    return db.readBackground(reader -> JqlQueryBuilder.create(connection, reader).build(constraint)).waitForCompletion();
  }

  public static QueryUrlInfo buildQueryInfo(DBReader reader, Constraint constraint, JiraConnection3 connection) {
    JqlQueryBuilder context = JqlQueryBuilder.create(connection, reader);
    JqlQuery jql = context.build(constraint);
    if (jql == null) {
      QueryUrlInfo info = new QueryUrlInfo();
      info.setValid(false);
      info.setFatalProblem(FALSE_QUERY.create());
      return info;
    }
    String url = connection.getConfigHolder().getBaseUrl() + "/secure/IssueNavigator!executeAdvanced.jspa?jqlQuery=" + HttpUtils.encode(jql.getJqlText()) + "&runQuery=true&clear=true";
    QueryUrlInfo info = new QueryUrlInfo(url);
    info.setWarning(context.createWarningHtml());
    return info;
  }

  /**
   * @return null for no warning, otherwise adds all warnings
   */
  @Nullable
  private String createWarningHtml() {
    StringBuilder builder = new StringBuilder();
    if (!myExcluded.isEmpty()) append(builder, (mySearchableKnown ? EXCLUDED_RIGHT : EXCLUDED_ALL).formatMessage(listFields(myExcluded)));
    if (!myTexts.isEmpty()) append(builder, TEXT_WARNING.formatMessage(listFields(myTexts), Setup.getProductName()));
    if (!myDates.isEmpty()) { // Append date warning iff server time zone has different offset than local one (https://jira.almworks.com/browse/JCO-1308)
      TimeZone serverTimeZone = myProperties.getTimeZone();
      TimeZone localTimeZone = TimeZone.getDefault();
      long now = System.currentTimeMillis();
      if (serverTimeZone == null || (localTimeZone != null && serverTimeZone.getOffset(now) != localTimeZone.getOffset(now)))
        append(builder, DATE_WARNING.formatMessage(listFields(myDates)));
    }
    return builder.length() == 0 ? null : builder.toString();
  }

  private StringBuilder append(StringBuilder builder, String message) {
    if (builder.length() > 0) builder.append("<br>");
    return builder.append(message);
  }

  private String listFields(List<Pair<String, String>> idNameList) {
    StringBuilder builder = new StringBuilder();
    String sep = "";
    for (Pair<String, String> pair : idNameList) {
      String name = pair.getSecond();
      if (name == null || name.isEmpty()) name = pair.getFirst();
      if (name.contains(" ")) name = "'" + name + "'";
      builder.append(sep).append(name);
      sep = ", ";
    }
    return builder.toString();
  }




  private Collection<JQLConvertor> collectConvertors() {
    ArrayList<JQLConvertor> convertors = Collections15.arrayList();
    convertors.add(JQLConvertor.CONNECTION);
    convertors.add(JQLConvertor.TEXT);
    convertors.add(KeyJqlConvertor.INSTANCE);
    convertors.add(new JqlSubStrings("summary", Issue.SUMMARY, "Summary"));
    convertors.add(new JqlSubStrings("description", Issue.DESCRIPTION, "Description"));
    convertors.add(new JqlSubStrings("environment", Issue.ENVIRONMENT, "Environment"));
    convertors.add(new JqlSubStrings("comment", Comment.TEXT, "Comments"));
    convertors.add(JqlEnum.generic("project", Issue.PROJECT, Project.ID, "Project"));
    convertors.add(JqlEnum.generic("issuetype", Issue.ISSUE_TYPE, IssueType.ID, "Issue Type"));
    convertors.add(JqlEnum.generic("status", Issue.STATUS, Status.ID, "Status"));
    convertors.add(JqlEnum.generic("priority", Issue.PRIORITY, Priority.ID, "Priority"));
    convertors.add(JqlEnum.generic("affectedVersion", Issue.AFFECT_VERSIONS, Version.ID, "Affects Version/s"));
    convertors.add(JqlEnum.generic("fixVersion", Issue.FIX_VERSIONS, Version.ID, "Fix Version/s"));
    convertors.add(JqlEnum.generic("component", Issue.COMPONENTS, Component.ID, "Component/s"));
    convertors.add(JqlEnum.generic("resolution", Issue.RESOLUTION, Resolution.ID, "-1", "Resolution"));
    convertors.add(JqlEnum.generic("reporter", Issue.REPORTER, User.ID, "Reporter"));
    convertors.add(JqlEnum.generic("assignee", Issue.ASSIGNEE, User.ID, "Assignee"));
    convertors.add(JqlEnum.generic("level", Issue.SECURITY, Security.ID, "Security Level"));
    convertors.add(new JqlDate("updated", Issue.UPDATED, "Updated"));
    convertors.add(new JqlDate("created", Issue.CREATED, "Created"));
    convertors.add(new JqlDate("due", Issue.DUE, "Due"));
    convertors.add(new JqlDate("resolved", Issue.RESOLVED, "Resolved"));
    CustomFieldsComponent customFields = myConnection.getCustomFields();
    LongArray fields = CustomField.queryKnownKey(myReader, myConnection.getConnectionItem());
    for (LongIterator cursor : fields) {
      ItemVersion field = SyncUtils.readTrunk(myReader, cursor.value());
      JQLConvertor jqlConvertor = customFields.getJQLSearch(field);
      if (jqlConvertor != null) convertors.add(jqlConvertor);
    }
    return convertors;
  }

  public DBReader getReader() {
    return myReader;
  }
}
