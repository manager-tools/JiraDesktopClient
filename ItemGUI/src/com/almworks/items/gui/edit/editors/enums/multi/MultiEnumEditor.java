package com.almworks.items.gui.edit.editors.enums.multi;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.FieldEditor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface MultiEnumEditor extends FieldEditor {
  @NotNull
  List<ItemKey> getValue(EditModelState model);

  void setValue(EditModelState model, List<ItemKey> itemKeys);
}
