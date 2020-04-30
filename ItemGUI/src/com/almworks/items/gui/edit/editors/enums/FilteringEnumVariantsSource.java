package com.almworks.items.gui.edit.editors.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.commons.Condition;
import com.almworks.util.config.Configuration;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class FilteringEnumVariantsSource implements EnumVariantsSource {
  private final EnumVariantsSource mySource;
  private final Condition<ItemKey> myFilter;

  public FilteringEnumVariantsSource(EnumVariantsSource source, Condition<ItemKey> filter) {
    mySource = source;
    myFilter = filter;
  }

  @Override
  public void prepare(VersionSource source, EditModelState model) {
    mySource.prepare(source, model);
  }

  @Override
  public void configure(final Lifespan life, EditItemModel model, final VariantsAcceptor<ItemKey> acceptor) {
    mySource.configure(life, model, new VariantsAcceptor<ItemKey>() {
      @Override
      public void accept(AListModel<? extends ItemKey> variants, @Nullable Configuration recentConfig) {
        acceptor.accept(filterModel(life, variants), recentConfig);
      }
    });
  }

  @Override
  public LoadedItemKey getResolvedItem(EditModelState model, long item) {
    return mySource.getResolvedItem(model, item);
  }

  @NotNull
  @Override
  public List<ItemKey> selectInvalid(EditModelState model, Collection<? extends ItemKey> items) {
    List<ItemKey> invalids = mySource.selectInvalid(model, items);
    if (myFilter == null) return invalids;
    boolean copied = false;
    for (ItemKey item : items) {
      if (item == null || myFilter.isAccepted(item)) continue;
      if (invalids.contains(item)) continue;
      if (!copied) {
        invalids = Collections15.arrayList(invalids);
        copied = true;
      }
      invalids.add(item);
    }
    return invalids;
  }

  @Override
  public boolean isValidValueFor(CommitContext context, long itemValue) {
    return mySource.isValidValueFor(context, itemValue); // Assume all values are valid (even filtered out values)
  }

  private <I extends ItemKey> AListModel<I> filterModel(Lifespan life, AListModel<I> model) {
    if (myFilter == null) return model;
    return FilteringListDecorator.create(life, model, myFilter);
  }
}
