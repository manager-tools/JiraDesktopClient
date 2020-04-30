package com.almworks.jira.provider3.custom.impl;

import com.almworks.items.api.*;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.fieldtypes.UnsupportedFieldType;
import com.almworks.jira.provider3.custom.fieldtypes.enums.RelevantApplicability;
import com.almworks.jira.provider3.custom.fieldtypes.enums.cascade.CascadeKind;
import com.almworks.jira.provider3.custom.fieldtypes.enums.multi.LabelsEnumFieldType;
import com.almworks.jira.provider3.custom.fieldtypes.enums.multi.MultiEnumFieldType;
import com.almworks.jira.provider3.custom.fieldtypes.enums.single.SingleEnumFieldType;
import com.almworks.jira.provider3.custom.fieldtypes.scalars.DateFieldType;
import com.almworks.jira.provider3.custom.fieldtypes.scalars.DecimalFieldType;
import com.almworks.jira.provider3.custom.fieldtypes.scalars.TextFieldType;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.jira.provider3.custom.loadxml.FieldKeysLoader;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.properties.Role;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import java.util.*;

public class CustomFieldsComponent implements Startable, GuiFeaturesManager.Provider {
  public static final Role<CustomFieldsComponent> ROLE = Role.role(CustomFieldsComponent.class);

  private static final FieldType SINGLE_ENUM = new SingleEnumFieldType();
  private static final FieldType MULTI_ENUM = new MultiEnumFieldType();
  private static final FieldType LABELS = new LabelsEnumFieldType();
  private static final FieldType TEXT = new TextFieldType();
  private static final FieldType DECIMAL = new DecimalFieldType();
  private static final FieldType DATE = new DateFieldType();

  public static final List<FieldType> ALL_FIELD_TYPES = Collections15.unmodifiableListCopy(SINGLE_ENUM, MULTI_ENUM, LABELS, CascadeKind.CASCADE_TYPE, TEXT, DECIMAL, DATE, UnsupportedFieldType.UNSUPPORTED);
  public static final TypeConfigSchema SCHEMA;
  static {
    SCHEMA = TypeConfigSchema.create(ALL_FIELD_TYPES);
  }

  private final Database myDB;

  private final Map<String, FieldKind> myFieldKinds = Collections15.hashMap();

  public CustomFieldsComponent(Database db) {
    myDB = db;
  }

  public static boolean collectByKey(List<Map<TypedKey<?>, ?>> list, Map<String, Map<TypedKey<?>, ?>> result) {
    for (Map<TypedKey<?>, ?> map : list) {
      String key = ConfigKeys.KEY.getFrom(map);
      if (key == null) {
        LogHelper.error("Missing key", map);
        return false;
      }
      Map<TypedKey<?>, ?> prev = result.put(key, map);
      if (prev != null) {
        LogHelper.error("Duplicated key", key);
        return false;
      }
    }
    return true;
  }

  @Nullable
  public static Map<String, Map<TypedKey<?>, ?>> collectByKey(List<Map<TypedKey<?>, ?>> list) {
    HashMap<String,Map<TypedKey<?>,?>> result = Collections15.hashMap();
    return !collectByKey(list, result) ? null : result;
  }

  public RemoteMetaConfig createIssueConversion() {
    Map<String, FieldKind> kinds;
    kinds = getCustomFieldKinds();
    return new RemoteMetaConfig(kinds);
  }

  public Map<String, FieldKind> getCustomFieldKinds() {
    Map<String, FieldKind> kinds;
    synchronized (myFieldKinds) {
      kinds = Collections15.hashMap(myFieldKinds);
    }
    return kinds;
  }

  public static void registerFeature(FeatureRegistry registry) {
    RelevantApplicability.registerFeature(registry);
  }

  /**
   * This service implements {@link GuiFeaturesManager.Provider feature provide interface} to block {@link GuiFeaturesManager feature manager} start up before meta data is updated in DB.
   */
  @Override
  public void registerFeatures(FeatureRegistry featureRegistry) {
  }

