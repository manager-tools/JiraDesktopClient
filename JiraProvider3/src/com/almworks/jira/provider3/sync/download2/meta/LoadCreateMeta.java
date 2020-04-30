package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.CannotParseException;
import com.almworks.api.connector.http.ConnectionException;
import com.almworks.integers.IntArray;
import com.almworks.integers.IntIterator;
import com.almworks.integers.IntList;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.jira.provider3.gui.edit.fields.JiraFieldsInfo;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.jira.provider3.sync.download2.meta.core.LoadMetaContext;
import com.almworks.jira.provider3.sync.download2.meta.core.MetaOperation;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.JRField;
import com.almworks.jira.provider3.sync.download2.rest.JRGeneric;
import com.almworks.jira.provider3.sync.schema.ServerProject;
import com.almworks.jira.provider3.sync.schema.ServerSecurity;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.sax.LocationHandler;
import com.almworks.util.LogHelper;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectProcedure;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

/**
 * Loads:<br>
 * 1. security levels<br>
 * 2. (Enum) custom fields options<br>
 * 3. Custom fields applicability (based on accessible on create issue)
 */
class LoadCreateMeta extends MetaOperation {
  private final FieldCollector myCustomFields;
  private final TLongObjectHashMap<IntArray> myLevelsInProjects = new TLongObjectHashMap<>();
  private final IntArray myLevelOrder = new IntArray();

  public LoadCreateMeta(FieldCollector customFields) {
    super(3);
    myCustomFields = customFields;
  }

  @Override
  public void perform(RestSession session, final EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException {
    if (session.getCredentials().isAnonymous()) return; // Create meta is not accessible for anonymous
    progress.startActivity("field values");
    String request = createRequest(context.getDataOrNull(LoadRestMeta.PROJECTS));
    RestResponse response;
    try {
      response = session.restGet(request, RequestPolicy.SAFE_TO_RETRY);
      if (!response.isSuccessful()) {
        int code = response.getStatusCode();
        LogHelper.assertError(code == 401, "Unexpected code", code);
        LogHelper.warning("Create meta not available");
        return;
      }
    } catch (ConnectorException e) {
      LogHelper.warning("Load createmeta failed", e);
      return;
    }
    LocationHandler handler = new CreateMetaFields() {
      @Override
      protected void addField(int projectId, int typeId, String id, JSONObject field) {
        processFields(transaction, projectId, typeId, id, field);
      }
    }.createMetaHandler();
    try {
      response.parseJSON(handler, progress.spawnTransferTracker(0.9));
    } catch (ConnectionException e) {
      LogHelper.warning("Load create meta failed (read)", e);
    } catch (CannotParseException e) {
      LogHelper.warning("Load create meta failed (parse)", e);
    }
    postProcess(transaction);
    progress.setDone();
  }

  private void processFields(EntityTransaction transaction, int projectId, int typeId, String id, JSONObject field) {
    if (ServerFields.SECURITY.getJiraId().equals(id)) addSecurityLevels(transaction, projectId, field);
    else myCustomFields.processField(id, field, projectId, typeId);
    JiraFieldsInfo.getInstance(transaction).processField(projectId, typeId, id, field);
  }

  private String createRequest(@Nullable ProjectsAndTypes projects) {
    StringBuilder request = new StringBuilder("api/2/issue/createmeta?expand=projects.issuetypes.fields.");
    if (projects == null) {
      LogHelper.warning("No project info. No project filter is added (see previous exceptions or warnings)");
      return request.toString();
    }
    IntList filteredProjects = projects.getProjectIds();
    request.append("&projectIds=");
    String sep = "";
    for (IntIterator cursor : filteredProjects) {
      request.append(sep);
      sep = "%2C";
      request.append(cursor.value());
    }
    return request.toString();
  }

  private void postProcess(EntityTransaction transaction) {
    postProcessSecurityLevels(transaction);
  }

  private void postProcessSecurityLevels(final EntityTransaction transaction) {
    final EntityBag2 allLevels = transaction.addBag(ServerSecurity.TYPE);
    allLevels.delete();
    myLevelsInProjects.forEachEntry(new TLongObjectProcedure<IntArray>() {
      @Override
      public boolean execute(long levelId, IntArray projects) {
        EntityHolder holder = transaction.addEntity(ServerSecurity.TYPE, ServerSecurity.ID, (int) levelId);
        EntityUtils.setRefCollectionByIds(holder, ServerSecurity.ONLY_IN_PROJECTS, projects, ServerProject.TYPE, ServerProject.ID);
        allLevels.exclude(holder);
        return true;
      }
    });
    for (int i = 0; i < myLevelOrder.size(); i++) {
      EntityHolder holder = transaction.addEntity(ServerSecurity.TYPE, ServerSecurity.ID, myLevelOrder.get(i));
      if (holder != null) holder.setValue(ServerSecurity.ORDER, i);
    }
  }

  private void addSecurityLevels(EntityTransaction transaction, int project, JSONObject field) {
    for (JSONObject level : JRField.ALLOWED_VALUES.list(field)) {
      Integer levelId = JRGeneric.ID_INT.getValue(level);
      String name = JRGeneric.NAME.getValue(level);
      if (levelId == null || name == null) {
        LogHelper.warning("Missing security level info", levelId, name);
        continue;
      }
      IntArray projects = myLevelsInProjects.get(levelId);
      if (projects == null) {
        projects = new IntArray();
        myLevelsInProjects.put(levelId, projects);
        myLevelOrder.add(levelId);
      }
      projects.addSorted(project);
      EntityHolder holder = transaction.addEntity(ServerSecurity.TYPE, ServerSecurity.ID, levelId);
      if (holder == null) LogHelper.error("Failed to store level", levelId);
      else holder.setNNValue(ServerSecurity.NAME, name);
    }
  }
}
