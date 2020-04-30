package com.almworks.jira.provider3.remotedata.issue.edit;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.ItemReference;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.MapItemVersion;
import com.almworks.items.sync.util.PerItemTransactionCache;
import com.almworks.items.util.AttributeMap;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.custom.loadxml.FieldKeysLoader;
import com.almworks.jira.provider3.gui.edit.workflow.WorkflowStep;
import com.almworks.jira.provider3.remotedata.issue.EditRequest;
import com.almworks.jira.provider3.remotedata.issue.MoveIssueStep;
import com.almworks.jira.provider3.remotedata.issue.ParsedIssueFields;
import com.almworks.jira.provider3.remotedata.issue.StepLoader;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldDescriptor;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFields;
import com.almworks.jira.provider3.remotedata.issue.move.MoveLoader;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.services.upload.*;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.workflow.UploadWorkflowUnit;
import com.almworks.jira.provider3.worklogs.PrepareWorklogsUpload;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.text.TextUtil;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.*;

public class EditIssue implements UploadUnit {
  private static final LocalizedAccessor.Value M_CONFLICT_SHORT = PrepareIssueUpload.I18N.getFactory("upload.problem.conflict.short");
  private static final LocalizedAccessor.MessageStr M_CONFLICT_FULL = PrepareIssueUpload.I18N.messageStr("upload.problem.conflict.full");
  private static final LocalizedAccessor.Value M_OPERATION_NAME = PrepareIssueUpload.I18N.getFactory("upload.operation.editIssue");
  private static final LocalizedAccessor.Value M_NOT_DONE_SHORT = PrepareIssueUpload.I18N.getFactory("upload.problem.notDone.short");
  private static final LocalizedAccessor.MessageIntStr M_NOT_DONE_FULL = PrepareIssueUpload.I18N.messageIntStr("upload.problem.notDone.full");
  private final CreateIssueUnit myCreate;
  private final List<IssueFieldValue> myValues;
  @Nullable private final UploadUnit myLastHistory;
  private Boolean myFinished = null;
  private boolean myConflictDetected = false;

  private EditIssue(CreateIssueUnit create, List<IssueFieldValue> values, @Nullable UploadUnit lastHistory) {
    myCreate = create;
    myValues = values;
    myLastHistory = lastHistory;
  }

  public List<IssueFieldValue> getValues() {
    return Collections.unmodifiableList(myValues);
  }

  @NotNull
  public CreateIssueUnit getCreate() {
    return myCreate;
  }

  @Override
  public boolean isDone() {
    return Boolean.TRUE.equals(myFinished);
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    return context.isFailed(myCreate) || (myLastHistory != null && context.isFailed(myLastHistory)) || Boolean.FALSE.equals(myFinished);
  }

  /**
   * This method is required for history units. Such units are "before" edit and cannot depend on edit success, but this units may be interested in detected conflict.
   * @return true if edit is failed due to conflict detected on initial state check
   */
  public boolean isConflictDetected() {
    return myConflictDetected;
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose) throws ConnectorException {
    EditRequest.ensureHasServerInfo(context, session);
    return myCreate.loadServerState(session, transaction, context, purpose);
  }

