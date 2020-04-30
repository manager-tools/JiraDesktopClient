package com.almworks.items.entities.api;

/**
 * Value merger to allow unite entities with different values for the same key. This interface defines value merge.<br>
 * To use this facility add the implementation to the EntityKey.
 */
public interface EntityValueMerge {
  EntityKey<EntityValueMerge> KEY = EntityKey.hint("sys.store.entityValueMerge", EntityValueMerge.class);

  <T> T mergeValues(T value1, T value2);
}
