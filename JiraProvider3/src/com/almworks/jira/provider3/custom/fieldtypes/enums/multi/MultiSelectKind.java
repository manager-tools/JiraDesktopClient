package com.almworks.jira.provider3.custom.fieldtypes.enums.multi;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.items.gui.meta.util.MultiEnumInfo;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.BadUtil;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.fieldtypes.ConvertorFactory;
import com.almworks.jira.provider3.custom.fieldtypes.enums.*;
import com.almworks.jira.provider3.gui.JiraFields;
import com.almworks.jira.provider3.remotedata.issue.fields.EntityType;
import com.almworks.jira.provider3.remotedata.issue.fields.MultiEntityDescriptor;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.download2.meta.OptionsLoader;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.TypedKey;

import java.util.Collection;
import java.util.Set;

class MultiSelectKind implements FieldKind {
  private final EnumKind myEnumInfo;
  private final String myPrefix;
  private final MultiEnumProperties myProperties;
  private final ConvertorFactory myConstraintFactory;
  private final MultiEnumEditorType myEditorType;

  public MultiSelectKind(EnumKind enumInfo, String prefix, MultiEnumProperties properties, ConvertorFactory constraintFactory, MultiEnumEditorType editorType) {
    myEnumInfo = enumInfo;
    myPrefix = prefix;
    myProperties = properties;
    myConstraintFactory = constraintFactory;
    myEditorType = editorType;
  }

  @Override
  public Field createFieldsDescriptor(String fieldId, String connectionId, String fieldName) {
    String fullId = ServerCustomField.createFullId(connectionId, myPrefix, fieldId);
    EntityKey<Collection<Entity>> key = EntityKey.entityCollection(fullId, isEditable() ? EntityKeyProperties.shadowable() : null);
    EntityType<?> entityType = myEnumInfo.createEnumType(fieldId, connectionId);
    MultiEntityDescriptor descriptor = MultiEntityDescriptor.create(fieldId, fieldName, key, entityType);
    return new Field(descriptor, entityType.getType());
  }

  @Override
  public void migrateField(ItemVersionCreator field) throws FieldType.MigrationProblem {
    migrateField(field, myPrefix, isEditable(), myEnumInfo.getEnumTypeKind());
  }

  public static void migrateField(ItemVersionCreator field, String prefix, boolean editable, EnumTypeKind enumInfo) throws FieldType.MigrationProblem {
    Pair<String, String> pair = FieldType.getConnectionFieldIds(field);
    String connectionId = pair.getFirst();
    String fieldId = pair.getSecond();
    String fullId = ServerCustomField.createFullId(connectionId, prefix, fieldId);
    EntityKey<Collection<Entity>> key = EntityKey.entityCollection(fullId, editable ? EntityKeyProperties.shadowable() : null);
    DBAttribute<?> attribute = FieldType.MigrationProblem.ensureCanMigrateAttribute(field, ServerJira.createAttributeInfo(key));
    Entity type = enumInfo.createType(connectionId, fieldId);
    if (type == null) {
      LogHelper.error("No enum type", connectionId, fieldId);
      throw FieldType.MigrationProblem.internalError();
    }
    field.setValue(CustomField.ATTRIBUTE, attribute);
    field.setValue(CustomField.ENUM_TYPE, ServerJira.toItemType(type));
  }

  @Override
  public FieldInfo createFieldInfo(ItemVersion field, String id, String name, DBAttribute<?> a, DBIdentity connection, String connectionIdPrefix,
    ScalarSequence applicability)
  {
    DBAttribute<Set<Long>> attribute = BadUtil.castSetAttribute(Long.class, a);
    Long typeItem = field.getValue(CustomField.ENUM_TYPE);
    DBItemType type = BadUtil.getItemType(field.getReader(), typeItem);
    if (attribute == null || type == null) {
      LogHelper.error("Wrong attribute", a, id, name, typeItem, type);
      return null;
    }
    MultiEnumInfo info = myProperties.createFieldInfo(attribute, connection, id, name, connectionIdPrefix, myEnumInfo.getEnumTypeKind(), type);
    FieldInfo result = info;
    info.setApplicability(applicability);
    if (isEditable()) info.setDefaultDnD(id, JiraFields.SEQUENCE_EDIT_APPLICABILITY, RelevantApplicability.sequence(field.getItem(), applicability));
    else result = EnumFieldUtils.readOnlyDnD(name, info, result);
    return result;
  }

  @Override
  public JQLConvertor getJqlSearch(ItemVersion field) {
    return myConstraintFactory.createJql(field);
  }

  @Override
  public boolean isEditable() {
    return myEditorType != null;
  }

  @Override
  public FieldEditor createEditor(ItemVersion field) {
    return myEditorType != null ? myEditorType.createEditor(field) : null;
  }

  @Override
  public <T> T getExtension(TypedKey<T> extensionKey) {
    if (OptionsLoader.CREATE_META.equals(extensionKey)) {
      OptionsLoader<?> loader = myEnumInfo.getEnumTypeKind().getExtension(OptionsLoader.CREATE_META);
      return extensionKey.cast(MetaOptionsLoader.wrapMulti(loader, myEnumInfo.getEnumTypeKind(), myPrefix, isEditable()));
    }
    return myEnumInfo.getEnumTypeKind().getExtension(extensionKey);
  }
}
