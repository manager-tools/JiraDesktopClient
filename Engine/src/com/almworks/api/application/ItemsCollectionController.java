package com.almworks.api.application;

import com.almworks.util.advmodel.AListModelUpdater;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.threads.ThreadSafe;

public interface ItemsCollectionController extends Modifiable {
  void reload();

  boolean isLoading();

  @ThreadSafe
  void cancelLoading(String reason);

  void dispose();

  AListModelUpdater<? extends LoadedItem> getListModelUpdater();

  void setLiveMode(boolean live);

  void updateAllItems();

  boolean isElementSetChanged();

  ScalarModel<LifeMode> getLifeModeModel();

  ItemsCollectionController createCopy(ItemCollectorWidget widget);
}
