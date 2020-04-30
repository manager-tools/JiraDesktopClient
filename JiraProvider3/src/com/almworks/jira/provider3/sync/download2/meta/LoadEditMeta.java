package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.integers.IntList;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.jira.provider3.gui.edit.fields.JiraFieldsInfo;
import com.almworks.jira.provider3.sync.download2.meta.core.LoadMetaContext;
import com.almworks.jira.provider3.sync.download2.meta.core.MetaOperation;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.JRField;
import com.almworks.jira.provider3.sync.download2.rest.JRIssue;
import com.almworks.jira.provider3.sync.download2.rest.JqlSearch;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.jql.JQLCompareConstraint;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.Map;
import java.util.Set;

class LoadEditMeta extends MetaOperation {
  private final FieldCollector myFieldCollector;

  protected LoadEditMeta(FieldCollector fieldCollector) {
    super(10);
    myFieldCollector = fieldCollector;
  }

  @Override
  public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException, JiraInternalException {
    ProjectsAndTypes projectsAndTypes = context.getData(LoadRestMeta.PROJECTS);
    progress.startActivity("field values");
    IntList projectIds = projectsAndTypes.getProjectIds();
    ProgressInfo[] progresses = progress.split(projectIds.size());
    Set<Integer> filter = context.getDataOrNull(LoadRestMeta.PROJECT_FILTER);
    for (int i = 0; i <projectIds.size(); i++) {
      ProgressInfo prjProgress = progresses[i];
      int prjId = projectIds.get(i);
      if (filter == null || filter.contains(prjId)) {
        prjProgress.startActivity(projectsAndTypes.getProjectName(prjId));
        IntList types = projectsAndTypes.getTypes(prjId);
        loadTypes(session, transaction, prjId, types, prjProgress);
      }
      prjProgress.setDone();
    }
  }

  private void loadTypes(RestSession session, EntityTransaction transaction, int prjId, @Nullable IntList types, ProgressInfo progress) throws CancelledException {
    if (types == null) return;
    ProgressInfo[] progresses = progress.split(types.size());
    ProgressInfo prevProgress = null;
    for (int i = 0; i < types.size(); i++) {
      if (prevProgress != null) prevProgress.setDone();
      prevProgress = progresses[i];
      int typeId = types.get(i);
      JSONObject fields;
      try {
        JSONObject issue = new JqlSearch(CompositeConstraint.and(
                JQLCompareConstraint.equal("project", prjId),
                JQLCompareConstraint.equal("type", typeId)
        )).addFields("key").querySingle(session);
        if (issue == null) continue;
        String key = JRIssue.KEY.getValue(issue);
        RestResponse response = session.restGet(String.format("api/2/issue/%s/editmeta", key), RequestPolicy.SAFE_TO_RETRY);
        if (!response.isSuccessful()) {
          myFieldCollector.addAllScope(prjId, typeId);
          continue;
        }
        fields = JRField.FIELDS.getValue(response.getJSONObject());
      } catch (CancelledException e) {
        throw e;
      } catch (ConnectorException e) {
        LogHelper.warning("Exception during load editmeta", prjId, typeId, e);
        continue;
      } catch (ParseException e) {
        LogHelper.warning("Failed to parse editmeta", prjId, typeId, e);
        continue;
      }
      JiraFieldsInfo fieldsInfo = JiraFieldsInfo.getInstance(transaction);
      for (Map.Entry<String, Object> entry : ((Map<String, Object>) fields).entrySet()) {
        JSONObject field = Util.castNullable(JSONObject.class, entry.getValue());
        String fieldId = entry.getKey();
        if (field == null || fieldId == null) {
          LogHelper.error("Missing field", entry);
          continue;
        }
        myFieldCollector.processField(fieldId, field, prjId, typeId);
        fieldsInfo.processField(prjId, typeId, fieldId, field);
      }
    }
  }
}
