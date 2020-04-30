package com.almworks.items.entities.api.collector.transaction;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class EntityHolder {
  public static final EntityHolder[] EMPTY_ARRAY = new EntityHolder[0];
  /**
   * Mark create-identity attributes to allow change values. Common create-identity attribute values can not be changed
   */
  public static final EntityKey<Boolean> MUTABLE_IDENTITY = EntityKey.hint("sys.transaction.mutableIdentity", Boolean.class);

  private final EntityTransaction myTransaction;
  private final EntityPlace myPlace;

  public EntityHolder(EntityTransaction transaction, EntityPlace place) {
    myTransaction = transaction;
    myPlace = place;
  }

  public EntityTransaction getTransaction() {
    return myTransaction;
  }

  public EntityPlace getPlace() {
    return myPlace;
  }

  public <T> EntityHolder setValue(EntityKey<T> key, @Nullable T value) {
    KeyInfo info = myPlace.getOrCreateColumn(key);
    if (info == null) return this;
    myPlace.setConvertedValue(info, info.convertValue(myPlace.getCollector(), value));
    return this;
  }

  public <T> void setNNValue(EntityKey<T> key, @Nullable T value) {
    if (value != null) setValue(key, value);
  }

  public void setReference(EntityKey<Entity> key, @Nullable EntityHolder value) {
    if (value == null) setValue(key, null);
    else {
      KeyInfo column = myPlace.getOrCreateColumn(key);
      if (column == null) return;
      myPlace.setConvertedValue(column, value.myPlace);
    }
  }

  public void setNNReference(EntityKey<Entity> key, @Nullable EntityHolder value) {
    if (value != null) setReference(key, value);
  }

  public void setReferenceCollection(EntityKey<? extends Collection<Entity>> key, @Nullable Collection<? extends EntityHolder> value) {
    value = Util.NN(value, Collections.<EntityHolder>emptyList());
    if (value.isEmpty()) {
      setValue(key, null);
      return;
    }
    KeyInfo column = myPlace.getOrCreateColumn(key);
    if (column == null) return;
    EntityPlace[] array = new EntityPlace[value.size()];
    int i = 0;
    for (EntityHolder holder : value) {
      array[i] = holder != null ? holder.myPlace : null;
      i++;
    }
    myPlace.setConvertedValue(column, array);
  }

  @Nullable
  public <T> T getScalarValue(EntityKey<T> key) {
    if (key == null) return null;
    EntityKey.Composition composition = key.getComposition();
    if ((composition != EntityKey.Composition.SCALAR && composition != EntityKey.Composition.HINT) || key.getValueClass() == Entity.class) {
      LogHelper.error("Unsupported key", key);
      return null;
    }
    KeyInfo info = myPlace.getCollector().getKnownKeyInfo(key);
    if (info == null) return null;
    Object value = myPlace.getValue(info);
    if (value == ValueRow.NULL_VALUE) return null;
    return (T) value;
  }

  public EntityHolder getReference(EntityKey<Entity> key) {
    if (key == null) return null;
    KeyInfo column = myPlace.getCollector().getKnownKeyInfo(key);
    if (column == null) return null;
    Object value = myPlace.getValue(column);
    if (value == ValueRow.NULL_VALUE || value == null) return null;
    EntityPlace place = Util.castNullable(EntityPlace.class, value);
    if (place == null) {
      LogHelper.error("Wrong value class", value, key);
      return null;
    }
    return new EntityHolder(myTransaction, place);
  }

  @NotNull
  public EntityHolder[] getReferenceCollection(EntityKey<? extends Collection<? extends Entity>> key) {
    if (key == null) return EntityHolder.EMPTY_ARRAY;
    KeyInfo column = myPlace.getCollector().getKnownKeyInfo(key);
    if (column == null) return EntityHolder.EMPTY_ARRAY;
    Object value = myPlace.getValue(column);
    if (value == ValueRow.NULL_VALUE) return EntityHolder.EMPTY_ARRAY;
    EntityPlace[] places = Util.castNullable(EntityPlace[].class, value);
    if (places == null) {
      if (value != null) LogHelper.error("Wrong value class", value, key);
      else LogHelper.warning("Null collection value", key);
      return EntityHolder.EMPTY_ARRAY;
    }
    EntityHolder[] result = new EntityHolder[places.length];
    for (int i = 0; i < places.length; i++) result[i] = new EntityHolder(myTransaction, places[i]);
    return result;
  }

  public Entity getItemType() {
    return myPlace.getTable().getItemType();
  }

  public void allowSearchCreate(EntityWriter writer) {
    writer.allowSearchCreate(myPlace);
  }

  @Nullable
  public <T> T getScalarFromReference(EntityKey<Entity> reference, EntityKey<T> scalarKey) {
    EntityHolder holder = getReference(reference);
    return holder == null ? null : holder.getScalarValue(scalarKey);
  }

  public boolean hasValue(EntityKey<?> key) {
    if (key == null) return false;
    KeyInfo column = myPlace.getCollector().getKnownKeyInfo(key);
    return column != null && myPlace.getValue(column) != null;
  }

  public <T> void setNewValue(EntityKey<T> key, T value) {
    if (!hasValue(key)) setValue(key, value);
  }

  public <T> void overrideValue(EntityKey<T> key, T value) {
    KeyInfo info = myPlace.getCollector().getKnownKeyInfo(key);
    if (info == null) {
      LogHelper.error("Can not override unknown key", key, value);
      return;
    }
    myPlace.override(info, info.convertValue(myPlace.getCollector(), value));
  }

  public void copyFrom(Entity entity) {
    if (entity == null) return;
    Entity type = entity.getType();
    if (!Util.equals(type, getItemType())) {
      LogHelper.error("Can not copy different type", this, EntityUtils.printValue(entity));
      return;
    }
    for (EntityKey<?> key : entity.getValueKeys()) {
      if (Entity.isKeyType(key.toEntity())) continue;
      copyValue(entity, key);
    }
  }
  
  public Entity restore() {
    Entity entity = Entity.ensureModifiable(myPlace.restoreEntity());
    Collection<KeyInfo> columns = myPlace.getTable().getAllColumns();
    for (KeyInfo column : columns) {
      EntityKey<?> key = column.getKey();
      if (entity.hasValue(key)) continue;
      Object rawValue = myPlace.getValue(column);
      if (rawValue == null) continue;
      if (rawValue == ValueRow.NULL_VALUE) {
        entity.put(key, null);
        continue;
      }
      Object restored = column.restoreValue(rawValue);
      if (restored != null) //noinspection unchecked
        entity.put((EntityKey<Object>) key, restored);
    }
    entity.fix();
    return entity;
  }
  
  public void copyFrom(EntityHolder other) {
    if (other == null) return;
    copyFrom(other.restore());
  }

  private <T> void copyValue(Entity source, EntityKey<T> key) {
    if (source.hasValue(key)) setValue(key, source.get(key));
  }

  public void setItem(long item) {
    myPlace.setItem(item);
  }
}
