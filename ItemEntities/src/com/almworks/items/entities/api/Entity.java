package com.almworks.items.entities.api;

import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class Entity implements EntityValues {
  public static final Comparator<Entity> TYPE_BY_ID = new Comparator<Entity>() {
    @Override
    public int compare(Entity o1, Entity o2) {
      if (o1 == o2) return 0;
      if (o1 == null || o2 == null) return o1 == null ? -1 : 1;
      String id1 = o1.get(EntityInit.TYPE_ID);
      String id2 = o2.get(EntityInit.TYPE_ID);
      if (Util.equals(id1, id2)) return 0;
      if (id1 == null || id2 == null) return id1 == null ? -1 : 1;
      return id1.compareTo(id2);
    }
  };

  private static final int PREDEFINED_ENTITIES = 7;
  private final Map<EntityKey<?>, ?> myValues = Collections15.hashMap();
  private int myHashCode = 0;
  private boolean myFixed = false;

  Entity(int hashCode) {
    if (hashCode < 1 || hashCode > PREDEFINED_ENTITIES) throw new RuntimeException(String.valueOf(hashCode));
    myHashCode = hashCode;
  }

  public Entity(Entity type) {
    LogHelper.assertError(EntityInit.META_TYPE.equals(type.get(EntityInit.TYPE)));
    priPut(EntityInit.TYPE, type);
  }

  public static Entity type(String typeId) {
    Entity type = buildType(typeId);
    type.fix();
    return type;
  }

  public static Entity buildType(String typeId) {
    Entity type = new Entity(EntityInit.META_TYPE);
    type.put(EntityInit.TYPE_ID, typeId);
    return type;
  }

  public <T> Entity put(EntityKey<T> key, @Nullable T value) {
    if (myFixed) LogHelper.error("Already fixed", key, value);
    else priPut(key, value);
    return this;
  }

  public <T> Entity putIfNotNull(EntityKey<T> key, T value) {
    if (value != null) put(key, value);
    return this;
  }

  @SuppressWarnings( {"unchecked"})
  <T> void priPut(EntityKey<T> key, T value) {
    if (value != null) {
      EntityKey.Composition composition = key.getComposition();
      if (composition == EntityKey.Composition.COLLECTION || composition == EntityKey.Composition.ORDER) {
        Collection collection = Util.castNullable(Collection.class, value);
        if (collection == null) {
          LogHelper.error("Wrong value class", key.getId(), value);
          value = null;
        } else if (collection.isEmpty()) value = null;
      }
    }
    ((Map)myValues).put(key, value);
  }

  @SuppressWarnings( {"unchecked"})
  public <T> T get(EntityKey<T> key) {
    return (T) myValues.get(key);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (myHashCode > 0 && myHashCode <= PREDEFINED_ENTITIES) return false;
    Entity other = Util.castNullable(Entity.class, obj);
    if (other == null) return false;
    if (hashCode() != other.hashCode()) return false;
    if (myValues.size() != other.myValues.size()) return false;
    for (EntityKey<?> key : myValues.keySet()) {
      if (!Util.equals(get(key), other.get(key))) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    if (myHashCode != 0) return myHashCode;
    if (!myFixed) LogHelper.error("Not fixed");
    myHashCode = calcHashCode(myValues);
    return myHashCode;
  }

  @Override
  public String toString() {
    if (!EntityInit.INIT_DONE) return super.toString();
    Entity type = get(EntityInit.TYPE);
    if (type == null) return "Entity[invalid]";
    if (EntityInit.META_TYPE.equals(type)) {
      String typeId = getTypeId();
      if (typeId == null) typeId = "<invalid>";
      return "Entity[TYPE:'" + typeId + "']";
    }
    if (EntityInit.T_KEY.equals(type)) {
      String keyId = get(EntityInit.ID);
      if (keyId == null) keyId = "<invalid>";
      return "Entity[KEY:'" + keyId + "']";
    }
    String typeId = type.getTypeId();
    if (typeId == null) typeId = "<invalidType>";
    return "Entity[" + typeId + "]";
  }

  public boolean isFixed() {
    return myFixed;
  }

  public Entity fix() {
    if (myFixed) return this;
    myFixed = true;
    for (Map.Entry<EntityKey<?>, ?> entry : myValues.entrySet()) {
      Object value = entry.getValue();
      if (value == null) continue;
      EntityKey<?> key = entry.getKey();
      Class<?> clazz = key.getValueClass();
      if (Entity.class.isAssignableFrom(clazz)) {
        Entity entity = Util.castNullable(Entity.class, value);
        if (entity != null) entity.fix();
        else {
          Collection collection = Util.castNullable(Collection.class, value);
          if (collection == null) LogHelper.error("Unknown composition", value);
          else {
            for (Object v : collection) {
              Entity e = Util.castNullable(Entity.class, v);
              if (e != null) e.fix();
              else LogHelper.error("Unknown composition", value, v);
            }
          }
        }
      }
    }
    return this;
  }

  private static int calcHashCode(Map<EntityKey<?>, ?> values) {
    int hash = 0;
    for (Map.Entry<EntityKey<?>, ?> entry : values.entrySet()) {
      hash = hash ^ entry.getKey().hashCode() ^ Util.hashCode(entry.getValue());
    }
    if (hash > 0 && hash <= PREDEFINED_ENTITIES) hash += PREDEFINED_ENTITIES;
    return hash;
  }

  /**
   * Checks that the entity is type and returns type ID. Log errors if the entity is not a type.
   * @return type Id if the Entity represents a type, null otherwise
   */
  public String getTypeId() {
    LogHelper.assertError(get(EntityInit.TYPE) == EntityInit.META_TYPE, this);
    String typeId = get(EntityInit.TYPE_ID);
    LogHelper.assertError(typeId != null, "Missing typeId", typeId, this);
    return typeId != null ? typeId : "<invalidType>";
  }

  public Entity getType() {
    return get(EntityInit.TYPE);
  }

  @Override
  public Collection<EntityKey<?>> getValueKeys() {
    return Collections.unmodifiableCollection(myValues.keySet());
  }

  public static boolean isMetaType(Entity entity) {
    return metaType().equals(entity);
  }

  public static Entity metaType() {
    return EntityInit.META_TYPE;
  }

  public static boolean isKeyType(Entity entity) {
    return EntityInit.T_KEY.equals(entity);
  }

  public static Entity copy(Entity other) {
    if (other == null) return null;
    Entity copy = new Entity(other.get(EntityInit.TYPE));
    for (EntityKey<?> key : other.getValueKeys()) {
      if (EntityInit.TYPE.equals(key)) continue;
      copy.copyFrom(key, other);
    }
    return copy;
  }

  /**
   * Returns modifiable entity, if the entity is already fixed returns modifiable copy.<br>
   * <b>Note:</b> Caller code should ensure that the returned entity added to some entity collection or referred by another entity, otherwise this method produces garbage
   * and modifications are lost.
   * @return not fixed entity with same values as original.
   */
  public static Entity ensureModifiable(Entity entity) {
    if (entity == null) return entity;
    if (entity.isFixed()) entity = copy(entity);
    return entity;
  }

  public <T> void copyFrom(EntityKey<T> key, EntityValues other) {
    put(key, other.get(key));
  }

  public void copyAllFrom(EntityValues other) {
    if (other == null) return;
    for (EntityKey<?> key : other.getValueKeys()) copyFrom(key, other);
  }

  public String getEntityTypeId() {
    Entity type = getType();
    return type != null ? type.getTypeId() : null;
  }

  public boolean hasValue(EntityKey<?> key) {
    return key != null && myValues.containsKey(key);
  }

  public <T> T getValueOfEntity(EntityKey<Entity> entityRef, EntityKey<T> valueKey) {
    Entity entity = get(entityRef);
    return entity != null ? entity.get(valueKey) : null;
  }
}
