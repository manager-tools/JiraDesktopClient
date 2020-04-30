package com.almworks.api.application;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.properties.PropertyMap;

public interface ItemUiModel extends ItemWrapper, Modifiable {
  ModelMap getModelMap();

  PropertyMap takeSnapshot();

  boolean isChanged();
}
