package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;

import java.util.List;

interface PriEntityTable extends EntityTable {
  Object getValue(KeyInfo column, int row);

  boolean setValue(EntityPlace place, KeyInfo column, Object value, boolean override);

  void setValues(EntityPlace place, ValueRow row);

  void setItem(EntityPlace place, long item);

  Entity restoreIdentified(int placeIndex);

  List<KeyInfo> getResolutionColumns(int resolutionIndex);
}
