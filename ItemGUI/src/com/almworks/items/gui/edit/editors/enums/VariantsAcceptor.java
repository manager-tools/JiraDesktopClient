package com.almworks.items.gui.edit.editors.enums;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.config.Configuration;
import org.jetbrains.annotations.Nullable;

public interface VariantsAcceptor<T> {
  void accept(AListModel<? extends T> variants, @Nullable Configuration recentConfig);
}
