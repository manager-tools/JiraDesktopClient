package com.almworks.items.gui.edit.editors;

import com.almworks.items.gui.edit.*;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.LogHelper;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class EditorProxy implements FieldEditor {
  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    LogHelper.error("Should no happen", this);
    return Collections.emptyList();
  }

  @Override
  public void afterModelFixed(EditItemModel model) {
    LogHelper.error("Should no happen", this);
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    LogHelper.error("Should no happen", this);
    return false;
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    LogHelper.error("Should no happen", this);
    return false;
  }

  @Override
  public boolean hasValue(EditModelState model) {
    LogHelper.error("Should no happen", this);
    return false;
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    LogHelper.error("Should no happen", this);
  }

  @Override
  public void afterModelCopied(EditItemModel copy) {
    LogHelper.error("Should no happen", this);
  }

  @Override
  public void commit(CommitContext context) {
    LogHelper.error("Should no happen", this);
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
    LogHelper.error("Should no happen", this);
  }
}
