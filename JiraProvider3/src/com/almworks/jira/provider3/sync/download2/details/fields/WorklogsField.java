package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.download2.details.slaves.DependentBagField;
import com.almworks.jira.provider3.sync.download2.details.slaves.SimpleDependent;
import com.almworks.jira.provider3.sync.download2.details.slaves.SlaveLoader;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.JRWorklog;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerWorklog;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.ArrayKey;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class WorklogsField implements JsonIssueField {
  public static final WorklogsField INSTANCE = new WorklogsField();

  private static final ArrayKey<JSONObject> WORKLOGS = ArrayKey.objectArray("worklogs");
  private static final HintValue<Boolean> NOT_FULL = HintValue.flag("worklog.load.notFull");

  private final SimpleDependent mySlaveLoader = new SimpleDependent(ServerWorklog.TYPE, ServerWorklog.ISSUE, JRWorklog.PARTIAL_JSON_CONVERTOR, null);

  @Override
  public Collection<? extends ParsedValue> loadValue(@Nullable Object jsonValue) {
    if (jsonValue == null) return null;
    JSONObject obj = Util.castNullable(JSONObject.class, jsonValue);
    if (obj == null) {
      LogHelper.error("Expected worklogs object", jsonValue);
      return null;
    }
    JSONArray worklogs = WORKLOGS.getValue(jsonValue);
    if (worklogs == null) {
      LogHelper.error("Missing worklogs records");
      return null;
    }
    return loadWorklogs(CheckFullCollection.isFullCollection(obj), worklogs);
  }


  private Collection<? extends ParsedValue> loadWorklogs(boolean allKnown, JSONArray worklogs) {
    List<SlaveLoader.Parsed<EntityBag2>> fullBag = DependentBagField.loadEntities(worklogs, mySlaveLoader);
    if (allKnown) return DependentBagField.createBagValueCollection(fullBag, mySlaveLoader);
    ArrayList<ParsedValue> result = Collections15.arrayList();
    result.add(new SlaveLoader.AsValue(fullBag));
    result.add(NOT_FULL);
    return result;
  }

  @Override
  public Collection<? extends ParsedValue> loadNull() {
    LogHelper.debug("Missing worklogs in response");
    return DependentBagField.createBagValueCollection(Collections.<SlaveLoader.Parsed<EntityBag2>>emptyList(), mySlaveLoader);
  }

  public void maybeLoadAdditional(EntityHolder issue, RestSession session, ProgressInfo progress) throws ConnectorException {
    if (!NOT_FULL.isValueSet(issue)) return; // all worklogs has been loaded
    Integer issueId = issue.getScalarValue(ServerIssue.ID);
    if (issueId == null) {
      LogHelper.error("Missing issue ID", issue);
      return;
    }
    RestResponse response = session.restGet("api/2/issue/" + issueId + "/worklog", RequestPolicy.SAFE_TO_RETRY);
    if (!response.isSuccessful()) {
      LogHelper.error("Failed to load all worklogs", response.getStatusCode());
      return;
    }
    try {
      JSONObject object = response.getJSONObject();
      if (!CheckFullCollection.isFullCollection(object)) {
        LogHelper.error("Not all worklogs has been loaded");
        return;
      }
      JSONArray worklogs = WORKLOGS.getValue(object);
      if (worklogs == null) {
        LogHelper.error("Missing worklogs");
        return;
      }
      List<SlaveLoader.Parsed<EntityBag2>> fullBag = DependentBagField.loadEntities(worklogs, mySlaveLoader);
      DependentBagField.createBagValue(fullBag, mySlaveLoader).addTo(issue);
    } catch (ParseException e) {
      LogHelper.error("Failed to get worklogs", e);
    }
    progress.setDone();
  }
}
