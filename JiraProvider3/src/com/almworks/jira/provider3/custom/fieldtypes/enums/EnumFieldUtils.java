package com.almworks.jira.provider3.custom.fieldtypes.enums;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.gui.meta.schema.dnd.DnDChange;
import com.almworks.items.gui.meta.util.BaseEnumInfo;
import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class EnumFieldUtils {
  public static void migrateField(ItemVersionCreator field, String prefix, boolean editable, Entity enumType) throws FieldType.MigrationProblem {
    DBItemType dbType = ServerJira.toItemType(enumType);
    if (field == null || enumType == null || prefix == null || dbType == null) {
      LogHelper.error("Missing value", field, enumType, prefix, dbType);
      throw FieldType.MigrationProblem.internalError();
    }
    Pair<String, String> pair = FieldType.getConnectionFieldIds(field);
    String connectionId = pair.getFirst();
    String fieldId = pair.getSecond();
    String fullId = ServerCustomField.createFullId(connectionId, prefix, fieldId);
    EntityKey<Entity> key = EntityKey.entity(fullId, editable ? EntityKeyProperties.shadowable() : null);
    DBAttribute<?> attribute = FieldType.MigrationProblem.ensureCanMigrateAttribute(field, ServerJira.createAttributeInfo(key));
    field.setValue(CustomField.ATTRIBUTE, attribute);
    field.setValue(CustomField.ENUM_TYPE, dbType);
  }

  @Nullable
  public static EntityKey<Entity> setupSingleEnumField(EntityHolder field, String prefix, boolean editable, Entity enumType) {
    if (enumType == null) return null;
    final String fullId = ServerCustomField.createFullId(field, prefix);
    if (fullId == null) return null;
    EntityKey<Entity> key = EntityKey.entity(fullId, editable ? EntityKeyProperties.shadowable() : null);
    field.setValue(ServerCustomField.ATTRIBUTE, key.toEntity());
    field.setValue(ServerCustomField.ENUM_TYPE, enumType);
    return key;
  }

  public static FieldInfo readOnlyDnD(String fieldName, BaseEnumInfo<?> info, FieldInfo toOverride) {
    return FieldInfo.OverrideDnD.override(toOverride, DnDChange.createStaticNoChange(info.getOwner(), info.getAttribute(), info.createModelKey(), fieldName + " is not editable"));
  }

  @Nullable
  public static EntityKey<Collection<Entity>> setupMultiEnumField(EntityHolder field, String prefix, boolean editable, Entity enumType) {
    String fullId = ServerCustomField.createFullId(field, prefix);
    if (fullId == null) return null;
    EntityKey<Collection<Entity>> key = EntityKey.entityCollection(fullId, editable ? EntityKeyProperties.shadowable() : null);
    field.setValue(ServerCustomField.ATTRIBUTE, key.toEntity());
    field.setValue(ServerCustomField.ENUM_TYPE, enumType);
    return key;
  }
}
