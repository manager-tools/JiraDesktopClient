package com.almworks.items.entities.api.collector;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntList;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.EntityValueMerge;
import com.almworks.items.entities.api.collector.typetable.EntityCollector2;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class KeyInfo {
  public static final Convertor<? super KeyInfo, String> GET_KEY_ID = new Convertor<KeyInfo, String>() {
    @Override
    public String convert(KeyInfo value) {
      return value == null ? "" : value.getKey().getId();
    }
  };

  public abstract boolean equalValue(Object o1, Object o2);

  public abstract Object getCellValue(Entity source, EntityCollector2 collector);

  public abstract EntityKey<?> getKey();

  public abstract Object convertValue(EntityCollector2 collector, Object value);

  public abstract Object restoreValue(Object rawValue);

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("KeyInfo[");
    getKey().appendIdType(builder);
    builder.append("]");
    return builder.toString();
  }

  @Nullable
  public static <T> KeyInfo create(EntityKey<T> key) {
    if (key.getComposition() == EntityKey.Composition.HINT) return HintInfo.hint(key);
    IdKeyInfo idInfo = createId(key);
    if (idInfo != null) return idInfo;
    if (key.getComposition() == EntityKey.Composition.COLLECTION) return createCollection(key);
    if (key.getComposition() == EntityKey.Composition.ORDER) return createOrder(key);
    if (key.getComposition() == EntityKey.Composition.SCALAR && key.getValueClass() == byte[].class)
      //noinspection unchecked
      return new RawBytesColumn((EntityKey<byte[]>) key);
    return new SimpleColumn<T>(key, 10);
  }

  public static IdKeyInfo createId(EntityKey<?> key) {
    if (key.getComposition() != EntityKey.Composition.SCALAR) return null;
    EntityKey<Integer> iKey = EntityKey.castScalar(key, Integer.class);
    if (iKey != null) return new SimpleColumn<Integer>(iKey, 0);
    EntityKey<String> sKey = EntityKey.castScalar(key, String.class);
    if (sKey != null) return new SimpleColumn<String>(sKey, 1);
    EntityKey<Entity> eKey = EntityKey.castScalar(key, Entity.class);
    if (eKey != null) return new EntityColumn(eKey);
    return null;
  }

  private static <T> KeyInfo createOrder(EntityKey<T> key) {
    if (key.getValueClass() != Entity.class) {
      LogHelper.error("Unsupported value class", key);
      return null;
    }
    //noinspection unchecked
    return new EntityOrderInfo((EntityKey<List<Entity>>) key);
  }

  private static KeyInfo createCollection(EntityKey<?> key) {
    if (key.getValueClass() != Entity.class) {
      LogHelper.error("Unsupported value class", key);
      return null;
    }
    //noinspection unchecked
    return new EntityCollectionInfo((EntityKey<Collection<Entity>>) key);
  }

  public static class EntityOrderInfo extends KeyInfo {
    private final EntityKey<List<Entity>> myKey;

    public EntityOrderInfo(EntityKey<List<Entity>> key) {
      myKey = key;
    }

    @Override
    public boolean equalValue(Object o1, Object o2) {
      if (o1 == o2) return true;
      if (o1 == null || o2 == null) return false;
      if (o1 == ValueRow.NULL_VALUE || o2 == ValueRow.NULL_VALUE) return false;
      EntityPlace[] a1 = Util.castNullable(EntityPlace[].class, o1);
      EntityPlace[] a2 = Util.castNullable(EntityPlace[].class, o2);
      if (a1 == null || a2 == null) return false;
      if (a1.length != a2.length) return false;
      for (int i = 0; i < a1.length; i++) {
        EntityPlace place1 = a1[i];
        EntityPlace place2 = a2[i];
        if (place1 == place2) continue;
        if (place1 == null || place2 == null) return false;
        if (place1.getIndex() != place2.getIndex() || place1.getTable() != place2.getTable()) return false;
      }
      return true;
    }

    @Override
    public Object getCellValue(Entity source, EntityCollector2 collector) {
      if (!source.hasValue(myKey)) return null;
      return convertValue(collector, source.get(myKey));
    }

    @Override
    public EntityKey<?> getKey() {
      return myKey;
    }

    @Override
    public Object convertValue(EntityCollector2 collector, Object value) {
      if (value == null) return ValueRow.NULL_VALUE;
      List list = Util.castNullable(List.class, value);
      if (list == null) {
        LogHelper.error("Wrong value", myKey, value);
        return null;
      }
      ArrayList<EntityPlace> result = Collections15.arrayList();
      for (Object obj : list) {
        Entity entity = Util.castNullable(Entity.class, obj);
        if (entity == null) {
          LogHelper.error("Wrong element", myKey, obj);
          return null;
        }
        EntityPlace place = collector.identify(entity);
        if (place == null) return null;
        result.add(place);
      }
      return result.toArray(new EntityPlace[result.size()]);
    }

    @Override
    public Object restoreValue(Object rawValue) {
      return EntityCollectionInfo.restoreCollection(myKey, rawValue);
    }
  }
  
  public static class EntityCollectionInfo extends KeyInfo {
    private final EntityKey<Collection<Entity>> myKey;

    private EntityCollectionInfo(EntityKey<Collection<Entity>> key) {
      myKey = key;
    }

    @Override
    public boolean equalValue(Object o1, Object o2) {
      if (o1 == o2) return true;
      if (o1 == null || o2 == null) return false;
      if (isEmpty(o1) && isEmpty(o2)) return true;
      if (o1 == ValueRow.NULL_VALUE || o2 == ValueRow.NULL_VALUE) return false;
      EntityPlace[] a1 = Util.castNullable(EntityPlace[].class, o1);
      EntityPlace[] a2 = Util.castNullable(EntityPlace[].class, o2);
      if (a1 == null || a2 == null) return false;
      if (a1.length != a2.length) return false;
      IntArray exclude = new IntArray(a2.length);
      for (EntityPlace place : a1) {
        int index = find(a2, place, exclude);
        if (index < 0) return false;
        exclude.add(index);
      }
      return true;
    }

    private boolean isEmpty(Object obj) {
      if (obj == ValueRow.NULL_VALUE) return true;
      EntityPlace[] collection = Util.castNullable(EntityPlace[].class, obj);
      return collection != null && collection.length == 0;
    }

    private int find(EntityPlace[] array, EntityPlace place, IntList exclude) {
      for (int i = 0; i < array.length; i++) {
        if (exclude.contains(i)) continue;
        EntityPlace entityPlace = array[i];
        if (entityPlace.getIndex() != place.getIndex()) continue;
        if (entityPlace.getTable() != place.getTable()) continue;
        return i;
      }
      return -1;
    }

    @Override
    public Object getCellValue(Entity source, EntityCollector2 collector) {
      if (!source.hasValue(myKey)) return null;
      return convertValue(collector, source.get(myKey));
    }

    @Override
    public EntityKey<?> getKey() {
      return myKey;
    }

    @Override
    public Object convertValue(EntityCollector2 collector, Object value) {
      if (value == null) return ValueRow.NULL_VALUE;
      Collection collection = Util.castNullable(Collection.class, value);
      if (collection == null) {
        LogHelper.error("Wrong value", myKey, value);
        return null;
      }
      ArrayList<EntityPlace> result = Collections15.arrayList();
      for (Object obj : collection) {
        Entity entity = Util.castNullable(Entity.class, obj);
        if (entity == null) {
          LogHelper.error("Wrong element", myKey, obj);
          return null;
        }
        EntityPlace place = collector.addEntity(entity);
        if (place == null) return null;
        result.add(place);
      }
      return result.toArray(new EntityPlace[result.size()]);
    }

    @Override
    public Object restoreValue(Object rawValue) {
      return restoreCollection(myKey, rawValue);
    }
    
    public static List<Entity> restoreCollection(EntityKey<? extends Collection<? extends Entity>> key, Object rawValue) {
      if (rawValue == null) {
        LogHelper.error("Can not restore no value", key);
        return null;
      }
      if (rawValue == ValueRow.NULL_VALUE) return null;
      EntityPlace[] places = Util.castNullable(EntityPlace[].class, rawValue);
      if (places == null) {
        LogHelper.error("Wrong raw value", rawValue);
        return null;
      }
      ArrayList<Entity> result = Collections15.arrayList();
      for (EntityPlace place : places) result.add(place == null ? null : place.restoreEntity());
      return result;
    }
  }

  public static class HintInfo<T> extends KeyInfo {
    private final EntityKey<T> myKey;

    private HintInfo(EntityKey<T> key) {
      myKey = key;
    }

    public static <T> HintInfo<T> hint(EntityKey<T> key) {
      return new HintInfo<T>(key);
    }

    @Override
    public boolean equalValue(Object o1, Object o2) {
      return Util.equals(o1, o2);
    }

    @Override
    public Object getCellValue(Entity source, EntityCollector2 collector) {
      if (!source.hasValue(myKey)) return null;
      T value = source.get(myKey);
      return value == null ? ValueRow.NULL_VALUE : value;
    }

    @Override
    public Object convertValue(EntityCollector2 collector, Object value) {
      if (value == null) return ValueRow.NULL_VALUE;
      Object casted = Util.castNullable(myKey.getValueClass(), value);
      if (casted == null) LogHelper.error("Wrong value class", value, myKey);
      return casted;
    }

    @Override
    public Object restoreValue(Object rawValue) {
      return SimpleColumn.restoreValue(myKey, rawValue);
    }

    @Override
    public EntityKey<?> getKey() {
      return myKey;
    }

    public Object mergeValue(Object prevValue, Object newValue) {
      if (prevValue == null) return newValue;
      if (newValue == null) return prevValue;
      EntityValueMerge merge = myKey.getValue(EntityValueMerge.KEY);
      if (merge == null) {
        LogHelper.error("Merge not supported", myKey, prevValue, newValue);
        return newValue;
      }
      Object merged = merge.mergeValues(prevValue == ValueRow.NULL_VALUE ? null : prevValue, newValue == ValueRow.NULL_VALUE ? null : newValue);
      return merged != null ? merged : ValueRow.NULL_VALUE;
    }
  }

  public static abstract class IdKeyInfo extends KeyInfo {
    public abstract boolean hasChanges(Object value, int indexVersion);

    public abstract int getHash(Object o);
  }

  public static class SimpleColumn<T> extends IdKeyInfo {
    private final EntityKey<T> myKey;
    private final int myClassOrder;

    public SimpleColumn(EntityKey<T> key, int classOrder) {
      myKey = key;
      myClassOrder = classOrder;
    }

    @Override
    public EntityKey<?> getKey() {
      return myKey;
    }

    @Override
    public boolean equalValue(Object o1, Object o2) {
      return Util.equals(o1, o2);
    }

    @Override
    public int getHash(Object o) {
      return Util.hashCode(o);
    }

    @Override
    public Object getCellValue(Entity source, EntityCollector2 collector) {
      if (!source.hasValue(myKey)) return null;
      return convertValue(collector, source.get(myKey));
    }

    @Override
    public Object convertValue(EntityCollector2 collector, Object value) {
      return value != null ? value : ValueRow.NULL_VALUE;
    }

    @Override
    public Object restoreValue(Object rawValue) {
      return restoreValue(myKey, rawValue);
    }

    public int getClassOrder() {
      return myClassOrder;
    }

    @Override
    public boolean hasChanges(Object value, int indexVersion) {
      return false;
    }

    @Override
    public String toString() {
      return "Column[" + myKey + "]" + getClass().toString();
    }

    public static <T> T restoreValue(EntityKey<T> key, Object rawValue) {
      if (rawValue == ValueRow.NULL_VALUE) return null;
      if (rawValue == null) {
        LogHelper.error("Can not restore no value", key);
        return null;
      }
      T casted = (T) Util.castNullable(key.getValueClass(), rawValue);
      LogHelper.assertError(casted != null, "Wrong value class", key, rawValue);
      return casted;
    }
  }

  public static class RawBytesColumn extends SimpleColumn<byte[]> {
    public RawBytesColumn(EntityKey<byte[]> entityKey) {
      super(entityKey, 100);
    }

    @Override
    public boolean equalValue(Object o1, Object o2) {
      if (o1 == o2) return true;
      if (o1 == ValueRow.NULL_VALUE || o2 == ValueRow.NULL_VALUE) return false;
      byte[] bytes1 = Util.castNullable(byte[].class, o1);
      byte[] bytes2 = Util.castNullable(byte[].class, o2);
      if ((bytes1 == null) != (o1 == null) || (bytes2 == null) != (o2 == null)) {
        LogHelper.error("Wrong values", o1, o2, this);
        return false;
      }
      if (bytes1 != null && bytes1.length == 0) bytes1 = null;
      if (bytes2 != null && bytes2.length == 0) bytes2 = null;
      return ArrayUtil.equals(bytes1, bytes2);
    }

    @Override
    public int getHash(Object o) {
      if (o == null) return 0;
      if (o == ValueRow.NULL_VALUE) return ValueRow.NULL_VALUE.hashCode();
      byte[] bytes = Util.castNullable(byte[].class, o);
      if (bytes == null) {
        LogHelper.error("Wrong value", o, this);
        return 0;
      }
      return ArrayUtil.sum(bytes);
    }
  }

  public static class EntityColumn extends IdKeyInfo {
    private final EntityKey<Entity> myKey;

    public EntityColumn(EntityKey<Entity> key) {
      myKey = key;
    }

    @Override
    public EntityKey<?> getKey() {
      return myKey;
    }

    @Override
    public Object getCellValue(Entity source, EntityCollector2 collector) {
      if (!source.hasValue(myKey)) return null;
      Entity value = source.get(myKey);
      if (value == null) return ValueRow.NULL_VALUE;
      else {
        EntityPlace place = collector.identify(value);
        if (place == null) return null;
        return place;
      }
    }

    @Override
    public Object convertValue(EntityCollector2 collector, Object value) {
      if (value == null) return ValueRow.NULL_VALUE;
      Entity entity = Util.castNullable(Entity.class, value);
      if (entity == null) {
        LogHelper.error("Wrong value", value);
        return null;
      }
      return collector.addEntity(entity);
    }

    @Override
    public Entity restoreValue(Object rawValue) {
      if (rawValue == ValueRow.NULL_VALUE) return null;
      if (rawValue == null) {
        LogHelper.error("Can not restore no value", this);
        return null;
      }
      EntityPlace place = Util.castNullable(EntityPlace.class, rawValue);
      if (place == null) {
        LogHelper.error("Wrong raw value class", this, rawValue);
        return null;
      }
      return place.restoreEntity();
    }

    @Override
    public boolean equalValue(Object o1, Object o2) {
      if (o1 == o2) return true;
      if (o1 == null || o2 == null) return false;
      if (o1 == ValueRow.NULL_VALUE || o2 == ValueRow.NULL_VALUE) return false;
      EntityPlace e1 = Util.castNullable(EntityPlace.class, o1);
      EntityPlace e2 = Util.castNullable(EntityPlace.class, o2);
      if (e1 == null || e2 == null) {
        LogHelper.error("Wrong data", o1, o2);
        return false;
      }
      return e1.getIndex() == e2.getIndex() && e1.getTable() == e2.getTable();
    }

    @Override
    public int getHash(Object o) {
      if (o == null) return 0;
      if (o == ValueRow.NULL_VALUE) return ValueRow.NULL_VALUE.hashCode();
      EntityPlace e = Util.castNullable(EntityPlace.class, o);
      if (e == null) {
        LogHelper.error("Wrong data", o);
        return 0;
      }
      return e.getIndex();
    }

    @Override
    public boolean hasChanges(Object value, int indexVersion) {
      if (value == null || value == ValueRow.NULL_VALUE) return false;
      EntityPlace place = Util.castNullable(EntityPlace.class, value);
      if (place == null) {
        LogHelper.error("Wrong value", value);
        return false;
      }
      return place.getIndexVersion() > indexVersion;
    }
  }
}
