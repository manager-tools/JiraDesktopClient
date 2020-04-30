package com.almworks.jira.provider3.custom.impl;

import com.almworks.dbproperties.DBPropertiesComponent;
import com.almworks.dbproperties.MapDeserializer;
import com.almworks.dbproperties.MapSerializer;
import com.almworks.dbproperties.SerializeSchema;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemProxy;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StoredCustomFieldConfig {
  private static final ItemProxy CUSTOM_FIELDS_ACTUAL = DBPropertiesComponent.createProperty("com.almworks.jira.customFieldsSchema:actual");
  private static final ItemProxy CUSTOM_FIELDS_PENDING = DBPropertiesComponent.createProperty("com.almworks.jira.customFieldsSchema:pending");

  private static final TypedKey<Integer> CUSTOM_FIELD_REVISION = TypedKey.create("sys_customFieldRevision", Integer.class);
  private static final TypedKey<List<Map<TypedKey<?>, ?>>> ALL_CONFIGS = TypedKey.<List<Map<TypedKey<?>, ?>>>create("sys_allCustomFieldConfigs", (Class)List.class);

  public static final Comparator<Map<TypedKey<?>,?>> BY_KEY_COMPARATOR = new Comparator<Map<TypedKey<?>, ?>>() {
    @Override
    public int compare(Map<TypedKey<?>, ?> o1, Map<TypedKey<?>, ?> o2) {
      if (o1 == o2)
        return 0;
      if (o1 == null || o2 == null)
        return o1 == null ? -1 : 1;
      String k1 = ConfigKeys.KEY.getFrom(o1);
      String k2 = ConfigKeys.KEY.getFrom(o2);
      if (Util.equals(k1, k2))
        return 0;
      if (k1 == null || k2 == null)
        return k1 == null ? -1 : 1;
      return k1.compareTo(k2);
    }
  };

  private final List<Map<TypedKey<?>, ?>> myConfigs;
  private final int myRevision;

  private StoredCustomFieldConfig(List<Map<TypedKey<?>, ?>> configs, int revision) {
    myConfigs = configs;
    myRevision = revision;
  }

  public List<Map<TypedKey<?>, ?>> getConfigs() {
    return myConfigs;
  }

  public int getRevision() {
    return myRevision;
  }

  public static void clearPending(DBDrain drain) {
    new StoredCustomFieldConfig(Collections15.<Map<TypedKey<?>, ?>>emptyList(), -1).writePending(drain);
  }
  
  @NotNull
  public static StoredCustomFieldConfig create(@Nullable List<Map<TypedKey<?>, ?>> configs) {
    return create(configs, -1);
  }

  @NotNull
  public static StoredCustomFieldConfig create(@Nullable List<Map<TypedKey<?>, ?>> configs, int revision) {
    if (revision >= 0 && (configs == null || configs.isEmpty())) {
      LogHelper.error("Expected not empty config for revision", revision);
      revision = -1;
    }
    if (configs == null) configs = Collections.emptyList();
    return new StoredCustomFieldConfig(configs, revision);
  }
  
  /**
   * @return stored actual configuration or null if no configuration stored or if failed to load
   */
  @Nullable
  public static StoredCustomFieldConfig loadActual(DBReader reader) {
    return load(reader, CUSTOM_FIELDS_ACTUAL);
  }

  /**
   * @return pending config change or null if no change is pending or failed to load
   */
  @Nullable
  public static List<Map<TypedKey<?>, ?>> loadPending(DBReader reader) {
    StoredCustomFieldConfig config = load(reader, CUSTOM_FIELDS_PENDING);
    if (config == null) return null;
    List<Map<TypedKey<?>, ?>> list = config.myConfigs;
    return list.isEmpty() ? null : list;
  } 

  @Nullable
  private static StoredCustomFieldConfig load(DBReader reader, ItemProxy property) {
    byte[] schemaBytes = DBPropertiesComponent.getValue(reader, property);
    if (schemaBytes == null) return null;
    Map<TypedKey<?>, ?> map = MapDeserializer.restore(schemaBytes, CustomFieldsComponent.SCHEMA.getSerializeSchema());
    if (map == null) return null;
    List<Map<TypedKey<?>, ?>> configs = ALL_CONFIGS.getFrom(map);
    if (configs == null) {
      LogHelper.error("Missing custom field config", map);
      return null;
    }
    Integer revision = CUSTOM_FIELD_REVISION.getFrom(map);
    if (revision != null && revision < 0) {
      LogHelper.error("Wrong revision", revision);
      revision = 0;
    }
    if (revision == null) revision = 0;
    return new StoredCustomFieldConfig(Collections.unmodifiableList(configs), revision);
  }

  void writePending(DBDrain drain) {
    write(drain, CUSTOM_FIELDS_PENDING, -1);
  }

  void writeActual(DBDrain drain) {
    if (myConfigs.isEmpty()) {
      LogHelper.error("Missing fields config, probably error occurred");
      return;
    }
    int revision = myRevision;
    if (revision < 0) {
      StoredCustomFieldConfig actual = loadActual(drain.getReader());
      if (actual != null) revision = actual.getRevision();
    }
    write(drain, CUSTOM_FIELDS_ACTUAL, revision);
  }

  private void write(DBDrain drain, ItemProxy property, int revision) {
    if (myConfigs.isEmpty()) {
      DBPropertiesComponent.setValue(drain, property, null);
      return;
    }
    ArrayList<Map<TypedKey<?>, ?>> copy = Collections15.arrayList(myConfigs);
    Collections.sort(copy, BY_KEY_COMPARATOR);
    HashMap<TypedKey<?>, ?> map = Collections15.hashMap();
    ALL_CONFIGS.putTo(map, copy);
    if (revision >= 0) CUSTOM_FIELD_REVISION.putTo(map, revision);
    byte[] bytes = MapSerializer.serialize(map, CustomFieldsComponent.SCHEMA.getSerializeSchema());
    if (bytes != null) DBPropertiesComponent.setValue(drain, property, bytes);
    else LogHelper.error("Failed to serialize");
  }

  @SuppressWarnings("unchecked")
  public static boolean addStoredKeys(SerializeSchema serializeSchema) {
    return serializeSchema.addListKey(ALL_CONFIGS, (Class) Map.class) && serializeSchema.addKey(CUSTOM_FIELD_REVISION);
  }
}
