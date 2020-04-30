package com.almworks.items.entities.api;

import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;

public class EntityKey<T> {
  public static final EntityKey[] EMPTY_ARRAY = new EntityKey[0];
  public static final Set<Class<?>> SCALAR_CLASSES =
    Collections15.<Class<?>>unmodifiableSetCopy(String.class, Integer.class, Date.class, BigDecimal.class, Boolean.class);

  public static Comparator<EntityKey<?>> ID_ORDER = new Comparator<EntityKey<?>>() {
    @Override
    public int compare(EntityKey<?> o1, EntityKey<?> o2) {
      if (o1 == o2) return 0;
      if (o1 == null || o2 == null) return o1 == null ? -1 : 1;
      return o1.getId().compareTo(o2.getId());
    }
  };
  private final Entity myDescription;

  EntityKey(Entity description) {
    myDescription = description;
  }
  
  public static <T> EntityKey<T> castScalar(EntityKey<?> key, Class<T> aClass) {
    if (key == null) return null;
    if (key.getComposition() != Composition.SCALAR) return null;
    if (key.getValueClass() != aClass) return null;
    //noinspection unchecked
    return (EntityKey<T>) key;
  }

  public static <T> EntityKey<? extends Collection<T>> castMulti(EntityKey<?> key, Class<T> valueClass) {
    if (key == null) return null;
    if (key.getValueClass() != valueClass) return null;
    Composition composition = key.getComposition();
    if (composition != Composition.COLLECTION && composition != Composition.ORDER) return null;
    //noinspection unchecked
    return (EntityKey<? extends Collection<T>>) key;
  }

  public static EntityKey<String> string(String id, @Nullable Entity description) {
    return scalar(id, String.class, description);
  }

  public static EntityKey<Integer> integer(String id, @Nullable Entity description) {
    return scalar(id, Integer.class, description);
  }

  public static EntityKey<Date> date(String id, @Nullable Entity description) {
    return scalar(id, Date.class, description);
  }

  public static <T> EntityKey<T> hint(String id, Class<T> valueClass) {
    return hint(id, valueClass, null);
  }

  public static <T> EntityKey<T> hint(String id, Class<T> valueClass, @Nullable Entity description) {
    return priCreate(id, description, Composition.HINT, valueClass);
  }

  public static EntityKey<Boolean> bool(String id, @Nullable Entity description) {
    return scalar(id, Boolean.class, description);
  }

  public static EntityKey<Long> longInt(String id, @Nullable Entity description) {
    return scalar(id, Long.class, description);
  }

  public static EntityKey<byte[]> bytes(String id, @Nullable Entity description) {
    return scalar(id, byte[].class, description);
  }

  @NotNull
  public static EntityKey<Entity> entity(String id, @Nullable Entity description) {
    return priCreate(id, description, Composition.SCALAR, Entity.class);
  }

  @NotNull
  public static EntityKey<Collection<Entity>> entityCollection(String id, @Nullable Entity description) {
    return collection(id, Entity.class, description);
  }

  @NotNull
  public static <T> EntityKey<Collection<T>> collection(String id, Class<T> clazz, Entity description) {
    return priCreate(id, description, Composition.COLLECTION, clazz);
  }

  public static EntityKey<List<Entity>> entityList(String id, @Nullable Entity description) {
    return priCreate(id, description, Composition.ORDER, Entity.class);
  }

  @NotNull
  public static <T> EntityKey<T> scalar(String id, Class<T> clazz, @Nullable Entity description) {
    return priCreate(id, description, Composition.SCALAR, clazz);
  }

  public static EntityKey<Entity> keyType() {
    return EntityInit.TYPE;
  }

  public static Entity typeKey() {
    return EntityInit.T_KEY;
  }

  @NotNull
  private static <T> EntityKey<T> priCreate(String id, @Nullable Entity description, Composition composition, Class<?> clazz) {
    if (description == null) description = buildKey();
    if (description.isFixed()) {
      LogHelper.error("Fixed description", description);
      description = buildKey();
    }
    if (!EntityInit.T_KEY.equals(description.getType())) {
      LogHelper.error("Wrong type", description);
      description = buildKey();
    }
    description.put(EntityInit.ID, id);
    description.put(EntityInit.COMPOSITION, composition);
    description.put(EntityInit.CLASS, clazz);
    description.fix();
    return new EntityKey<T>(description);
  }

  public String getId() {
    String id = getId(myDescription);
    if (id == null) {
      LogHelper.error("Missing id", this);
      id = "";
    }
    return id;
  }

  public Composition getComposition() {
    return getComposition(myDescription);
  }

  public Class<?> getValueClass() {
    return getValueClass(myDescription);
  }

  public <T> T getValue(EntityKey<T> key) {
    return myDescription.get(key);
  }

  public Entity toEntity() {
    return myDescription;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    EntityKey other = Util.castNullable(EntityKey.class, obj);
    return other != null && myDescription.equals(other.myDescription);
  }

  @Override
  public int hashCode() {
    return myDescription.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("EntityKey[");
    appendIdType(builder);
    builder.append("]");
    return builder.toString();
  }

  public void appendIdType(StringBuilder builder) {
    builder.append(getId(myDescription)).append(":");
    Class<?> clazz = getValueClass(myDescription);
    Composition composition = getComposition(myDescription);
    if (clazz == null || composition == null) builder.append("<invalid>");
    else {
      builder.append(getDisplayableName(clazz));
      if (composition != Composition.SCALAR) builder.append(" ").append(composition);
    }
  }

  private String getDisplayableName(Class<?> clazz) {
    if (clazz == String.class) return "String";
    if (clazz == Integer.class) return "Int";
    if (clazz == Entity.class) return "Entity";
    if (clazz == Date.class) return "Date";
    if (clazz == BigDecimal.class) return "Decimal";
    if (clazz == Boolean.class) return "bool";
    return clazz.getName();
  }

  @NotNull
  public static Entity buildKey() {
    return new Entity(EntityInit.T_KEY);
  }

  public static Composition getComposition(Entity entity) {
    return entity != null ? entity.get(EntityInit.COMPOSITION) : null;
  }

  public static String getId(Entity entity) {
    return entity != null ? entity.get(EntityInit.ID) : null;
  }

  public static Class<?> getValueClass(Entity entity) {
    return entity != null ? entity.get(EntityInit.CLASS) : null;
  }

  public enum Composition {
    SCALAR, COLLECTION, ORDER, HINT
  }
}
