package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;

interface DnDFieldEditor extends FieldEditor {
  String getDescription(ActionContext context, boolean full) throws CantPerformException;
}
