package com.almworks.items.gui.edit.editors;

import com.almworks.items.gui.edit.*;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.LogHelper;
import com.almworks.util.text.NameMnemonic;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Base class for facilities which are not real editors, not intended to edit anything.<br>
 * It provides "do nothing" implementation.
 */
public abstract class MockEditor implements FieldEditor {
  @NotNull
  private final NameMnemonic myLabelText;

  public MockEditor() {
    this(NameMnemonic.EMPTY);
  }

  protected MockEditor(@NotNull NameMnemonic labelText) {
    myLabelText = labelText;
  }

  @Override
  public void afterModelCopied(EditItemModel copy) {
  }

  @Override
  public void afterModelFixed(EditItemModel model) {
  }

  @Override
  @NotNull
  public NameMnemonic getLabelText(EditModelState model) {
    return myLabelText;
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    return Collections.emptyList();
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    return false;
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return false;
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    return false;
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
  }

  @Override
  public void commit(CommitContext context) {
    LogHelper.error("Should not happen", this, context);
  }
}
