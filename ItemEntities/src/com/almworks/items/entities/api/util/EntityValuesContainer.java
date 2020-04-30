package com.almworks.items.entities.api.util;

import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.EntityValues;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class EntityValuesContainer implements EntityValues {
  private final Map<EntityKey<?>, Object> myValues = Collections15.hashMap();

  public <T> void put(EntityKey<T> key, T value) {
    myValues.put(key, value);
  }

  @SuppressWarnings( {"unchecked"})
  @Override
  public <T> T get(EntityKey<T> key) {
    return (T) myValues.get(key);
  }

  @Override
  public Collection<EntityKey<?>> getValueKeys() {
    return Collections.unmodifiableCollection(myValues.keySet());
  }
}
