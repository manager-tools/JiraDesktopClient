package com.almworks.jira.provider3.custom.fieldtypes;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * This class represent a custom field type that is surely unsupported. Unlike {@link com.almworks.jira.provider3.custom.loadxml.FieldKeysLoader#UNKNOWN unknown} type which may have
 * meaningful value (but the application does not known it type yet) the fields of this type sure does not have meaningful value (or no value at all).<br>
 * This type is used to remove all meta info for the field that never has any value.
 */
public class UnsupportedFieldType extends FieldType {
  public static final FieldType UNSUPPORTED = new UnsupportedFieldType();

  private static final FieldKind UNSUPPORTED_KIND = new FieldKind() {
    @Override
    public FieldInfo createFieldInfo(ItemVersion field, String id, String name, DBAttribute<?> attribute, DBIdentity connection, String connectionIdPrefix,
      ScalarSequence applicability)
    {
      return null;
    }

    @Override
    public Field createFieldsDescriptor(String fieldId, String connectionId, String fieldName) {
      return null;
    }

    @Override
    public JQLConvertor getJqlSearch(ItemVersion field) {
      return null;
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
    public void migrateField(ItemVersionCreator field) throws MigrationProblem {
      field.setValue(CustomField.ATTRIBUTE, (Long) null);
      field.setValue(CustomField.ENUM_TYPE, (Long) null);
    }

    @Override
    public <T> T getExtension(TypedKey<T> extensionKey) {
      return null;
    }
  };

  private UnsupportedFieldType() {
    super("unsupported");
  }

  @NotNull
  @Override
  public FieldKind createKind(Map<TypedKey<?>, ?> map) throws CreateProblem {
    return UNSUPPORTED_KIND;
  }
}
