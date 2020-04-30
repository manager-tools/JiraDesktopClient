package com.almworks.jira.provider3.custom.impl;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.LoadAllFields;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class FieldTypesMigration {
  private final Map<String, Map<TypedKey<?>, ?>> myCurrent;
  private final Map<String, Map<TypedKey<?>, ?>> myChanged;
  private final Map<String, Map<TypedKey<?>, ?>> myAdded;
  private final LoadAllFields myAllFields;

  private FieldTypesMigration(Map<String, Map<TypedKey<?>, ?>> current, Map<String, Map<TypedKey<?>, ?>> changed, Map<String, Map<TypedKey<?>, ?>> added, LoadAllFields allFields) {
    myCurrent = current;
    myChanged = changed;
    myAdded = added;
    myAllFields = allFields;
  }

  public static FieldTypesMigration prepare(DBReader reader, Map<String, Map<TypedKey<?>, ?>> updateRequest) throws FieldType.MigrationProblem {
    Map<String, Map<TypedKey<?>, ?>> current = Collections15.hashMap();
    Map<String, Map<TypedKey<?>, ?>> changed = Collections15.hashMap();
    Map<String, Map<TypedKey<?>, ?>> added = Collections15.hashMap();
    StoredCustomFieldConfig actual = StoredCustomFieldConfig.loadActual(reader);
    if (actual == null) throw FieldType.MigrationProblem.internalError();
    if (!CustomFieldsComponent.collectByKey(actual.getConfigs(), current)) throw FieldType.MigrationProblem.internalError();
    for (Map.Entry<String, Map<TypedKey<?>, ?>> entry : updateRequest.entrySet()) {
      String key = entry.getKey();
      Map<TypedKey<?>, ?> update = entry.getValue();
      Map<TypedKey<?>, ?> existing = current.get(key);
      if (existing == null) added.put(key, update);
      else if (!Util.equals(existing, update)) changed.put(key, update);
    }
    LoadAllFields allFields = new LoadAllFields();
    allFields.load(reader);
    FieldTypesMigration migration = new FieldTypesMigration(Collections.unmodifiableMap(current), Collections.unmodifiableMap(changed), Collections.unmodifiableMap(added), allFields);
    migration.checkMigrationPossible();
    return migration;
  }

  private void checkMigrationPossible() throws FieldType.MigrationProblem {
    for (String key : myChanged.keySet()) {
      Map<TypedKey<?>, ?> current = myCurrent.get(key);
      Map<TypedKey<?>, ?> wasEditable = ConfigKeys.EDITABLE.getFrom(current);
      if (wasEditable != null) {
        if (!MigrationChecker.canMigrate(current, myChanged.get(key))) {
          List<Map<TypedKey<?>, ?>> fields = myAllFields.getFieldsByKey(key);
          LogHelper.warning("Cannot migrate", key, fields.isEmpty());
          if (!fields.isEmpty()) throw cantChangeEditableFields(fields);
        }
      }
    }
  }

  private FieldType.MigrationProblem cantChangeEditableFields(List<Map<TypedKey<?>, ?>> fields) {
    StringBuilder builder = new StringBuilder("Can not update editable fields:");
    for (Map<TypedKey<?>, ?> field : fields) {
      String name = LoadAllFields.NAME.getFrom(field);
      builder.append("\n").append(name);
    }
    return new FieldType.MigrationProblem(builder.toString());
  }

  public List<Map<TypedKey<?>, ?>> perform(DBDrain drain, LongSet affectedConnections) throws FieldType.MigrationProblem {
    migrateFields(drain, myChanged, affectedConnections);
    migrateFields(drain, myAdded, affectedConnections);
    ArrayList<Map<TypedKey<?>, ?>> newFieldTypes = Collections15.arrayList();
    for (Map.Entry<String, Map<TypedKey<?>, ?>> entry : myCurrent.entrySet()) {
      String key = entry.getKey();
      Map<TypedKey<?>, ?> updateMap = myChanged.get(key);
      if (updateMap == null) updateMap = entry.getValue();
      newFieldTypes.add(updateMap);
    }
    newFieldTypes.addAll(myAdded.values());
    return newFieldTypes;
  }

  private void migrateFields(DBDrain drain, Map<String, Map<TypedKey<?>, ?>> update, LongSet affectedConnections) throws FieldType.MigrationProblem {
    for (Map<TypedKey<?>, ?> map : update.values()) {
      FieldKind kind;
      try {
        kind = CustomFieldsComponent.createKind(map, CustomFieldsComponent.SCHEMA);
      } catch (FieldType.CreateProblem createProblem) {
        throw new FieldType.MigrationProblem(createProblem);
      }
      for (ItemVersionCreator field : drain.changeItems(myAllFields.getFieldItemsByKey(ConfigKeys.KEY.getFrom(map)))) {
        Long connection = field.getValue(SyncAttributes.CONNECTION);
        if (connection != null) affectedConnections.add(connection);
        kind.migrateField(field);
      }
    }
  }

  public Map<String, Map<TypedKey<?>, ?>> getChanged() {
    return myChanged;
  }

  public Map<String, Map<TypedKey<?>, ?>> getAdded() {
    return myAdded;
  }

  public Map<String, Map<TypedKey<?>, ?>> getCurrent() {
    return myCurrent;
  }
}
