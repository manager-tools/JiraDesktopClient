package com.almworks.actions.console;

import com.almworks.util.advmodel.AListModel;

public interface VariantModelController<T> {
  void setText(String text);

  AListModel<T> getVariants();
}
