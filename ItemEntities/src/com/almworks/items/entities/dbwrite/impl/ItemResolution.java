package com.almworks.items.entities.dbwrite.impl;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.dbwrite.ResolutionException;

interface ItemResolution {
  /**
   * Resolves entity to item if the resolution is possible.<br>
   * Subclasses may provide special obligation. Such obligations may include throw exception if resolution not possible.
   * @param entity entity to resolve. Null value never resolves to positive item.
   * @return positive if item resolved, 0 for null or unresolved entity
   * @throws com.almworks.items.entities.dbwrite.ResolutionException if the entity cannot be resolved
   */
  long resolve(Entity entity) throws ResolutionException;
}
