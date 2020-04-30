package com.almworks.jira.provider3.gui.edit;

import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Assigns specific editor to field.<br>
 * This configuration is required due to some fields can be edited by several editor kinds (with different features)<br>
 * Configuration supports inheritance: child configuration overrides ancestor's editors.
 */
public class EditorsScheme {
  @Nullable
  private final EditorsScheme myParent;
  private final Map<ServerFields.Field, FieldEditor> myEditors = Collections15.hashMap();
  private boolean myFixed = false;

  public EditorsScheme(@Nullable EditorsScheme parent) {
    myParent = parent;
  }

  public EditorsScheme addEditor(ServerFields.Field field, FieldEditor editor) {
    if (editor == null) {
      LogHelper.error("Null field editor", field);
      return this;
    }
    if (myEditors.containsKey(field)) {
      LogHelper.error("Redefinition of an editor", field, editor, myEditors.get(field));
      return this;
    }
    return priPutEditor(field, editor);
  }

  public EditorsScheme removeEditor(ServerFields.Field field) {
    return priPutEditor(field, null);
  }

  private EditorsScheme priPutEditor(ServerFields.Field field, FieldEditor editor) {
    if (field == null) {
      LogHelper.error("Null field", editor);
      return this;
    }
    if (myFixed) {
      LogHelper.error("Already fixed", field, editor);
      return this;
    }
    myEditors.put(field, editor);
    return this;
  }

  public EditorsScheme fix() {
    myFixed = true;
    return this;
  }

  @Nullable
  public FieldEditor getEditor(ServerFields.Field field) {
    if (!myEditors.containsKey(field)) return myParent != null ? myParent.getEditor(field) : null;
    return myEditors.get(field);
  }

  public FieldEditor getEditor(JiraConnection3 connection, ItemVersion field) {
    for (Map.Entry<ServerFields.Field, FieldEditor> entry : myEditors.entrySet()) if (SyncUtils.equalValue(field.getReader(), field.getItem(), entry.getKey().getDBField())) return entry.getValue();
    if (myParent != null) return myParent.getEditor(connection, field);
    return connection.getCustomFields().createFieldEditor(field);
  }
}
