package com.almworks.items.sync.util.identity;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.LongSet;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.util.Map;
import java.util.Set;

public class DBStaticObject implements ItemProxy {
  public static final Convertor<DBStaticObject, DBIdentity> GET_IDENTITY = new Convertor<DBStaticObject, DBIdentity>() {
    @Override
    public DBIdentity convert(DBStaticObject value) {
      return value != null ? value.getIdentity() : null;
    }
  };
  private static final TypedKey<Boolean> FULL_MATERIALIZE = TypedKey.create("fullMaterialize");
  private static final TypedKey<LongSet> MATERIALIZED_OBJECTS = TypedKey.create("materializedObjects");
  private final DBIdentity myIdentity;
  private final DBAttribute<?>[] myAttributes;
  private final Object[] myValues;

  private DBStaticObject(DBIdentity identity, DBAttribute<?>[] attributes, Object[] values) {
    myIdentity = identity;
    myAttributes = attributes;
    myValues = values;
  }

  @Override
  public long findItem(DBReader reader) {
    return myIdentity.findItem(reader);
  }

  @Override
  public long findOrCreate(DBDrain drain) {
    long item = findItem(drain.getReader());
    if (item > 0) {
      Map cache = drain.getReader().getTransactionCache();
      if (!Boolean.TRUE.equals(cache.get(FULL_MATERIALIZE))) return item;
      LongSet materialized = MATERIALIZED_OBJECTS.getFrom(cache);
      if (materialized != null && materialized.contains(item)) return item;
      if (materialized == null) {
        materialized = new LongSet();
        MATERIALIZED_OBJECTS.putTo(cache, materialized);
      }
      materialized.add(item);
    } else item = myIdentity.findOrCreate(drain);
    if (item <= 0) return 0;
    initialize(drain.changeItem(item));
    return item;
  }

  public static void transactionForceFullMaterialize(DBDrain drain) {
    FULL_MATERIALIZE.putTo(drain.getReader().getTransactionCache(), true);
  }

  public long forceWrite(DBDrain drain) {
    long item = myIdentity.findOrCreate(drain);
    if (item <= 0) return 0;
    initialize(drain.changeItem(item));
    return item;
  }

  private void initialize(ItemVersionCreator creator) {
    if (myAttributes == null || myValues == null) return;
    for (int i = 0; i < myAttributes.length; i++) {
      DBAttribute<?> attribute = myAttributes[i];
      Object value = myValues[i];
      value = toDBValue(creator, value);
      if (value == null) continue;
      creator.setValue((DBAttribute<Object>) attribute, value);
    }
  }

  private static final Set<Class<?>> SIMPLE_SCALARS = Collections15.<Class<?>>unmodifiableSetCopy(String.class, Boolean.class, Integer.class, byte[].class, Long.class, Byte.class);
  public static Object toDBValue(DBDrain drain, Object value) {
    if (value == null) return null;
    Class<?> aClass = value.getClass();
    if (SIMPLE_SCALARS.contains(aClass)) return value;
    if (DBIdentifiedObject.class.isAssignableFrom(aClass)) return toItemValue(drain.materialize((DBIdentifiedObject)value));
    if (ItemProxy.class.isAssignableFrom(aClass)) return toItemValue(drain.materialize((ItemProxy)value));
    if (ScalarSequence.class.isAssignableFrom(aClass)) return ((ScalarSequence) value).serialize(drain);
    LogHelper.error("Unknown data class", aClass, value);
    return null;
  }

  private static Object toItemValue(long item) {
    return item > 0 ? item : null;
  }

  /**
   * This method should be used in rare cases to lookup in maps. Never use this method to materialize the object since
   * it is not initialized in such case.
   * @return this object identity
   */
  public DBIdentity getIdentity() {
    return myIdentity;
  }

  @Override
  public String toString() {
    return "DBSO:" + myIdentity;
  }

  public static DBStaticObject noInit(DBIdentity identity) {
    return identity != null ? new DBStaticObject(identity, null, null) : null;
  }

  public static class Builder {
    private final Map<DBAttribute<?>, Object> myValues = Collections15.hashMap();

    public Builder putSequence(DBAttribute<byte[]> attribute, ScalarSequence sequence) {
      myValues.put(attribute, sequence);
      return this;
    }

    public <T> Builder put(DBAttribute<T> attribute, T value) {
      if (value != null) myValues.put(attribute, value);
      return this;
    }

    public Builder putReference(DBAttribute<Long> attribute, DBIdentity identity) {
      return putReference(attribute, DBStaticObject.noInit(identity));
    }

    public Builder putReference(DBAttribute<Long> attribute, DBStaticObject identity) {
      if (identity != null) myValues.put(attribute, identity);
      return this;
    }

    public Builder putReference(DBAttribute<Long> attribute, DBIdentifiedObject object) {
      return putReference(attribute, DBIdentity.fromDBObject(object));
    }

    public DBStaticObject create(DBIdentity identity) {
      if (identity == null) return null;
      if (myValues.isEmpty()) return new DBStaticObject(identity, null, null);
      DBAttribute<?>[] attributes = myValues.keySet().toArray(new DBAttribute<?>[myValues.size()]);
      Object[] values = new Object[attributes.length];
      for (int i = 0; i < attributes.length; i++) {
        DBAttribute<?> attribute = attributes[i];
        values[i] = myValues.get(attribute);
      }
      return new DBStaticObject(identity, attributes, values);
    }
  }
}
