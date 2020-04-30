package com.almworks.jira.provider3.custom;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldDescriptor;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

public interface FieldKind {
  /**
   * Creates {@link FieldInfo meta info package} for the custom field. May return null if the field has no any meta info (cannot hold standard visible data)
   * @param field custom field
   * @param id field id
   * @param name field display name
   * @param attribute field attribute
   * @param connection field connection
   * @param connectionIdPrefix connection id prefix
   * @param applicability field applicability
   * @return standard field meta info package
   */
  @Nullable
  FieldInfo createFieldInfo(ItemVersion field, String id, String name, DBAttribute<?> attribute, DBIdentity connection, String connectionIdPrefix, ScalarSequence applicability);

  /**
   * Creates full fields description.
   *
   * @param fieldId custom field ID (like "customfield_NNNN")
   * @param connectionId field's connection ID
   * @param fieldName not null if field name is known
   * @return field object or null if the field is not supported
   */
  Field createFieldsDescriptor(String fieldId, String connectionId, @Nullable String fieldName);

  /**
   * @return true if the field may has editor (and value may be changed)
   */
  boolean isEditable();

  /**
   * Creates editor for the field if edit is possible. If {@link #isEditable()} returns false this method should return null.
   * @param field field to be edited
   * @return field editor or null if field value cannot be changed
   */
  @Nullable
  FieldEditor createEditor(ItemVersion field);

  /**
   * Update custom field item to store values of the field kind.<br>
   * When a field changes kind previous field attributes ({@link com.almworks.jira.provider3.schema.CustomField#ATTRIBUTE issue attribute} and
   * {@link com.almworks.jira.provider3.schema.CustomField#ENUM_TYPE custom enum type}) may be has to be changed. This method performs the change
   * @param field custom field
   * @throws FieldType.MigrationProblem if migration from current custom field kind (and values) is not possible
   */
  void migrateField(ItemVersionCreator field) throws FieldType.MigrationProblem;

  /**
   * @return JQL constraint convertor. May return null if JQL is not supported for the field kind.
   */
  @Nullable
  JQLConvertor getJqlSearch(ItemVersion field);

  /**
   * Allows to extend the interface for implementation which has to provide special facilities.
   * @param extensionKey extension selector
   * @param <T> extension class
   * @return extension of the FieldKind interface if the implementation supports it
   */
  @Nullable
  public <T> T getExtension(TypedKey<T> extensionKey);

  class Field {
    private final IssueFieldDescriptor myDescriptor;
    @Nullable
    private final Entity myEnumType;
    private JsonIssueField myField; // Just a cache, needs no synchronization

    public Field(IssueFieldDescriptor descriptor, @Nullable DBItemType enumType) {
      myDescriptor = descriptor;
      myEnumType = ServerJira.dbTypeToEntity(enumType);
    }

    public void setupField(EntityTransaction transaction, String fieldClass) {
      EntityHolder fieldEntity = ServerCustomField.getField(transaction, myDescriptor.getFieldId());
      if (fieldEntity == null) return;
      EntityKey<?> key = myDescriptor.getIssueEntityKey();
      fieldEntity.setValue(ServerCustomField.ATTRIBUTE, key.toEntity());
      fieldEntity.setValue(ServerCustomField.ENUM_TYPE, myEnumType);
      fieldEntity.setNNValue(ServerCustomField.KEY, fieldClass);
      if (myDescriptor.hasDisplayName()) fieldEntity.setNNValue(ServerCustomField.NAME, myDescriptor.getDisplayName());
    }

    public IssueFieldDescriptor getDescriptor() {
      return myDescriptor;
    }

    public JsonIssueField getIssueField() {
      if (myField == null) myField = myDescriptor.createDownloadField();
      return myField;
    }
  }
}
