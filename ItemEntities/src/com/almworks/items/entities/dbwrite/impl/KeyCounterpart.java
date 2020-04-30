package com.almworks.items.entities.dbwrite.impl;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.EntityValues;
import com.almworks.items.entities.dbwrite.ResolutionException;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.bool.BoolExpr;

class KeyCounterpart<K, D> {
  private final EntityKey<K> myKey;
  private final DBAttribute<D> myAttribute;
  private final KeyDataKind<K, D> myBehaviour;

  KeyCounterpart(EntityKey<K> key, KeyDataKind<K, D> behaviour, DBAttribute<D> attribute) {
    myKey = key;
    myBehaviour = behaviour;
    myAttribute = attribute;
  }

  public void copyValue(WriteResolution resolution, ItemVersionCreator target, EntityValues source)
    throws ResolutionException {
    myBehaviour.copyValue(this, resolution, target, source);
  }

  public BoolExpr<DP> buildQuery(ItemResolution resolution, EntityValues entity) throws ResolutionException {
    return myBehaviour.buildQuery(this, resolution, entity);
  }

  public DBAttribute<D> getAttribute() {
    return myAttribute;
  }

  protected EntityKey<K> getKey() {
    return myKey;
  }

  @Override
  public String toString() {
    DBAttribute<?> attribute = getAttribute();
    EntityKey<?> key = getKey();
    StringBuilder builder = new StringBuilder("KeyCounterpart[");
    if (attribute == null || key == null) builder.append("<invalid>");
    builder.append(attribute).append("<-").append(key).append("]");
    return builder.toString();
  }

  public static <K, D> KeyCounterpart create(EntityKey<K> key, DBAttribute<?> attribute, KeyDataKind<K, D> behaviour) {
    DBAttribute<D> checked = behaviour.checkAttribute(attribute);
    return checked != null ? new KeyCounterpart<K, D>(key, behaviour, checked) : null;
  }

  public static <T> DBAttribute<T> checkAttribute(DBAttribute<?> attribute, DBAttribute.ScalarComposition composition, Class<?> valueClass) {
    if (attribute == null) return null;
    if (attribute.getComposition() != composition) return null;
    if (!attribute.getScalarClass().equals(valueClass)) return null;
    return (DBAttribute<T>) attribute;
  }
}
