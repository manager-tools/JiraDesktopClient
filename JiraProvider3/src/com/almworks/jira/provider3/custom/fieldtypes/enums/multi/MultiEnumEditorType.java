package com.almworks.jira.provider3.custom.fieldtypes.enums.multi;

import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.sync.ItemVersion;

interface MultiEnumEditorType {
  FieldEditor createEditor(ItemVersion field);
}
