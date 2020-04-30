package com.almworks.items.entities.api;

import com.almworks.items.entities.api.util.EntityValuesContainer;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class EntityQuery implements EntityValues {
  private final EntityValuesContainer myValues = new EntityValuesContainer();
  private boolean myIncludeConnection = false;

  public EntityQuery() {
  }

  public <T> EntityQuery equal(EntityKey<T> key, T value) {
    myValues.put(key, value);
    return this;
  }

  public static <T> EntityQuery connection(EntityKey<T> key, T value) {
    EntityQuery query = new EntityQuery();
    query.includeConnection();
    query.equal(key, value);
    return query;
  }

  @Override
  public <T> T get(EntityKey<T> key) {
    return (T) myValues.get(key);
  }

  @Override
  public Collection<EntityKey<?>> getValueKeys() {
    return myValues.getValueKeys();
  }

  public boolean isIncludeConnection() {
    return myIncludeConnection;
  }

  public EntityQuery includeConnection() {
    myIncludeConnection = true;
    return this;
  }

  @NotNull
  public List<Entity> select(Collection<? extends Entity> entities) {
    List<Entity> result = Collections15.arrayList();
    for (Entity entity : entities) if (matches(entity)) result.add(entity);
    return result;
  }

  private boolean matches(Entity entity) {
    for (EntityKey<?> key : myValues.getValueKeys()) {
      Object value = myValues.get(key);
      if (!Util.equals(value, entity.get(key))) return false;
    }
    return true;
  }
}
