package com.almworks.jira.provider3.sync.download2.details;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.Connection;
import com.almworks.integers.LongArray;
import com.almworks.items.api.*;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.loadxml.FieldKeysLoader;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.ConnectorManager;
import com.almworks.jira.provider3.sync.download2.rest.JRField;
import com.almworks.restconnector.json.sax.LocationHandler;
import com.almworks.restconnector.json.sax.PeekEntryValue;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.Map;

public class CustomFieldsSchema {
  private static final LocalizedAccessor.Value DB_LOAD_FAILURE_SHORT = ConnectorManager.LOCAL.getFactory("loadSchema.dbFailure.short");
  private static final LocalizedAccessor.Value DB_LOAD_FAILURE_FULL = ConnectorManager.LOCAL.getFactory("loadSchema.dbFailure.full");

  private static final String STD_LABELS_CLASS = "com.atlassian.jira.plugin.system.customfieldtypes:labels";
  public static final String CUSTOMFIELD_PREFIX = "customfield_";
  public static final String ID_LABELS = "labels";
  private final Map<String, Pair<FieldKind.Field, String>> myFieldAndClass = Collections15.hashMap();
  private boolean mySchemaWritten = false;

  private CustomFieldsSchema() {
  }

  public static boolean isCustomField(String id) {
    return id != null && (ID_LABELS.equals(id) || id.startsWith(CUSTOMFIELD_PREFIX));
  }

  public void writeSchema(EntityTransaction transaction) {
    if (mySchemaWritten) return;
    for (Pair<FieldKind.Field, String> pair : myFieldAndClass.values()) {
      String fieldClass = pair.getSecond();
      FieldKind.Field field = pair.getFirst();
      if (field != null) field.setupField(transaction, fieldClass);
    }
    mySchemaWritten = true;
  }

  @Nullable
  public JsonIssueField getField(String id) {
    LogHelper.assertError(mySchemaWritten, "Schema not written", id);
    Pair<FieldKind.Field, String> pair = myFieldAndClass.get(id);
    FieldKind.Field field = pair != null ? pair.getFirst() : null;
    return field != null ? field.getIssueField() : null;
  }

  private void addField(String id, String fieldClass, String connectionId, @Nullable FieldKind kind, @Nullable String name) {
    if (kind == null) kind = FieldKeysLoader.UNKNOWN;
    FieldKind.Field field = kind.createFieldsDescriptor(id, connectionId, name);
    myFieldAndClass.put(id, Pair.create(field, fieldClass));
    if (mySchemaWritten) {
      LogHelper.error("Schema written before filled");
      mySchemaWritten = false;
    }
  }

  public static CustomFieldsSchema loadFromDB(SyncManager syncManager, final Map<String, FieldKind> fieldKinds, Connection connection) throws ConnectorException {
    final DBIdentifiedObject connectionObj = connection.getConnectionObj();
    CustomFieldsSchema schema = syncManager.enquireRead(DBPriority.FOREGROUND, new ReadTransaction<CustomFieldsSchema>() {
      @Override
      public CustomFieldsSchema transaction(DBReader reader) throws DBOperationCancelledException {
        long connectionItem = reader.findMaterialized(connectionObj);
        CustomFieldsSchema result = new CustomFieldsSchema();
        if (connectionItem <= 0) {
          LogHelper.warning("Connection not materialized", connectionObj);
          return result;
        }
        ItemVersion connection = SyncUtils.readTrunk(reader, connectionItem);
        String connectionId = connection.getValue(SyncAttributes.CONNECTION_ID);
        if (connectionId == null) {
          LogHelper.error("Missing connection id", connection);
          return result;
        }
        LongArray fields = CustomField.queryKnownKey(connection);
        for (ItemVersion field : connection.readItems(fields)) {
          String id = field.getValue(CustomField.ID);
          String fieldClass = field.getValue(CustomField.KEY);
          if (id == null || fieldClass == null) {
            LogHelper.error("Missing custom field values", id, fieldClass, field);
            continue;
          }
          String name = field.getValue(CustomField.NAME);
          FieldKind kind = Util.NN(fieldKinds.get(fieldClass), FieldKeysLoader.UNKNOWN);
          result.addField(id, fieldClass, connectionId, kind, name);
        }
        return result;
      }
    }).waitForCompletion();
    if (schema == null) throw new ConnectorException("db load failed", DB_LOAD_FAILURE_SHORT.create(), DB_LOAD_FAILURE_FULL.create());
    return schema;
  }

  public static class RestLoader {
    private final Map<String, FieldKind> myCustomFields;
    private final String myConnectionId;
    private final Map<String, String> myFieldClasses = Collections15.hashMap();
    private final Procedure2<String, JSONObject> mySchemaConsumer = new Procedure2<String, JSONObject>() {
      @Override
      public void invoke(String id, JSONObject description) {
        String fieldClass = getFieldCustomClass(description);
        if (fieldClass != null) myFieldClasses.put(id, fieldClass);
        else if (isCustomField(id)) LogHelper.error("Missing custom class", description);
      }
    };
    private final Map<String, String> myNames = Collections15.hashMap();
    private final Procedure2<String, Object> myNamesConsumer = new Procedure2<String, Object>() {
      @Override
      public void invoke(String id, Object nameObj) {
        String name = Util.castNullable(String.class, nameObj);
        if (name == null) LogHelper.error("Missing name", id, nameObj);
        else myNames.put(id, name);
      }
    };

    public RestLoader(Map<String, FieldKind> customFields, String connectionId) {
      myCustomFields = customFields;
      myConnectionId = connectionId;
    }

    public LocationHandler getSchemaHandler() {
      return PeekEntryValue.objectValue(mySchemaConsumer);
    }

    public LocationHandler getNamesHandler() {
      return new PeekEntryValue(myNamesConsumer).getUpLink();
    }

    public CustomFieldsSchema createSchema() {
      CustomFieldsSchema schema = new CustomFieldsSchema();
      for (Map.Entry<String, String> entry : myFieldClasses.entrySet()) {
        String id = entry.getKey();
        String fieldClass = entry.getValue();
        String name = myNames.get(id);
        FieldKind kind = Util.NN(myCustomFields.get(fieldClass), FieldKeysLoader.UNKNOWN);
        schema.addField(id, fieldClass, myConnectionId, kind, name);
      }
      return schema;
    }
  }

  @Nullable("For system fields except labels")
  public static String getFieldCustomClass(JSONObject fieldSchema) {
    String fieldClass = JRField.SCHEMA_CUSTOM.getValue(fieldSchema);
    if (fieldClass == null) {
      String system = JRField.SCHEMA_SYSTEM.getValue(fieldSchema);
      if (CustomFieldsSchema.ID_LABELS.equals(system)) fieldClass = STD_LABELS_CLASS;
    }
    return fieldClass;
  }
}
