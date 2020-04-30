package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldDescriptor;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFields;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.jira.provider3.sync.download2.details.CustomFieldsSchema;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.download2.details.slaves.DependentBagField;
import com.almworks.jira.provider3.sync.download2.details.slaves.SimpleDependent;
import com.almworks.jira.provider3.sync.download2.rest.JRComment;
import com.almworks.jira.provider3.sync.download2.rest.JRIssue;
import com.almworks.jira.provider3.sync.schema.ServerComment;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class JiraIssueJsonFields {
  /**
   * @see LoadDetails#loadWatchersVotes(EntityTransaction, RestSession, ProgressInfo, int, JsonUserParser.LoadedUser)
   */
  @SuppressWarnings("JavadocReference")
  public static final ObjectField STATIC_FIELDS;
  static {
    ObjectField.Builder time = new ObjectField.Builder(true) // todo check null values
      .add("remainingEstimateSeconds", ScalarField.integer(ServerIssue.REMAIN_ESTIMATE))
      .add("originalEstimateSeconds", ScalarField.integer(ServerIssue.ORIGINAL_ESTIMATE));
    // For votes and watches see LoadDetails#loadWatchersVotes
    ObjectField votes = new ObjectField.Builder(true)
      .add("votes", JsonIssueField.FilteredValue.integerPositive(ServerIssue.VOTES_COUNT))
      .add("hasVoted", JsonIssueField.FilteredValue.boolTrueOnly(ServerIssue.VOTED))
      .create();
    ObjectField watchers = new ObjectField.Builder(true)
      .add("watchCount", JsonIssueField.FilteredValue.integerPositive(ServerIssue.WATCHERS_COUNT)) // JIRA REST API reports wrong zero value for watched issues. Only details provide right watches list
      .add("isWatching", JsonIssueField.FilteredValue.boolTrueOnly(ServerIssue.WATCHING))
      .create();
    ObjectField.Builder builder = new ObjectField.Builder(true)
      .add(ServerFields.TIME_TRACKING.getJiraId(), time.create())
      .add("votes", votes)
      .add("watches", watchers)
      .add(ServerFields.COMMENTS.getJiraId(), new FieldMultiplexer(
        new SimpleDependent(ServerComment.TYPE, ServerComment.ISSUE, JRComment.PARTIAL_JSON_CONVERTOR, null).toField(false, "comments"),
        CheckFullCollection.INSTANCE))
      .add(ServerFields.LINKS.getJiraId(), DependentBagField.create(new SlaveLink(), true))
      .add(ServerFields.WORK_LOG.getJiraId(), WorklogsField.INSTANCE)
      .add("subtasks", DependentBagField.create(new Subtasks(), true))
      .add(ServerFields.PARENT.getJiraId(), ScalarField.entity(ServerIssue.PARENT, JRIssue.DUMMY_JSON_CONERTOR))
      .add("timespent", ScalarField.integer(ServerIssue.TIME_SPENT).nullValue(null))
      .add(ServerFields.ATTACHMENT.getJiraId(), AttachmentsField.INSTANCE);
    for (IssueFieldDescriptor descriptor : IssueFields.DESCRIPTORS) {
      builder.add(descriptor.getFieldId(), descriptor.createDownloadField());
    }
    STATIC_FIELDS = builder.create();
    // Not loaded custom fields are cleared during DB write. See com.almworks.jira.provider3.custom.impl.RemoteMetaConfig.beforeWrite
    // Ignored: progress, workratio, aggregateprogress, lastViewed, timeoriginalestimate, aggregatetimeoriginalestimate, aggregatetimeestimate, timeestimate, aggregatetimespent
  }

  public static void loadIssue(EntityHolder issue, JSONObject fields, CustomFieldsSchema customSchema) {
    ArrayList<JsonIssueField.ParsedValue> left = Collections15.arrayList(JiraIssueJsonFields.STATIC_FIELDS.loadValue(fields));
    applyValues(issue, left);
    ArrayList<JsonIssueField.ParsedValue> allValues = Collections15.arrayList();
    for (Map.Entry<String, Object> entry : ((Map<String, Object>) fields).entrySet()) {
      String id = entry.getKey();
      JsonIssueField issueField = customSchema.getField(id);
      if (issueField == null) continue;
      Collection<? extends JsonIssueField.ParsedValue> values = LogHelper.withHint("FieldId=" + id, () -> issueField.loadValue(entry.getValue()));
      if (values != null) allValues.addAll(values);
    }
    applyValues(issue, allValues);
  }

  private static void applyValues(EntityHolder issue, ArrayList<JsonIssueField.ParsedValue> left) {
    int prevLeft = Integer.MAX_VALUE;
    while (prevLeft > left.size()) {
      prevLeft = left.size();
      for (Iterator<JsonIssueField.ParsedValue> it = left.iterator(); it.hasNext(); ) if (it.next().addTo(issue)) it.remove();
    }
    if (!left.isEmpty()) LogHelper.error("Failed to store values", issue, left);
  }
}
