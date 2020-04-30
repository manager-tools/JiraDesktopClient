package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.CannotParseException;
import com.almworks.api.connector.http.ConnectionException;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.jira.provider3.sync.download2.details.CustomFieldsSchema;
import com.almworks.jira.provider3.sync.download2.rest.JRField;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.json.sax.*;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import com.almworks.util.commons.Procedure2;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class ParsedIssueFields {
  private static final TypedKey<Map<Pair<Integer, Integer>, ParsedIssueFields>> CREATE_META = TypedKey.create("createMeta");
  private final Map<String, Info> myInfos = Collections15.hashMap();
  private final List<BiConsumer<String, JSONObject>> myAdditionalFieldProcessors = new ArrayList<>();

  public ParsedIssueFields() {
  }

  public void addFieldProcessor(BiConsumer<String, JSONObject> processor) {
    myAdditionalFieldProcessors.add(processor);
  }

  @NotNull
  public static ParsedIssueFields loadEditMeta(RestSession session, int issueId) throws ConnectorException {
    RestResponse response = restGetEditMeta(session, issueId);
    response.ensureSuccessful();
    return parseEditMeta(response);
  }

  public static ParsedIssueFields parseEditMeta(RestResponse response) throws ConnectionException, CannotParseException {
    ParsedIssueFields issueFields = new ParsedIssueFields();
    response.parseJSON(PeekObjectEntry.objectEntry("fields", PeekEntryValue.objectValue(new ParseFields(issueFields))));
    return issueFields;
  }

  public static RestResponse restGetEditMeta(RestSession session, int issueId) throws ConnectorException {
    return session.restGet(String.format("api/2/issue/%s/editmeta", issueId), RequestPolicy.SAFE_TO_RETRY);
  }

  public static ParsedIssueFields loadTransitionMeta(RestSession session, int issueId, final int actionId) throws ConnectorException {
    StringBuilder request = new StringBuilder();
    request.append("api/2/issue/").append(issueId).append("/transitions?");
    request.append("transitionId=").append(actionId).append("&");
    request.append("expand=transitions.fields");
    RestResponse response = session.restGet(request.toString(), RequestPolicy.SAFE_TO_RETRY);
    response.ensureSuccessful();
    ParsedIssueFields issueFields = new ParsedIssueFields();
    LocationHandler parser = issueFields.createFieldsHandler();
    LocationHandler check = PeekObjectEntry.objectEntry("id", new JSONCollector(new Procedure<Object>() {
      @Override
      public void invoke(Object arg) {
        Integer actual = JSONKey.INTEGER.convert(arg);
        LogHelper.assertError(Util.equals(actual, actionId), "Expected action", actionId, "but was", arg);
      }
    }));
    response.parseJSON(PeekArrayElement.entryArray("transitions", new CompositeHandler(parser, check)));
    return issueFields;
  }

  public LocationHandler createFieldsHandler() {
    return PeekObjectEntry.objectEntry("fields", PeekEntryValue.objectValue(new ParseFields(this)));
  }

  public static ParsedIssueFields loadCreateMeta(RestSession session, int projectId, int typeId) throws ConnectorException {
    Pair<Integer, Integer> loadedKey = Pair.create(projectId, typeId);
    Map<Pair<Integer, Integer>, ParsedIssueFields> cache = session.getUserData().getUserData(CREATE_META);
    if (cache == null) {
      cache = Collections15.hashMap();
      session.getUserData().putUserData(CREATE_META, cache);
    }
    ParsedIssueFields fields = cache.get(loadedKey);
    if (fields == null) {
      RestResponse response = session.restGet("api/2/issue/createmeta?projectIds=" + projectId + "&issuetypeIds=" + typeId + "&expand=projects.issuetypes.fields.", RequestPolicy.SAFE_TO_RETRY);
      response.ensureSuccessful();
      fields = new ParsedIssueFields();
      response.parseJSON(new PeekObjectEntry("fields", 8, PeekEntryValue.objectValue(new ParseFields(fields))).getUpLink());
      LogHelper.assertError(!fields.myInfos.isEmpty(), "No fields for project:", projectId, "type:", typeId);
      cache.put(loadedKey, fields);
    }
    return fields;
  }

  private void addInfo(String field, boolean required, List<String> operations) {
    myInfos.put(field, new Info(field, required, operations));
  }

  public boolean hasOperation(String fieldId, String operation) {
    Info info = myInfos.get(fieldId);
    return info != null && info.hasOperation(operation);
  }

  @Nullable
  public Info getFieldInfo(String fieldId) {
    return myInfos.get(fieldId);
  }

  public Map<String, Info> getFields() {
    return Collections.unmodifiableMap(myInfos);
  }

  public List<EntityHolder> getAllFields(EntityTransaction transaction) {
    ArrayList<EntityHolder> result = Collections15.arrayList();
    for (String id : myInfos.keySet()) {
      EntityHolder field = getFieldEntity(transaction, id);
      if (field != null) result.add(field);
    }
    return result;
  }

  @Nullable("For unknown static fields")
  private static EntityHolder getFieldEntity(EntityTransaction transaction, String id) {
    if (CustomFieldsSchema.isCustomField(id)) return ServerCustomField.getField(transaction, id);
    Entity field = ServerFields.staticFieldEntity(id);
    if (field != null) return transaction.addEntity(field);
    return null;
  }

  public static class Info {
    private final String myId;
    private final boolean myRequired;
    private final List<String> myOperations;

    public Info(String id, boolean required, List<String> operations) {
      myId = id;
      myRequired = required;
      myOperations = Collections.unmodifiableList(operations);
    }

    public String getId() {
      return myId;
    }

    public boolean isRequired() {
      return myRequired;
    }

    public List<String> getOperations() {
      return myOperations;
    }

    public boolean hasOperation(String operation) {
      return myOperations.contains(operation);
    }

    @Nullable
    public EntityHolder getFieldEntity(EntityTransaction transaction) {
      EntityHolder fieldHolder = ParsedIssueFields.getFieldEntity(transaction, myId);
      if (fieldHolder == null && myRequired) LogHelper.warning("Unknown required field", myId); // todo JCO-1547
      return fieldHolder;
    }
  }


  private static class ParseFields implements Procedure2<String, JSONObject> {
    private final ParsedIssueFields myIssueFields;

    public ParseFields(ParsedIssueFields issueFields) {
      myIssueFields = issueFields;
    }

    @Override
    public void invoke(String field, JSONObject description) {
      Boolean required = JRField.REQUIRED.getValue(description);
      if (required == null) {
        LogHelper.warning("Missing required", field);
        required = false;
      }
      List<String> operations = JRField.OPERATIONS.list(description);
      myIssueFields.addInfo(field, required, operations);
      for (BiConsumer<String, JSONObject> processor : myIssueFields.myAdditionalFieldProcessors) {
        processor.accept(field, description);
      }
    }
  }
}
