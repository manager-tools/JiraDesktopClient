package com.almworks.items.entities.api;

import java.util.Collection;

public interface EntityValues {
  <T> T get(EntityKey<T> key);

  Collection<EntityKey<?>> getValueKeys();
}
