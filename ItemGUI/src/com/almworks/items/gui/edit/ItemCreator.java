package com.almworks.items.gui.edit;

import com.almworks.items.sync.ItemVersionCreator;
import org.almworks.util.TypedKey;

public interface ItemCreator {
  TypedKey<ItemCreator> KEY = TypedKey.create("creator");

  void setupNewItem(EditModelState model, ItemVersionCreator item);
}