  @Override
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    Integer issueId = myCreate.getIssueId();
    if (issueId == null) return null;
    EntityHolder issue = ServerIssue.findIssue(transaction, issueId);
    ArrayList<String> conflicts = Collections15.arrayList();
    for (IssueFieldValue value : myValues) {
      String conflictMessage = value.checkInitialState(issue);
      if (conflictMessage != null) conflicts.add(conflictMessage);
    }
    if (conflicts.isEmpty()) return null;
    LogHelper.warning("Conflict detected", myCreate, conflicts);
    myConflictDetected = true;
    return UploadProblem.conflict(myCreate.getIssueItem(), M_CONFLICT_SHORT.create(), M_CONFLICT_FULL.formatMessage(TextUtil.separate(conflicts, "\n")));
  }

  private boolean hasNotDone(RestServerInfo serverInfo) {
    for (IssueFieldValue value : myValues)
      if (value.needsUpload(serverInfo))
        return true;
    return false;
  }

  public boolean hasNotDone(UploadContext context) throws UploadProblem.Thrown {
    return hasNotDone(EditRequest.getServerInfo(context));
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    Integer issueId = myCreate.getIssueId();
    if (issueId == null) return UploadProblem.notNow("Issue not submitted yet " + myCreate).toCollection();
    if (myLastHistory != null && !myLastHistory.isDone()) return UploadProblem.notNow("History not loaded yet " + myLastHistory).toCollection();
    RestServerInfo serverInfo = EditRequest.getServerInfo(context);
    if (!hasNotDone(serverInfo)) {
      myFinished = true;
      return null;
    }
    ParsedIssueFields issueFields = ParsedIssueFields.loadEditMeta(session, issueId);
    EditRequest request = new EditRequest(issueFields, false, serverInfo);
    UploadProblem problem = request.fillFields(myValues);
    if (problem != null) return problem.toCollection();
    if (!request.isAnyNotDone()) {
      myFinished = true;
      return null;
    }
    JSONObject edit = new JSONObject();
    request.addUpdate(edit);
    RestResponse response = null;
    ConnectorException failure = null;
    try {
      response = session.restPut("api/2/issue/" + issueId, edit, RequestPolicy.NEEDS_LOGIN);
    } catch (@NotNull ConnectorException e) {
      failure = e;
    }
    myFinished = request.processResponse(myCreate, response, failure, M_OPERATION_NAME.create());
    return request.getProblems();
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    Integer issueId = myCreate.getIssueId();
    if (issueId == null) return;
    EntityHolder issue = ServerIssue.findIssue(transaction, issueId);
    if (issue == null) {
      LogHelper.error("Issue not downloaded", issueId, myCreate);
      return;
    }
    for (IssueFieldValue value : myValues) value.finishUpload(myCreate.getIssueItem(), issue, context);
    if (!context.isFailed(this)) {
      try {
        RestServerInfo serverInfo = EditRequest.getServerInfo(context);
        ArrayList<IssueFieldValue> notUploaded = Collections15.arrayList();
        for (IssueFieldValue value : myValues) if (value.needsUpload(serverInfo)) notUploaded.add(value);
        UploadProblem problem = createNotDone(notUploaded);
        if (problem != null) context.addMessage(this, problem);
      } catch (UploadProblem.Thrown thrown) {
        LogHelper.error(thrown.getProblem()); // Just log, but do nothing - this should not happen
      }
    }
  }

  @Nullable("When no problem")
  private UploadProblem createNotDone(ArrayList<IssueFieldValue> notUploaded) {
    if (notUploaded == null || notUploaded.isEmpty()) return null;
    StringBuilder builder = new StringBuilder();
    for (IssueFieldValue value : notUploaded) {
      if (builder.length() > 0) builder.append("\n");
      builder.append(value.getDisplayName());
    }
    return UploadProblem.fatal(M_NOT_DONE_SHORT.create(), M_NOT_DONE_FULL.formatMessage(notUploaded.size(), builder.toString()));
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myCreate.getMasterItems();
  }

  /**
   * Loads history step units and edit issue state unit
   * @return pair[stateChange, historySteps]
   */
  static Pair<EditIssue, List<UploadUnit>> load(ItemVersion issue, CreateIssueUnit create, LoadUploadContext context) throws CantUploadException {
    ItemVersion base = issue.switchToServer();
    ArrayList<IssueFieldValue> values = loadValues(context, issue, base);
    List<UploadUnit> steps = loadHistory(issue.switchToTrunk(), create, context);
    UploadUnit lastHistory = steps.isEmpty() ? null : steps.get(steps.size() - 1);
    EditIssue edit = new EditIssue(create, values, lastHistory);
    create.setEdit(edit);
    IssueFieldValue timeTracking = PrepareWorklogsUpload.createEditValue(context, create, issue);
    if (timeTracking != null) edit.myValues.add(timeTracking);
    return Pair.create(edit, steps);
  }

  public static ArrayList<IssueFieldValue> loadValues(LoadUploadContext context, ItemVersion current, ItemVersion base) {
    ArrayList<IssueFieldValue> values = Collections15.arrayList();
    List<IssueFieldDescriptor> descriptors = AllFields.getInstance(current, context.getConnection()).getDescriptors(current);
    for (IssueFieldDescriptor descriptor : descriptors) {
      IssueFieldValue value = descriptor.load(current, base);
      if (value != null) values.add(value);
    }
    return values;
  }

  public static ArrayList<IssueFieldValue> loadValues(LoadUploadContext context, AttributeMap current, ItemVersion base) {
    return loadValues(context, new MapItemVersion(current, base), base);
  }

  private static final Map<ItemReference, StepLoader> STEP_LOADERS;
  static {
    HashMap<ItemReference, StepLoader> map = Collections15.hashMap();
    map.put(WorkflowStep.STEP_KIND, UploadWorkflowUnit.LOADER);
    map.put(MoveIssueStep.STEP_KIND, MoveLoader.INSTANCE);
    STEP_LOADERS = Collections.unmodifiableMap(map);
  }
  @NotNull
  private static List<UploadUnit> loadHistory(ItemVersion issue, CreateIssueUnit create, LoadUploadContext context) throws CantUploadException {
    ArrayList<UploadUnit> steps = Collections15.arrayList();
    UploadUnit prevStep = null;
    HistoryRecord[] history = issue.getHistory();
    for (int i = 0, historyLength = history.length; i < historyLength; i++) {
      HistoryRecord record = history[i];
      StepLoader loader = record.mapKind(issue.getReader(), STEP_LOADERS);
      if (loader == null) {
        LogHelper.error("Unknown step kind", record);
        continue;
      }
      UploadUnit step = loader.loadStep(issue, record, create, context, prevStep, i);
      if (step != null) {
        prevStep = step;
        steps.add(step);
      }
    }
    return steps;
  }

  private static class AllFields {
    private static final PerItemTransactionCache<AllFields> KEY = PerItemTransactionCache.create("customFields");
    private final TLongObjectHashMap<IssueFieldDescriptor> myFieldToDescriptor;
    private final JiraConnection3 myConnection3;

    public AllFields(TLongObjectHashMap<IssueFieldDescriptor> fieldToDescriptor, JiraConnection3 connection3) {
      //To change body of created methods use File | Settings | File Templates.
      myFieldToDescriptor = fieldToDescriptor;
      myConnection3 = connection3;
    }

    public static AllFields getInstance(VersionSource source, JiraConnection3 jiraConnection) {
      ItemVersion connection = source.forItem(jiraConnection.getConnectionItem());
      AllFields result = KEY.get(connection);
      if (result == null) {
        result = create(connection, jiraConnection);
        KEY.put(connection, result);
      }
      return result;
    }

    private static AllFields create(ItemVersion connection, JiraConnection3 jiraConnection) {
      CustomFieldsComponent component = jiraConnection.getCustomFields();
      String connectionID = jiraConnection.getConnectionID();
      TLongObjectHashMap<IssueFieldDescriptor> fieldToDescriptor = new TLongObjectHashMap<>();
      for (ItemVersion field : connection.readItems(CustomField.queryKnownKey(connection))) {
        IssueFieldDescriptor descriptor = createDescriptor(field, component, connectionID);
        if (descriptor != null) fieldToDescriptor.put(field.getItem(), descriptor);
      }
      return new AllFields(fieldToDescriptor, jiraConnection);
    }

    private static IssueFieldDescriptor createDescriptor(ItemVersion field, CustomFieldsComponent component, String connectionID) {
      String atlasClass = field.getValue(CustomField.KEY);
      String id = field.getValue(CustomField.ID);
      if (atlasClass == null || id == null) {
        LogHelper.error("Missing class, id", atlasClass, id, field);
        return null;
      }
      String name = field.getValue(CustomField.NAME);
      FieldKind kind = Util.NN(component.getFieldKind(atlasClass), FieldKeysLoader.UNKNOWN);
      FieldKind.Field fieldObject = kind.createFieldsDescriptor(id, connectionID, name);
      return fieldObject != null ? fieldObject.getDescriptor() : null;
    }

    public List<IssueFieldDescriptor> getDescriptors(ItemVersion issue) {
      ItemVersion trunk = issue.switchToTrunk();
      ItemVersion server = issue.switchToServer();
      collectDescriptors(trunk);
      collectDescriptors(server);
      ArrayList<IssueFieldDescriptor> result = Collections15.arrayList();
      result.addAll(IssueFields.DESCRIPTORS);
      //noinspection unchecked
      result.addAll((List<IssueFieldDescriptor>)(List<?>)Arrays.asList(myFieldToDescriptor.getValues()));
      return result;
    }

    private void collectDescriptors(ItemVersion item) {
      CustomFieldsComponent component = myConnection3.getCustomFields();
      String connectionID = myConnection3.getConnectionID();
      for (DBAttribute<?> attribute : item.getAllShadowableMap().keySet()) {
        long fieldItem = CustomField.getCustomField(item.getReader(), attribute);
        if (fieldItem <= 0 || myFieldToDescriptor.contains(fieldItem)) continue;
        ItemVersion field = item.readTrunk(fieldItem);
        IssueFieldDescriptor descriptor = createDescriptor(field, component, connectionID);
        if (descriptor != null) {
          LogHelper.warning("Not collected field", fieldItem, descriptor);
          myFieldToDescriptor.put(field.getItem(), descriptor);
        }
      }
    }
  }
}
