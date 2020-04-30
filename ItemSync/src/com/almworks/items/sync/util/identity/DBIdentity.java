package com.almworks.items.sync.util.identity;

import com.almworks.integers.LongArray;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.BadUtil;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DBIdentity implements ItemProxy {
  private static final TypedKey<Map<DBIdentity, Long>> RESOLUTION_CACHE = TypedKey.create("identity.resolutions");
  private static final TypedKey<TLongObjectHashMap<DBIdentity>> LOAD_CACHE = TypedKey.create("identity.load");

  public static final DBNamespace NS = DBNamespace.moduleNs("com.almworks.items.sync.util");
  public static final DBAttribute<Set<Long>> IDENTITY_ATTRIBUTES = NS.linkSet("identitySet");
  private static final Set<Class<?>> ALLOWED_CLASSES
    = Collections15.<Class<?>>unmodifiableSetCopy(Boolean.class, Integer.class, Long.class, String.class);

  private DBIdentity() {}

  public static DBIdentity fromDBObject(DBIdentifiedObject object) {
    if (object == null) return null;
    return new ObjectIdentity(object);
  }

  public static DBIdentity load(DBReader reader, long item) {
    LongArray loadStack = new LongArray();
    return priLoad(loadStack, reader, item, true);
  }

  @Nullable
  public static DBIdentity tryLoad(ItemVersion item) {
    return priLoad(new LongArray(), item.getReader(), item.getItem(), false);
  }

  private static DBIdentity priLoad(LongArray loadStack, DBReader reader, long item, boolean check) {
    TLongObjectHashMap<DBIdentity> cache = getLoadCache(reader);
    DBIdentity identity = cache.get(item);
    if (identity != null) return identity;
    Set<Long> attrItems = reader.getValue(item, IDENTITY_ATTRIBUTES);
    if (attrItems == null || attrItems.isEmpty()) {
      DBIdentity identifiedObject = loadIdentifiedObject(reader, item, check);
      if (identifiedObject != null) return identifiedObject;
      LogHelper.assertError(!check, "Missing identity", item);
      return null;
    }
    if (loadStack.contains(item)) {
      LogHelper.error("Identity loop detected", item, loadStack);
      return null;
    }
    loadStack.add(item);
    try {
      List<DBAttribute<?>> attrList = BadUtil.getAttributes(reader, attrItems);
      if (attrList == null || attrList.isEmpty()) return null;
      for (DBAttribute<?> attribute : attrList) if (attribute == null) return null;
      DBAttribute[] attributes = attrList.toArray(new DBAttribute[attrList.size()]);
      Arrays.sort(attributes, DBIdentifiedObject.BY_ID_ORDER);
      Object[] values = new Object[attributes.length];
      for (int i = 0; i < attributes.length; i++) {
        DBAttribute attribute = attributes[i];
        DBAttribute.ScalarComposition composition = attribute.getComposition();
        if (composition != DBAttribute.ScalarComposition.SCALAR) {
          LogHelper.error("Not scalar identity attribute", attribute, item);
          return null;
        }
        Class aClass = attribute.getScalarClass();
        if (!ALLOWED_CLASSES.contains(aClass)) {
          LogHelper.error("Not allowed class", aClass, attribute, item);
          return null;
        }
        Object value = reader.getValue(item, attribute);
        if (value == null) {
          LogHelper.error("Missing identity value", attribute, item);
          return null;
        }
        if (Long.class.equals(aClass)) {
          Long refItem = (Long) value;
          DBIdentity ref = priLoad(loadStack, reader, refItem, check);
          if (ref == null) {
            LogHelper.error("Failed to load reference", refItem, attribute, item);
            return null;
          }
          value = ref;
        }
        values[i] = value;
      }
      identity = new GenericIdentity(attributes, values);
      cache.put(item, identity);
      return identity;
    } finally {
      loadStack.remove(item);
    }
  }

  @SuppressWarnings( {"unchecked"})
  private static TLongObjectHashMap<DBIdentity> getLoadCache(DBReader reader) {
    Map cache = reader.getTransactionCache();
    TLongObjectHashMap<DBIdentity> map = LOAD_CACHE.getFrom(cache);
    if (map == null) {
      map = new TLongObjectHashMap<>();
      LOAD_CACHE.putTo(cache, map);
    }
    return map;
  }

  @SuppressWarnings( {"unchecked"})
  private static Map<DBIdentity, Long> getResolutionCache(DBReader reader) {
    Map cache = reader.getTransactionCache();
    Map<DBIdentity, Long> map = RESOLUTION_CACHE.getFrom(cache);
    if (map == null) {
      map = Collections15.hashMap();
      RESOLUTION_CACHE.putTo(cache, map);
    }
    return map;
  }

  private static DBIdentity loadIdentifiedObject(DBReader reader, long item, boolean check) {
    Long type = reader.getValue(item, DBAttribute.TYPE);
    DBIdentifiedObject object = null;
    if (type != null) {
      if (type == reader.findMaterialized(DBItemType.ATTRIBUTE)) object = BadUtil.getAttribute(reader, item);
      else if (type == reader.findMaterialized(DBItemType.TYPE)) object = BadUtil.getItemType(reader, item);
    }
    if (object == null) {
      String id = reader.getValue(item, DBAttribute.ID);
      if (id == null) {
        LogHelper.assertError(!check, "Not identified object", item);
        return null;
      }
      object = new DBIdentifiedObject(id);
    }
    return new ObjectIdentity(object);
  }

  @Nullable
  public static DBIdentifiedObject extractIdentifiedObject(DBIdentity identity) {
    if (identity == null) return null;
    ObjectIdentity objectIdentity = Util.cast(ObjectIdentity.class, identity);
    return objectIdentity != null ? objectIdentity.myObject : null;
  }

  public static class Builder {
    private final Map<DBAttribute<?>, Object> myValues = Collections15.hashMap();

    public Builder put(DBAttribute<String> attribute, String value) {
      LogHelper.assertError(!DBAttribute.ID.equals(attribute), "System id is not allowed", value);
      return priPut(attribute, value);
    }

    public Builder put(DBAttribute<Integer> attribute, int value) {
      return priPut(attribute, value);
    }

    public Builder put(DBAttribute<Long> attribute, DBIdentity identity) {
      return priPut(attribute, identity);
    }

    public Builder put(DBAttribute<Long> attribute, DBStaticObject object) {
      return priPut(attribute, object);
    }

    public Builder put(DBAttribute<Long> attribute, DBIdentifiedObject object) {
      return priPut(attribute, fromDBObject(object));
    }

    private Builder priPut(DBAttribute<?> attribute, Object value) {
      if (value == null) throw new NullPointerException("Null value for " + attribute);
      else myValues.put(attribute, value);
      return this;
    }

    public DBIdentity create() {
      DBAttribute[] attributes = myValues.keySet().toArray(new DBAttribute[myValues.size()]);
      Arrays.sort(attributes, DBIdentifiedObject.BY_ID_ORDER);
      Object[] values = new Object[attributes.length];
      for (int i = 0; i < attributes.length; i++) {
        DBAttribute attribute = attributes[i];
        Object value = myValues.get(attribute);
        values[i] = value;
      }
      return new GenericIdentity(attributes, values);
    }
  }

  private static class GenericIdentity extends DBIdentity {
    private final DBAttribute<?>[] myAttributes;
    private final Object[] myValues;
    private int myHashCode;

    private GenericIdentity(DBAttribute<?>[] attributes, Object[] values) {
      myAttributes = attributes;
      myValues = values;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      GenericIdentity other = Util.castNullable(GenericIdentity.class, obj);
      if (other == null) return false;
      if (hashCode() != other.hashCode()) return false;
      if (myAttributes.length != other.myAttributes.length) return false;
      for (int i = 0; i < myAttributes.length; i++) {
        DBAttribute<?> attribute = myAttributes[i];
        if (!Util.equals(attribute, other.myAttributes[i])) return false;
        if (!Util.equals(myValues[i], other.myValues[i])) return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      if (myHashCode == 0) {
        int hash = 0;
        for (int i = 0; i < myAttributes.length; i++) {
          DBAttribute<?> attribute = myAttributes[i];
          hash = hash ^ attribute.hashCode() ^ myValues[i].hashCode();
        }
        if (hash == 0) hash = 1;
        myHashCode = hash;
      }
      return myHashCode;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder("DBIdentity{");
      String sep = "";
      for (int i = 0; i < myAttributes.length; i++) {
        DBAttribute<?> attribute = myAttributes[i];
        builder.append(sep);
        sep = ", ";
        builder.append(attribute).append("=").append(myValues[i]);
      }
      return builder.append("}").toString();
    }

    public long findOrCreate(DBDrain drain) {
      Map<DBIdentity, Long> cache = getResolutionCache(drain.getReader());
      Long known = cache.get(this);
      if (known != null && known > 0) return known;
      long item = doSearchItem(drain.getReader());
      if (item > 0) {
        drain.changeItem(item).setAlive();
        cache.put(this, item);
        return item;
      }
      item = doCreate(drain);
      if (item > 0) cache.put(this, item);
      return item;
    }

    private long doCreate(DBDrain drain) {
      Object[] dbValues = new Object[myAttributes.length];
      for (int i = 0; i < myAttributes.length; i++) {
        DBAttribute<?> attribute = myAttributes[i];
        Object value = myValues[i];
        if (value == null) {
          LogHelper.error("Illegal value for", attribute);
          return 0;
        }
        if (Long.class.equals(attribute.getScalarClass())) {
          value = DBStaticObject.toDBValue(drain, value);
          Long valueItem = Util.castNullable(Long.class, value);
          if (valueItem == null || valueItem <= 0) {
            LogHelper.error("Illegal item", attribute, value);
            return 0;
          }
          value = valueItem;
        }
        dbValues[i] = value;
      }
      ItemVersionCreator creator = drain.createItem();
      for (int i = 0; i < myAttributes.length; i++) {
        DBAttribute<Object> attribute = (DBAttribute<Object>) myAttributes[i];
        creator.setValue(attribute, dbValues[i]);
      }
      LongArray attrItems = new LongArray();
      for (DBAttribute<?> attribute : myAttributes) attrItems.add(drain.materialize(attribute));
      creator.setSet(IDENTITY_ATTRIBUTES, attrItems);
      return creator.getItem();
    }

    @Override
    public long findItem(DBReader reader) {
      Map<DBIdentity, Long> resolutions = getResolutionCache(reader);
      Long item = resolutions.get(this);
      if (item != null && item > 0) return item;
      long found = doSearchItem(reader);
      if (found > 0) resolutions.put(this, found);
      return found;
    }

    private long doSearchItem(DBReader reader) {
      List<BoolExpr<DP>> exprs = Collections15.arrayList();
      for (int i = 0; i < myAttributes.length; i++) {
        DBAttribute<?> attribute = myAttributes[i];
        Object value = myValues[i];
        if (Long.class.equals(attribute.getScalarClass())) {
          ItemReference identity = (ItemReference) value;
          long valueItem = identity.findItem(reader);
          if (valueItem <= 0) return 0;
          value = valueItem;
        }
        exprs.add(DPEquals.create((DBAttribute<Object>) attribute, value));
      }
      LongArray items = reader.query(BoolExpr.and(exprs)).copyItemsSorted();
      if (items.isEmpty()) return 0;
      return chooseResolution(reader, items);
    }

    private long chooseResolution(DBReader reader, LongArray array) {
      LongArray candidates = new LongArray();
      for (int i = 0; i < array.size(); i++) {
        long item = array.get(i);
        Set<Long> attrItems = reader.getValue(item, IDENTITY_ATTRIBUTES);
        boolean hasIdentity = attrItems != null && attrItems.size() == myAttributes.length;
        if (!hasIdentity) continue;
        boolean allAttributes = true;
        for (DBAttribute<?> attribute : myAttributes) {
          if (!attrItems.contains(reader.findMaterialized(attribute))) {
            allAttributes = false;
            break;
          }
        }
        if (!allAttributes) continue;
        candidates.add(item);
      }
      if (candidates.isEmpty()) return 0;
      LogHelper.assertError(candidates.size() == 1, "Not unique resolution", this, candidates);
      return candidates.get(0);
    }
  }

  private static class ObjectIdentity extends DBIdentity {
    private final DBIdentifiedObject myObject;

    private ObjectIdentity(DBIdentifiedObject object) {
      super();
      myObject = object;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      ObjectIdentity other = Util.castNullable(ObjectIdentity.class, obj);
      return other != null && myObject.equals(other.myObject);
    }

    @Override
    public int hashCode() {
      return myObject.hashCode();
    }

    @Override
    public String toString() {
      return "DBIdentity{" + myObject + "}";
    }

    public long findOrCreate(DBDrain drain) {
      return drain.materialize(myObject);
    }

    @Override
    public long findItem(DBReader reader) {
      return reader.findMaterialized(myObject);
    }
  }
}
