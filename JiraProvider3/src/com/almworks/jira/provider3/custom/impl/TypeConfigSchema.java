package com.almworks.jira.provider3.custom.impl;

import com.almworks.dbproperties.SerializeSchema;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.loadxml.ConfigKey;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TypeConfigSchema {
  @SuppressWarnings("unchecked")
  private static final Set<TypedKey<?>> COMMON_KEYS = Collections15.<TypedKey<?>>unmodifiableSetCopy(ConfigKeys.KEY, ConfigKeys.TYPE, ConfigKeys.PREFIX);
  private final ArrayList<FieldType> myTypes;
  private final List<ConfigKey<?>> myConfigKeys;
  private final SerializeSchema mySerializeSchema;

  private TypeConfigSchema(ArrayList<FieldType> types, HashSet<ConfigKey<?>> configKeys, SerializeSchema serializeSchema) {
    myTypes = types;
    myConfigKeys = Collections15.unmodifiableListCopy(configKeys);
    mySerializeSchema = serializeSchema;
  }

  public static TypeConfigSchema create(List<FieldType> types) {
    ArrayList<FieldType> allTypes = Collections15.arrayList();
    HashSet<ConfigKey<?>> allConfigKeys = Collections15.hashSet();
    SerializeSchema serializeSchema = new SerializeSchema();
    for (FieldType type : types) {
      List<TypedKey<?>> keys = type.getKeys();
      if (!addKeys(serializeSchema, keys)) continue;
      ArrayList<ConfigKey<?>> configKeys = collectConfigKeys(keys);
      if (configKeys == null) {
        LogHelper.error("Failed to support type", type.getType());
        continue;
      }
      FieldType prev = FieldType.find(allTypes, type.getType());
      if (prev != null) {
        LogHelper.error("Duplicated type", type.getType());
        continue;
      }
      allTypes.add(type);
      allConfigKeys.addAll(configKeys);
    }
    ArrayList<ConfigKey<?>> common = collectConfigKeys(COMMON_KEYS);
    if (common != null) allConfigKeys.addAll(common);
    if (!addKeys(serializeSchema, COMMON_KEYS)
      || !addKeys(serializeSchema, Arrays.asList(ConfigKeys.EDITABLE, ConfigKeys.EDITOR, ConfigKeys.UPLOAD, ConfigKeys.UPLOAD_JSON))
      || !StoredCustomFieldConfig.addStoredKeys(serializeSchema))
      LogHelper.error("Failed to register all configs key");
    return new TypeConfigSchema(allTypes, allConfigKeys, serializeSchema);
  }

  private static boolean addKeys(SerializeSchema schema, Collection<? extends TypedKey<?>> keys) {
    for (TypedKey<?> key : keys) {
      if (schema.isKnownKey(key)) continue;
      if (!schema.addKey(key)) return false;
    }
    return true;
  }

  @Nullable
  private static ArrayList<ConfigKey<?>> collectConfigKeys(Collection<TypedKey<?>> typedKeys) {
    ArrayList<ConfigKey<?>> configKeys = Collections15.arrayList();
    for (TypedKey<?> typedKey : typedKeys) {
      ConfigKey configKey = ConfigKey.create(typedKey);
      if (configKey == null) {
        configKeys = null;
        break;
      }
      configKeys.add(configKey);
    }
    return configKeys;
  }

  public FieldType getTypeByName(String typeName) {
    return FieldType.find(myTypes, typeName);
  }

  public List<ConfigKey<?>> getAllConfigKeys() {
    return myConfigKeys;
  }

  public boolean isCommonKey(TypedKey<?> typedKey) {
    return COMMON_KEYS.contains(typedKey);
  }

  public SerializeSchema getSerializeSchema() {
    return mySerializeSchema;
  }
}
