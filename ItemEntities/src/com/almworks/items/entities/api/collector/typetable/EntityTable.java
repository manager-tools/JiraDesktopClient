package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.Pair;

import java.util.Collection;
import java.util.List;

public interface EntityTable {
  EntityCollector2 getCollector();

  Entity getItemType();

  int getResolutionsCount();

  Pair<List<KeyInfo>,Collection<EntityPlace>> getResolution(int resolutionIndex);

  boolean isCreateResolution(int resolutionIndex);

  Pair<ItemProxy[], EntityPlace[]> getResolvedByProxy(DBNamespace namespace);

  Collection<KeyInfo> getAllColumns();

  int getPlaceCount();

  Collection<EntityPlace> getAllPlaces();

  boolean isCreateResolutionColumn(KeyInfo info);

  boolean isMutableResolution(int resolutionIndex);
}
