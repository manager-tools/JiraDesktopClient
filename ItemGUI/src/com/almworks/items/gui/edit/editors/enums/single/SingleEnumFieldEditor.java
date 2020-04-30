package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;

public interface SingleEnumFieldEditor extends FieldEditor {
  DBAttribute<Long> getAttribute();

  EnumVariantsSource getVariants();

  void setValue(EditModelState model, ItemKey itemKey);

  void setValueItem(EditModelState model, Long item);

  ItemKey getValue(EditModelState model);
}
