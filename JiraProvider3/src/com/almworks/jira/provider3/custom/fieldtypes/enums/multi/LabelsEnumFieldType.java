package com.almworks.jira.provider3.custom.fieldtypes.enums.multi;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.gui.edit.editors.enums.multi.LabelsEditor;
import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.items.gui.meta.util.MultiEnumInfo;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.BadUtil;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.fieldtypes.JqlSearchInfo;
import com.almworks.jira.provider3.custom.fieldtypes.enums.*;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.jira.provider3.gui.JiraFields;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldDescriptor;
import com.almworks.jira.provider3.remotedata.issue.fields.LabelsFieldsDescriptor;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.download2.details.CustomFieldsSchema;
import com.almworks.jira.provider3.sync.download2.meta.CommonEnumOptions;
import com.almworks.jira.provider3.sync.download2.meta.OptionsLoader;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.jira.provider3.sync.jql.JqlEnum;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class LabelsEnumFieldType extends FieldType {
  public static final FieldKind STD_LABELS = new LabelsKind(new MultiEnumProperties("_no_option_", "No Label", true, " ", true));
  private static final EnumTypeKind.CustomEnumType LABEL_ENUM_KIND = new EnumTypeKind.CustomEnumType(CommonEnumOptions.UNORDERED);
  private static final String PREFIX = "multiLabels";

  public LabelsEnumFieldType() {
    super(CustomFieldsSchema.ID_LABELS, unite(MultiEnumProperties.KEYS, ConfigKeys.NONE_NAME));
  }

  @Override
  @NotNull
  public FieldKind createKind(Map<TypedKey<?>, ?> map) throws CreateProblem {
    return new LabelsKind(MultiEnumProperties.create(map, "_no_option_", "None"));
  }

  public static void storeLabels(EntityTransaction transaction, String fieldId, Set<String> labels) {
    EntityHolder field = ServerCustomField.getField(transaction, fieldId);
    if (field == null) return;
    Entity type = LABEL_ENUM_KIND.createType(field);
    if (type == null) return;
    EnumFieldUtils.setupMultiEnumField(field, PREFIX, true, type);
    for (String label : labels) transaction.addEntity(type, ServerCustomField.ENUM_STRING_ID, label);
  }

  private static class LabelsKind implements FieldKind {
    private final MultiEnumProperties myProperties;

    public LabelsKind(MultiEnumProperties properties) {
      myProperties = properties;
    }

    @Override
    public Field createFieldsDescriptor(String fieldId, String connectionId, String fieldName) {
      Entity type = ServerCustomField.createEnumType(connectionId, fieldId);
      String fullId = ServerCustomField.createFullId(connectionId, PREFIX, fieldId);
      EntityKey<Collection<Entity>> key = EntityKey.entityCollection(fullId, EntityKeyProperties.shadowable());
      IssueFieldDescriptor descriptor = new LabelsFieldsDescriptor(fieldId, fieldName, key, type);
      return new Field(descriptor, ServerJira.toItemType(type));
    }

    @Override
    public void migrateField(ItemVersionCreator field) throws MigrationProblem {
      MultiSelectKind.migrateField(field, PREFIX, true, LABEL_ENUM_KIND);
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
      MultiEnumInfo info = myProperties.createFieldInfo(attribute, connection, id, name, connectionIdPrefix, LABEL_ENUM_KIND, type);
      info.setDefaultDnD(id, JiraFields.SEQUENCE_EDIT_APPLICABILITY, RelevantApplicability.sequence(field.getItem(), applicability));
      info.setApplicability(applicability);
      return info;
    }

    @Override
    public JQLConvertor getJqlSearch(ItemVersion field) {
      JqlSearchInfo<?> info = JqlSearchInfo.load(field, CustomFieldsSchema.ID_LABELS);
      return info != null ? JqlEnum.generic(info.getJqlName(), info.getAttribute(), CustomField.ENUM_STRING_ID, info.getDisplayName()) : null;
    }

    @Override
    public boolean isEditable() {
      return true;
    }

    @Override
    public FieldEditor createEditor(ItemVersion field) {
      EnumEditorInfo<Set<Long>> info = EnumEditorInfo.SET.load(field);
      if (info == null) return null;
      LogHelper.assertError(info.getOverrideRenderer() == null, "Renderer overriding not supported");
      EnumItemCreator.SimpleCreator creator = new EnumItemCreator.SimpleCreator(info.getType(), CustomField.ENUM_STRING_ID);
      return new LabelsEditor(NameMnemonic.rawText(info.getName()), info.getAttribute(), info.getVariants(), creator);
    }

    @Override
    public <T> T getExtension(TypedKey<T> extensionKey) {
      if (OptionsLoader.CREATE_META.equals(extensionKey)) {
        return extensionKey.cast(MetaOptionsLoader.wrapMulti(CommonEnumOptions.UNORDERED_NO_REMOVE, LABEL_ENUM_KIND, PREFIX, isEditable()));
      }
      return null;
    }
  }
}
