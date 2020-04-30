package com.almworks.jira.provider3.custom.loadxml;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.BadUtil;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.gui.JiraFields;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldDescriptor;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.download2.details.fields.SimpleKeyValue;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.util.LogHelper;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

class UnknownFieldKind implements FieldKind {

  @Override
  public Field createFieldsDescriptor(String fieldId, String connectionId, String fieldName) {
    String fullId = ServerCustomField.createFullId(connectionId, "unknownKind", fieldId);
    EntityKey<String> key = EntityKey.scalar(fullId, String.class, null);
    IssueFieldDescriptor descriptor = new Descriptor(fieldId, fieldName, key);
    return new Field(descriptor, null);
  }

  @Override
  public void migrateField(ItemVersionCreator field) throws FieldType.MigrationProblem {
    LogHelper.error("Migration to Unknown type", field, field.getValue(CustomField.NAME), field.getValue(CustomField.ID));
    throw FieldType.MigrationProblem.internalError();
  }

  @Override
  public <T> T getExtension(TypedKey<T> extensionKey) {
    return null;
  }

  @Override
  public FieldInfo createFieldInfo(ItemVersion field, String id, String name, DBAttribute<?> a, DBIdentity connection, String connectionIdPrefix,
    ScalarSequence applicability)
  {
    final DBAttribute<String> attribute = BadUtil.castScalar(String.class, a);
    if (attribute == null) {
      LogHelper.error("Wrong attribute", a, id, name, a.getScalarClass(), a.getComposition(), field.getValue(CustomField.KEY));
      return null;
    }
    return JiraFields.shortText(ItemDownloadStage.QUICK, true)
      .setMultiline(false)
      .setHideEmptyLeftField(true)
      .setColumnId(connectionIdPrefix + id)
      .setAttribute(attribute)
      .setOwner(connection)
      .setId(id)
      .setConstraintId(name != null ? name : id)
      .setDisplayName(name)
      .setApplicability(applicability);
  }

  @Override
  public boolean isEditable() {
    return false;
  }

  @Override
  public FieldEditor createEditor(ItemVersion field) {
    return null;
  }

  @Override
  public JQLConvertor getJqlSearch(ItemVersion field) {
    return null;
  }

  private static class Descriptor extends IssueFieldDescriptor implements JsonIssueField {
    private final EntityKey<String> myKey;

    protected Descriptor(String fieldId, @Nullable String displayName, EntityKey<String> key) {
      super(fieldId, displayName);
      myKey = key;
    }

    @NotNull
    @Override
    public EntityKey<?> getIssueEntityKey() {
      return myKey;
    }

    @Override
    public JsonIssueField createDownloadField() {
      return this;
    }

    @Override
    public Collection<? extends ParsedValue> loadValue(@Nullable Object jsonValue) {
      String text = Util.castNullable(String.class, jsonValue);
      if (text != null) {  // todo JCO-1431 Find all custom-fields when jsonValue != null && text == null
        text = text.trim();
        if (text.isEmpty()) text = null;
      }
      return SimpleKeyValue.single(myKey, text);
    }

    @Override
    public Collection<? extends ParsedValue> loadNull() {
      return SimpleKeyValue.single(myKey, null);
    }

    @Override
    public IssueFieldValue load(ItemVersion trunk, ItemVersion base) {
      return null; // No upload for unknown fields
    }
  }
}
