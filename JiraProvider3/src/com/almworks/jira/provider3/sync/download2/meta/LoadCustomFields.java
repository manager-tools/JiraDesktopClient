package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.details.CustomFieldsSchema;
import com.almworks.jira.provider3.sync.download2.meta.core.LoadMetaContext;
import com.almworks.jira.provider3.sync.download2.meta.core.MetaOperation;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.JRField;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.ArrayKey;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.List;

class LoadCustomFields extends MetaOperation {
  private static final ArrayKey<JSONObject> JQL_FIELDS = ArrayKey.objectArray("visibleFieldNames");
  private static final JSONKey<String> JQL_FIELD_CFID = JSONKey.text("cfid");
  private static final ArrayKey<String> JQL_FIELD_OPERATORS = ArrayKey.textArray("operators");

  private final CustomFieldsComponent myFieldsComponent;

  LoadCustomFields(CustomFieldsComponent fieldsComponent) {
    super(2);
    myFieldsComponent = fieldsComponent;
  }

  @Override
  public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException {
    progress.startActivity("custom fields");
    loadFieldSet(session, transaction, progress.spawn(0.5));
    loadFieldSearch(session, transaction, progress.spawnAll());
    progress.setDone();
  }

  private static void loadFieldSearch(RestSession session, EntityTransaction transaction, ProgressInfo progress) {
    JSONObject rawJSON = null;
    try {
      RestResponse response = session.restGet("api/2/jql/autocompletedata", RequestPolicy.SAFE_TO_RETRY);
      if (!response.isSuccessful()) {
        LogHelper.warning("/jql/autocompletedata not available", response.getStatusCode());
        return;
      }
      rawJSON = JSONKey.ROOT_OBJECT.getValue(response.getJSON());
    } catch (ConnectorException e) {
      LogHelper.warning("Failed to load auto complete data", e);
      return;
    } catch (ParseException e) {
      LogHelper.warning("Failed to parse auto complete data", e);
      return;
    }
    if (rawJSON == null) {
      LogHelper.error("Missing /jql/autocompletedata");
      return;
    }
    List<String> searchableIds = Collections15.arrayList();
    for (JSONObject field : JQL_FIELDS.list(rawJSON)) {
      String cfid = JQL_FIELD_CFID.getValue(field);
      if (cfid == null) continue;
      List<String> operators = JQL_FIELD_OPERATORS.list(field);
      if (!operators.isEmpty()) searchableIds.add(cfid);
    }
    ServerInfo.updateSearchableCustomFields(transaction, searchableIds);
    progress.setDone();
  }

  private void loadFieldSet(RestSession session, EntityTransaction transaction, ProgressInfo progress) {
    List<JSONObject> list;
    try {
      RestResponse response = session.restGet("api/2/field", RequestPolicy.SAFE_TO_RETRY);
      response.ensureSuccessful();
      Object rawJSON = response.getJSON();
      list = ArrayKey.ROOT_ARRAY.list(rawJSON);
      if (list.isEmpty()) LogHelper.warning("Missing fields", rawJSON);
    } catch (ConnectorException e) {
      LogHelper.warning("Load fields failed", e);
      return;
    } catch (ParseException e) {
      LogHelper.warning("Failed to parse fields", e);
      return;
    }
    if (list.isEmpty()) return;
    EntityBag2 fieldBad = transaction.addBag(ServerCustomField.TYPE);
    fieldBad.delete();
    for (JSONObject field : list) {
      String atlassianClass = CustomFieldsSchema.getFieldCustomClass(JRField.SCHEMA.getValue(field));
      if (atlassianClass == null) continue;
      String name = JRField.NAME.getValue(field);
      String fieldId = JRField.ID.getValue(field);
      if (name == null || fieldId == null) {
        LogHelper.warning("Failed to get field info", field);
        continue;
      }
      EntityHolder holder = ServerCustomField.getField(transaction, fieldId);
      if (holder == null) {
        LogHelper.error("Failed to store custom field", fieldId, name, atlassianClass, field);
        continue;
      }
      fieldBad.exclude(holder);
      holder.setValue(ServerCustomField.KEY, atlassianClass);
      holder.setValue(ServerCustomField.NAME, name);
      FieldKind kind = myFieldsComponent.getFieldKind(atlassianClass);
      if (kind == null) continue;
      FieldSetup setup = kind.getExtension(FieldSetup.SETUP_FIELD);
      if (setup != null) setup.setupField(holder);
    }
    progress.setDone();
  }
}
