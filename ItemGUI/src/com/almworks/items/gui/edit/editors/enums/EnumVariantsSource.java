package com.almworks.items.gui.edit.editors.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.VersionSource;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface EnumVariantsSource {
  void prepare(VersionSource source, EditModelState model);

  void configure(Lifespan life, final EditItemModel model, final VariantsAcceptor<ItemKey> acceptor);

  @Nullable
  LoadedItemKey getResolvedItem(EditModelState model, long item);

  @NotNull
  List<ItemKey> selectInvalid(EditModelState model, Collection<? extends ItemKey> items);

  boolean isValidValueFor(CommitContext context, long itemValue);
}
