package com.almworks.jira.provider3.custom.impl;

import com.almworks.integers.LongIterator;
import com.almworks.items.api.DBException;
import com.almworks.items.api.DBWriter;
import com.almworks.items.sync.util.WriterToDrainAdapter;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.loadxml.FieldKeysLoader;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import org.almworks.util.TypedKey;

import java.util.List;
import java.util.Map;

class RestoreTypesConfigMethod {
  private final WriterToDrainAdapter myDrain;
  private FieldType.MigrationProblem myMigrationProblem;

  public RestoreTypesConfigMethod(DBWriter writer) {
    myDrain = new WriterToDrainAdapter(writer);
  }

  public Map<String, FieldKind> perform() {
    List<Map<TypedKey<?>, ?>> fieldsConfig = restoreFromDB();
    if (fieldsConfig == null) fieldsConfig = setupInitial3_0_1();
    try {
      return CustomFieldsComponent.createKindsMap(true, fieldsConfig);
    } catch (FieldType.CreateProblem createProblem) {
      LogHelper.error("Should not happen", createProblem);
      return null;
    }
  }

  private List<Map<TypedKey<?>, ?>> setupInitial3_0_1() {
    LogHelper.warning("No custom fields types restored from DB. Migrating from 3.0.1 meta schema");
    List<Map<TypedKey<?>, ?>> fields3_0_1;
    try {
      FieldKeysLoader loader = FieldKeysLoader.load("/com/almworks/jira/provider3/customFields_3_0_1.xml", CustomFieldsComponent.SCHEMA);
      fields3_0_1 = loader.getLoadedKinds();
    } catch (Exception e) {
      LogHelper.error("Restoring 3.0.1 schema", e);
      throw new DBException(e);
    }
    StoredCustomFieldConfig.create(fields3_0_1).writeActual(myDrain);
    StoredCustomFieldConfig.clearPending(myDrain);
    StoredCustomFieldConfig uptodate;
    try {
      uptodate = loadLastConfig();
    } catch (Exception e) {
      LogHelper.error("Restoring up to date schema. Reverting to 3.0.1", e);
      return fields3_0_1;
    }
    List<Map<TypedKey<?>, ?>> result = doMigrate(StoredCustomFieldConfig.create(fields3_0_1), uptodate);
    if (myMigrationProblem != null) LogHelper.error("Error migrating to actual", myMigrationProblem);
    return result;
  }

  private StoredCustomFieldConfig loadLastConfig() throws Exception {
    StoredCustomFieldConfig uptodate;
    FieldKeysLoader loader = FieldKeysLoader.load("/com/almworks/jira/provider3/customFields.xml", CustomFieldsComponent.SCHEMA);
    uptodate = StoredCustomFieldConfig.create(loader.getLoadedKinds(), loader.getRevision());
    return uptodate;
  }

  private List<Map<TypedKey<?>, ?>> restoreFromDB() {
    StoredCustomFieldConfig actual = StoredCustomFieldConfig.loadActual(myDrain.getReader());
    if (actual == null) return null;
    List<Map<TypedKey<?>, ?>> pending = StoredCustomFieldConfig.loadPending(myDrain.getReader());
    if (pending != null) {
      StoredCustomFieldConfig.clearPending(myDrain);
      return doMigrate(actual, StoredCustomFieldConfig.create(pending));
    }
    List<Map<TypedKey<?>, ?>> fieldsConfig = actual.getConfigs();
    try {
      CustomFieldsComponent.createKindsMap(false, fieldsConfig);
    } catch (FieldType.CreateProblem createProblem) {
      LogHelper.error("Problem loading from DB", createProblem);
      return null;
    }
    StoredCustomFieldConfig lastKnown;
    try {
      lastKnown = loadLastConfig();
    } catch (Exception e) {
      LogHelper.error("Failed to load last known config", e);
      return fieldsConfig;
    }
    if (lastKnown.getRevision() > actual.getRevision()) {
      LogHelper.warning("Migrating custom fields to new revision", lastKnown.getRevision());
      return doMigrate(actual, lastKnown);
    }
    LogHelper.warning("Keeping previous custom fields. Revision:", actual.getRevision());
    return fieldsConfig;
  }

  private List<Map<TypedKey<?>, ?>> doMigrate(StoredCustomFieldConfig current, StoredCustomFieldConfig pending) {
    try {
      return migrateConfig(pending);
    } catch (FieldType.MigrationProblem migrationProblem) {
      LogHelper.debug(migrationProblem);
      myMigrationProblem = migrationProblem;
      // todo notify
      return current.getConfigs();
    }
  }

  private List<Map<TypedKey<?>, ?>> migrateConfig(StoredCustomFieldConfig pending) throws FieldType.MigrationProblem {
    FieldTypesMigration migration = FieldTypesMigration.prepare(myDrain.getReader(), CustomFieldsComponent.collectByKey(pending.getConfigs()));
    LongSet affectedConnections = new LongSet();
    List<Map<TypedKey<?>, ?>> updatedTypes = migration.perform(myDrain, affectedConnections);
    Map<String, FieldKind> kindsMap;
    try {
      kindsMap = CustomFieldsComponent.createKindsMap(false, updatedTypes);
    } catch (FieldType.CreateProblem createProblem) {
      throw new FieldType.MigrationProblem(createProblem);
    }
    RemoteMetaConfig metaConfig = new RemoteMetaConfig(kindsMap);
    DBStaticObject.transactionForceFullMaterialize(myDrain);
    for (LongIterator cursor : affectedConnections) {
      long connection = cursor.value();
      DBIdentity connectionIdentity = DBIdentity.load(myDrain.getReader(), connection);
      if (connectionIdentity == null) {
        LogHelper.error("Failed to load connection identity", connection);
        continue;
      }
      metaConfig.updateMetaInfo(myDrain, connectionIdentity);
    }
    StoredCustomFieldConfig.create(updatedTypes, pending.getRevision()).writeActual(myDrain);
    return updatedTypes;
  }
}
