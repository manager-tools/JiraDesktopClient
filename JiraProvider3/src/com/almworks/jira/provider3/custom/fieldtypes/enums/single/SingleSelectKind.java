package com.almworks.jira.provider3.custom.fieldtypes.enums.single;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.items.gui.meta.util.SingleEnumInfo;
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
import com.almworks.jira.provider3.remotedata.issue.fields.EntityFieldDescriptor;
import com.almworks.jira.provider3.remotedata.issue.fields.EntityType;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.download2.meta.OptionsLoader;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Function2;
import org.almworks.util.TypedKey;

class SingleSelectKind implements FieldKind {
  private final EnumKind myEnumInfo;
  private final String myAttributeTypePrefix;
  private final String myConstraintNullId;
  private final String myConstraintNullName;
  private final ConvertorFactory myConstraintFactory;
  private final SingleEnumEditorType myEditorType;
  private final EnumItemCreator myCreator;
  private final Function2<Pair<?,String>,String,?> myToJson;

  public SingleSelectKind(EnumKind enumInfo, String constraintNullId, String constraintNullName, String attributeTypePrefix,
    ConvertorFactory constraintFactory, SingleEnumEditorType editorType, EnumItemCreator creator, Function2<Pair<?, String>, String, ?> toJson) {
    if (editorType == null) {
      LogHelper.assertError(toJson == null, "Upload of not editable field", attributeTypePrefix, toJson);
      toJson = null;
    }
    myEnumInfo = enumInfo;
    myConstraintNullId = constraintNullId;
    myConstraintNullName = constraintNullName;
    myAttributeTypePrefix = attributeTypePrefix;
    myConstraintFactory = constraintFactory;
    myEditorType = editorType;
    myCreator = myEditorType != null ? creator : null;
    myToJson = toJson;
  }

  @Override
  public FieldInfo createFieldInfo(ItemVersion field, String id, String name, DBAttribute<?> a, DBIdentity connection, String connectionIdPrefix,
    ScalarSequence applicability)
  {
    DBAttribute<Long> attribute = BadUtil.castScalar(Long.class, a);
    Long typeItem = field.getValue(CustomField.ENUM_TYPE);
    DBItemType type = BadUtil.getItemType(field.getReader(), typeItem);
    if (attribute == null || type == null) {
      LogHelper.error("Wrong attribute", a, id, name, typeItem, type);
      return null;
    }
    SingleEnumInfo info = JiraFields.singleEnum(ItemDownloadStage.QUICK)
      .setAttribute(attribute)
      .setOwner(connection)
      .setId(id)
      .setConstraintId(name != null ? name : id)
      .setColumnId(connectionIdPrefix + id)
      .setDisplayName(name)
      .setHideEmptyLeftField(true)
      .setNullableEnum(myConstraintNullId, myConstraintNullName);
    FieldInfo result = info;
    myEnumInfo.setEnumType(info, type);
    info.setApplicability(applicability);
    if (isEditable()) info.setDefaultDnD(id, JiraFields.SEQUENCE_EDIT_APPLICABILITY, RelevantApplicability.sequence(field.getItem(), applicability));
    else result = EnumFieldUtils.readOnlyDnD(name, info, result);
    return result;
  }

  @Override
  public Field createFieldsDescriptor(String fieldId, String connectionId, String fieldName) {
    String fullId = ServerCustomField.createFullId(connectionId, myAttributeTypePrefix, fieldId);
    boolean editable = isEditable();
    EntityKey<Entity> key = EntityKey.entity(fullId, editable ? EntityKeyProperties.shadowable() : null);
    EntityType<?> type = myEnumInfo.createEnumType(fieldId, connectionId);
    EntityFieldDescriptor<?> descriptor =
      editable ? EntityFieldDescriptor.customJson(fieldId, fieldName, key, type, myToJson) : EntityFieldDescriptor.special(fieldId, fieldName, key, type, myToJson);
    return new Field(descriptor, type.getType()) ;
  }

  @Override
  public void migrateField(ItemVersionCreator field) throws FieldType.MigrationProblem {
    Pair<String, String> pair = FieldType.getConnectionFieldIds(field);
    String connectionId = pair.getFirst();
    String fieldId = pair.getSecond();
    EnumFieldUtils.migrateField(field, myAttributeTypePrefix, isEditable(), myEnumInfo.createEnumType(connectionId, fieldId).getType());
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
    return myEditorType == null ? null : myEditorType.createEditor(field, myCreator);
  }

  @Override
  public <T> T getExtension(TypedKey<T> extensionKey) {
    EnumTypeKind enumTypeKind = myEnumInfo.getEnumTypeKind();
    if (OptionsLoader.CREATE_META.equals(extensionKey)) {
      OptionsLoader<?> loader = enumTypeKind.getExtension(OptionsLoader.CREATE_META);
      return extensionKey.cast(MetaOptionsLoader.wrapSingle(loader, enumTypeKind, myAttributeTypePrefix, isEditable()));
    }
    return enumTypeKind.getExtension(extensionKey);
  }
}
