package com.almworks.items.gui.edit.editors.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.advmodel.AListModel;

public interface DefaultItemSelector {
  ItemKey selectDefaultItem(EditModelState model, AListModel<? extends ItemKey> variants);

  void readDB(VersionSource source, EditModelState model);

  DefaultItemSelector ALLOW_EMPTY = new DefaultItemSelector() {
    @Override
    public ItemKey selectDefaultItem(EditModelState model, AListModel<? extends ItemKey> variants) {
      return null;
    }

    @Override
    public void readDB(VersionSource source, EditModelState model) {
    }
  };

  DefaultItemSelector ANY = new DefaultItemSelector() {
    @Override
    public ItemKey selectDefaultItem(EditModelState model, AListModel<? extends ItemKey> variants) {
      if (variants.getSize() == 0) return null;
      return variants.getAt(0);
    }

    @Override
    public void readDB(VersionSource source, EditModelState model) {
    }
  };
}