  @Override
  public void start() {
    myDB.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        Map<String, FieldKind> kindsMap = new RestoreTypesConfigMethod(writer).perform();
        synchronized (myFieldKinds) {
          myFieldKinds.putAll(kindsMap);
        }
        return null;
      }
    }).waitForCompletion();
  }

  @Override
  public void stop() {
  }

  @NotNull
  public List<Map<TypedKey<?>, ?>> loadDefaultFieldTypes() {
    FieldKeysLoader loader;
    try {
      loader = FieldKeysLoader.load("/com/almworks/jira/provider3/customFields.xml", SCHEMA);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      LogHelper.error(e);
      return Collections.emptyList();
    }
    return loader.getLoadedKinds();
  }

  public static Map<String, FieldKind> createKindsMap(boolean proceedOnErrors, List<Map<TypedKey<?>, ?>> loaded) throws FieldType.CreateProblem {
    HashMap<String,FieldKind> result = Collections15.hashMap();
    for (Map<TypedKey<?>, ?> map : loaded) {
      String key = ConfigKeys.KEY.getFrom(map);
      FieldKind kind;
      try {
        kind = createKind(map, SCHEMA);
      } catch (FieldType.CreateProblem e) {
        if (!proceedOnErrors) throw e;
        LogHelper.error("Failed to create field", key, ConfigKeys.TYPE.getFrom(map), e);
        continue;
      }
      FieldKind prev = result.put(key, kind);
      LogHelper.assertError(prev == null, "Duplicated field info", key, ConfigKeys.TYPE.getFrom(map));
    }
    return result;
  }

  @NotNull
  public static FieldKind createKind(Map<TypedKey<?>, ?> map, TypeConfigSchema schema) throws FieldType.CreateProblem {
    String typeName = ConfigKeys.TYPE.getFrom(map);
    FieldType fieldType = schema.getTypeByName(typeName);
    if (fieldType == null) throw new FieldType.CreateProblem("Unknown field type \"" + typeName + "\"");
    return fieldType.createKind(map);
  }

  @Nullable
  public JQLConvertor getJQLSearch(ItemVersion field) {
    String key = field.getValue(CustomField.KEY);
    if (key == null) {
      LogHelper.error("Missing key", field);
      return null;
    }
    FieldKind kind;
    synchronized (myFieldKinds) {
      kind = myFieldKinds.get(key);
    }
    return kind != null ? kind.getJqlSearch(field) : null;
  }

  @Nullable
  public FieldEditor createFieldEditor(ItemVersion field) {
    FieldKind kind = getFieldKind(field);
    return kind != null ? kind.createEditor(field) : null;
  }

  public boolean isEditable(ItemVersion field) {
    FieldKind kind = getFieldKind(field);
    return kind != null && kind.isEditable();
  }

  @Nullable
  public FieldKind getFieldKind(String atlasClass) {
    if (atlasClass == null) return null;
    synchronized (myFieldKinds) {
      return myFieldKinds.get(atlasClass);
    }
  }

  @Nullable
  private FieldKind getFieldKind(ItemVersion field) {
    if (field == null) return null;
    String key = field.getValue(CustomField.KEY);
    return getFieldKind(key);
  }
  
  public void updateFields(SyncManager syncManager, final List<Map<TypedKey<?>, ?>> update, final Procedure<FieldType.MigrationProblem> callback) throws FieldType.CreateProblem {
    createKindsMap(false, update); // Check config
    syncManager.writeDownloaded(new DownloadProcedure<DBDrain>() {
      private FieldType.MigrationProblem myMigrationProblem;

      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        List<Map<TypedKey<?>, ?>> updateConfigsList;
        try {
          List<Map<TypedKey<?>, ?>> pendingConfigList = StoredCustomFieldConfig.loadPending(drain.getReader());
          Map<String, Map<TypedKey<?>, ?>> pendingConfigs = pendingConfigList != null ? collectByKey(pendingConfigList) : null;
          pendingConfigs = Collections15.hashMap(pendingConfigs);
          Map<String, Map<TypedKey<?>, ?>> updateConfigs = collectByKey(update);
          if (updateConfigs == null) {
            LogHelper.error("Nothing to update");
            return;
          }
          pendingConfigs.putAll(updateConfigs);
          FieldTypesMigration migration = FieldTypesMigration.prepare(drain.getReader(), updateConfigs);
          updateConfigsList = Collections15.arrayList();
          updateConfigsList.addAll(migration.getChanged().values());
          updateConfigsList.addAll(migration.getAdded().values());
        } catch (FieldType.MigrationProblem migrationProblem) {
          myMigrationProblem = migrationProblem;
          // todo notify
          throw new DBOperationCancelledException();
        }
        StoredCustomFieldConfig.create(updateConfigsList).writePending(drain);
      }

      @Override
      public void onFinished(DBResult<?> result) {
        if (myMigrationProblem != null) {
          LogHelper.debug(myMigrationProblem);
          callback.invoke(myMigrationProblem);
          return;
        }
        if (callback != null) callback.invoke(null);
      }
    });
  }

  /**
   * Merges maps from source to target. Replaces maps {@link ConfigKeys#KEY with same key}. All replaced are removed from source, new maps are left in source.
   * @param source merge from. Must support remove
   * @param target merge to. Must accept set to replace previous map with updated
   */
  public static void mergeTypes(ArrayList<Map<TypedKey<?>, ?>> source, ArrayList<Map<TypedKey<?>, ?>> target) {
    Map<String, Integer> indexes = Collections15.hashMap();
    for (int i = 0; i < target.size(); i++) {
      Map<TypedKey<?>, ?> map = target.get(i);
      String key = ConfigKeys.KEY.getFrom(map);
      if (key == null) LogHelper.error("Missing key");
      else indexes.put(key, i);
    }
    for (Iterator<Map<TypedKey<?>, ?>> iterator = source.iterator(); iterator.hasNext(); ) {
      Map<TypedKey<?>, ?> map = iterator.next();
      String key = ConfigKeys.KEY.getFrom(map);
      if (key == null) LogHelper.error("Missing key");
      else {
        Integer index = indexes.get(key);
        if (index == null) continue;
        target.set(index, map);
      }
      iterator.remove();
    }
  }
}