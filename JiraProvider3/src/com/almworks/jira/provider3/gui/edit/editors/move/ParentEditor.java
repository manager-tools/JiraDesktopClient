package com.almworks.jira.provider3.gui.edit.editors.move;

import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.CancelCommitException;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.sync.ItemVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

interface ParentEditor extends FieldEditor {
  long getSingleParent(@NotNull EditItemModel model, @NotNull DBReader reader, @Nullable ItemVersion issue) throws CancelCommitException;
}
