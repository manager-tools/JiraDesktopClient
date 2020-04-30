package com.almworks.items.gui.edit.util;

import com.almworks.items.gui.edit.DataVerification;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.util.text.NameMnemonic;
import org.jetbrains.annotations.NotNull;

public abstract class BaseFieldEditor implements FieldEditor {
  private final NameMnemonic myLabelText;

  public BaseFieldEditor(@NotNull NameMnemonic labelText) {
    myLabelText = labelText;
  }

  @Override
  public void afterModelFixed(EditItemModel model) {}

  @Override
  public void afterModelCopied(EditItemModel copy) {}

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    return isChanged(model) || (model.isNewItem() && hasValue(model));
  }

  @NotNull
  public final NameMnemonic getLabelText(EditModelState model) {
    return getLabelText();
  }

  @NotNull
  public final NameMnemonic getLabelText() {
    return myLabelText;
  }

  public final boolean hasErrors(EditItemModel model) {
    DataVerification verifyContext = new DataVerification(model, DataVerification.Purpose.ANY_ERROR);
    verifyData(verifyContext);
    return verifyContext.hasErrors();
  }
}
