package com.almworks.jira.provider3.custom.fieldtypes.enums.single;

import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEditorBuilder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumEditorInfo;
import org.jetbrains.annotations.Nullable;

abstract class SingleEnumEditorType {
  private final boolean myVerify;
  private final boolean myForbidIllegalCommit;

  public SingleEnumEditorType(boolean verify, boolean forbidIllegalCommit) {
    super();
    myVerify = verify;
    myForbidIllegalCommit = forbidIllegalCommit;
  }

  public FieldEditor createEditor(ItemVersion field, @Nullable EnumItemCreator creator) {
    EnumEditorInfo<Long> info = EnumEditorInfo.SINGLE.load(field);
    if (info == null) return null;
    DropdownEditorBuilder builder = EnumEditorInfo.buildDropDown(info, myForbidIllegalCommit, myVerify, creator);
    return createEditorFace(builder);
  }

  protected abstract FieldEditor createEditorFace(DropdownEditorBuilder builder);
}
