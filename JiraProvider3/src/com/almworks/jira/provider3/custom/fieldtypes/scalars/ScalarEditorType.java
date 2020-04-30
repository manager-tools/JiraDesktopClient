package com.almworks.jira.provider3.custom.fieldtypes.scalars;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.BadUtil;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.util.text.NameMnemonic;

public abstract class ScalarEditorType<T> {
  private final Class<T> myScalarClass;

  public ScalarEditorType(Class<T> scalarClass) {
    myScalarClass = scalarClass;
  }

  public FieldEditor createEditor(ItemVersion field) {
    final DBAttribute<T> attribute = readScalarAttr(field, myScalarClass);
    if(attribute == null) {
      return null;
    }

    final String name = readName(field);
    if(name == null) {
      return null;
    }

    return createEditor(NameMnemonic.rawText(name), attribute);
  }

  protected abstract FieldEditor createEditor(NameMnemonic name, DBAttribute<T> attribute);

  private static <T> DBAttribute<T> readScalarAttr(ItemVersion field, Class<T> scalarClass) {
    final DBAttribute<?> rawAttr = BadUtil.getAttribute(field.getReader(), field.getValue(CustomField.ATTRIBUTE));
    if(rawAttr == null) {
      return null;
    }
    return BadUtil.castScalar(scalarClass, rawAttr);
  }

  private static String readName(ItemVersion field) {
    final String id = field.getValue(CustomField.ID);
    final String name = field.getValue(CustomField.NAME);
    return name == null ? id : name;
  }
}
