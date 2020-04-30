package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.integers.LongArray;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.dbwrite.impl.DBObjectsCache;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.BadUtil;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class AttributeCache {
  private final Map<EntityKey<?>, KeyCounterpart> myAttributes = Collections15.hashMap();
  private final DBObjectsCache myCache;
  private final DBReader myReader;
  private final Map<DBAttribute<?>, Boolean> myShadowableCache = Collections15.hashMap();

  AttributeCache(DBNamespace nameSpace, DBReader reader) {
    myReader = reader;
    myCache = new DBObjectsCache(nameSpace);
  }

  @NotNull
  public KeyCounterpart getCounterpart(KeyInfo info) {
    EntityKey<?> key = info.getKey();
    KeyCounterpart counterpart = myAttributes.get(key);
    if (counterpart == null) {
      counterpart = createKeyCounterpart(info);
      myAttributes.put(key, counterpart);
    }
    return counterpart;
  }

  @NotNull
  private KeyCounterpart createKeyCounterpart(KeyInfo info) {
    KeyInfo.HintInfo hint = Util.castNullable(KeyInfo.HintInfo.class, info);
    if (hint != null) return new HintCounterpart(info);
    KeyInfo.SimpleColumn scalar = Util.castNullable(KeyInfo.SimpleColumn.class, info);
    DBAttribute<?> attribute = myCache.getAttribute(info.getKey().toEntity());
    if (scalar != null) {
      if (attribute == null) {
        LogHelper.error("No attribute", info);
        return new HintCounterpart(info);
      }
      //noinspection unchecked
      return ScalarCounterpart.create(scalar, attribute);
    }
    KeyInfo.EntityColumn entity = Util.castNullable(KeyInfo.EntityColumn.class, info);
    if (entity != null) {
      DBAttribute<Long> refAttribute = BadUtil.castScalar(Long.class, attribute);
      if (refAttribute == null) {
        LogHelper.error("Wrong attribute", attribute, info);
        return new HintCounterpart(info);
      }
      return new EntityCounterpart(info, refAttribute);
    }
    KeyInfo.EntityCollectionInfo entityCollection = Util.castNullable(KeyInfo.EntityCollectionInfo.class, info);
    if (entityCollection != null) {
      DBAttribute<Set<Long>> refSetAttribute = BadUtil.castSetAttribute(Long.class, attribute);
      if (refSetAttribute == null) {
        LogHelper.error("Wrong attribute", attribute, info);
        return new HintCounterpart(info);
      }
      return new EntityCollection(info, refSetAttribute);
    }
    KeyInfo.EntityOrderInfo entityOrder = Util.castNullable(KeyInfo.EntityOrderInfo.class, info);
    if (entityOrder != null) {
      DBAttribute<List<Long>> refListAttribute = BadUtil.castListAttribute(Long.class, attribute);
      if (refListAttribute == null) {
        LogHelper.error("Wrong attribute", attribute, info);
        return new HintCounterpart(info);
      }
      return new EntityOrder(info, refListAttribute);
    }
    LogHelper.error("Unknown info", info);
    return new HintCounterpart(info);
  }

  public DBItemType getType(Entity entityType) {
    return myCache.getType(entityType);
  }

  public DBNamespace getNamespace() {
    return myCache.getNamespace();
  }

  public boolean isShadowable(DBAttribute<?> attribute) {
    Boolean shadowable = myShadowableCache.get(attribute);
    if (shadowable == null) {
      long item = myReader.findMaterialized(attribute);
      if (item <= 0) return false;
      shadowable = SyncAttributes.isShadowable(myReader, item);
      myShadowableCache.put(attribute, shadowable);
    }
    return shadowable;
  }

  private class HintCounterpart implements KeyCounterpart {
    private final KeyInfo myInfo;

    public HintCounterpart(KeyInfo info) {
      myInfo = info;
    }

    @Override
    public BoolExpr<DP> query(WriteState state, Object value) {
      LogHelper.error("Query not supported", this, value);
      return null;
    }

    @Override
    public BoolExpr<DP> queryOneOf(WriteState state, Collection<Object> entityValues) {
      LogHelper.error("Query not supported", this, entityValues);
      return null;
    }

    @Override
    public DBAttribute<?> getAttribute() {
      return null;
    }

    @Override
    public void update(WriteState state, ItemVersionCreator item, EntityPlace place) {
      LogHelper.error("Not supported", item, this, place);
    }

    @Override
    public void update(WriteState state, ItemVersionCreator item, Object value) {
      LogHelper.error("Not supported", item, this, value);
    }

    @Override
    public boolean equalValue(WriteState state, Object dbValue, Object entityValue) {
      LogHelper.error("Not supported", dbValue, entityValue, this);
      return false;
    }

    @Override
    public String toString() {
      return "Hint:" + myInfo;
    }

    @Override
    public Object convertToDB(WriteState state, Object value) {
      LogHelper.error("Not supported", value);
      return null;
    }
  }

  private static class ScalarCounterpart<T> implements KeyCounterpart {
    private final KeyInfo myInfo;
    private final DBAttribute<T> myAttribute;

    public ScalarCounterpart(KeyInfo.SimpleColumn<T> info, DBAttribute<T> attribute) {
      myInfo = info;
      myAttribute = attribute;
    }
    
    public static <T> ScalarCounterpart<T> create(KeyInfo.SimpleColumn<T> info, DBAttribute<T> attribute) {
      return new ScalarCounterpart<T>(info, attribute);
    }

    @Override
    public BoolExpr<DP> query(WriteState state, Object value) {
      T dbValue = convertToDB(state, value);
      if (dbValue == null) return null;
      return DPEquals.create(myAttribute, dbValue);
    }

    @Override
    public T convertToDB(WriteState state, Object value) {
      if (value == null || value == ValueRow.NULL_VALUE) {
        LogHelper.error("Can not query null", value, this);
        return null;
      }
      T dbValue = Util.castNullable(myAttribute.getValueClass(), value);
      if (dbValue == null) {
        LogHelper.error("Wrong value", value, this);
        return null;
      }
      return dbValue;
    }

    @Override
    public BoolExpr<DP> queryOneOf(WriteState state, Collection<Object> entityValues) {
      if (!Comparable.class.isAssignableFrom(myAttribute.getValueClass())) return null;
      DBAttribute<Comparable> attribute = (DBAttribute<Comparable>) myAttribute;
      ArrayList<T> dbValues = Collections15.arrayList();
      for (Object value : entityValues) {
        T dbValue = convertToDB(state, value);
        if (dbValue == null) return null;
        dbValues.add(dbValue);
      }
      if (dbValues.isEmpty()) return BoolExpr.FALSE();
      return DPEquals.equalOneOf(attribute, (List<Comparable>)dbValues);
    }

    @Override
    public DBAttribute<?> getAttribute() {
      return myAttribute;
    }

    @Override
    public void update(WriteState state, ItemVersionCreator item, EntityPlace place) {
      Object value = place.getValue(myInfo);
      update(state, item, value);
    }

    @Override
    public void update(WriteState state, ItemVersionCreator item, Object value) {
      if (value == null) return;
      if (value == ValueRow.NULL_VALUE) value = null;
      //noinspection unchecked
      item.setValue(myAttribute, (T) value);
    }

    @Override
    public boolean equalValue(WriteState state, Object dbValue, Object entityValue) {
      if (entityValue == null) return false;
      if (dbValue == null) return entityValue == ValueRow.NULL_VALUE;
      return Util.equals(dbValue, entityValue);
    }

    @Override
    public String toString() {
      return "Scalar:" + myInfo + " " + myAttribute;
    }
  }


  private class EntityCounterpart implements KeyCounterpart {
    private final KeyInfo myInfo;
    private final DBAttribute<Long> myAttribute;

    public EntityCounterpart(KeyInfo info, DBAttribute<Long> attribute) {
      myInfo = info;
      myAttribute = attribute;
    }

    @Override
    public BoolExpr<DP> query(WriteState state, Object value) {
      EntityPlace place = convertToPlace(value);
      if (place == null) return null;
      long item = state.getItem(place);
      if (item <= 0) return BoolExpr.FALSE();
      return DPEquals.create(myAttribute, item);
    }

    private EntityPlace convertToPlace(Object value) {
      if (value == null || value == ValueRow.NULL_VALUE) {
        LogHelper.error("Can not query null", value, this);
        return null;
      }
      EntityPlace place = castEntity(value);
      if (place == null)
        return null;
      return place;
    }

    @Override
    public Long convertToDB(WriteState state, Object value) {
      EntityPlace place = convertToPlace(value);
      if (place == null) return null;
      long item = state.getItem(place);
      return item > 0 ? item : null;
    }

    @Override
    public BoolExpr<DP> queryOneOf(WriteState state, Collection<Object> entityValues) {
      LongArray dbValues = new LongArray();
      for (Object entityValue : entityValues) {
        EntityPlace place = convertToPlace(entityValue);
        if (place == null) return null;
        long item = state.getItem(place);
        if (item > 0) dbValues.add(item);
      }
      if (dbValues.isEmpty()) return BoolExpr.FALSE();
      return DPEquals.equalOneOf(myAttribute, dbValues);
    }

    private EntityPlace castEntity(Object value) {
      EntityPlace place = Util.castNullable(EntityPlace.class, value);
      if (place == null) {
        LogHelper.error("Wrong value", value, this);
        return null;
      }
      return place;
    }
    
    private long getEntityItem(WriteState state, Object entityValue) {
      EntityPlace place = castEntity(entityValue);
      if (place == null) return 0;
      long item = state.getItem(place);
      if (item <= 0) {
        LogHelper.error("Unresolved", entityValue, item, myInfo);
        return 0;
      }
      return item;
    }

    @Override
    public DBAttribute<?> getAttribute() {
      return myAttribute;
    }

    @Override
    public void update(WriteState state, ItemVersionCreator item, EntityPlace place) {
      Object value = place.getValue(myInfo);
      update(state, item, value);
    }

    public void update(WriteState state, ItemVersionCreator item, Object value) {
      if (value == null) return;
      if (value == ValueRow.NULL_VALUE) {
        item.setValue(myAttribute, (Long)null);
        return;
      }
      long valueItem = getEntityItem(state, value);
      if (valueItem <= 0) throw new DBOperationCancelledException();
      else item.setValue(myAttribute, valueItem);
    }

    @Override
    public boolean equalValue(WriteState state, Object dbValue, Object entityValue) {
      if (entityValue == null) return false;
      if (dbValue == null) return entityValue == ValueRow.NULL_VALUE;
      Long itemValue = Util.castNullable(Long.class, dbValue);
      if (itemValue == null) {
        LogHelper.error("Wrong db value class", dbValue, entityValue, myInfo);
        return false;
      }
      if (itemValue <= 0) {
        LogHelper.error("Wrong db value", dbValue, entityValue, myInfo);
        return entityValue == ValueRow.NULL_VALUE;
      }
      //noinspection SimplifiableIfStatement
      if (entityValue == ValueRow.NULL_VALUE) return false;
      return getEntityItem(state, entityValue) == itemValue;
    }

    @Override
    public String toString() {
      return "Entity:" + myInfo + " " + myAttribute;
    }
  }

  private abstract class BaseEntityCollection implements KeyCounterpart {
    private final KeyInfo myInfo;

    private BaseEntityCollection(KeyInfo info) {
      myInfo = info;
    }

    protected KeyInfo getInfo() {
      return myInfo;
    }

    @Override
    public BoolExpr<DP> query(WriteState state, Object value) {
      LogHelper.error("Query not supported", this, value);
      return null;
    }

    @Override
    public BoolExpr<DP> queryOneOf(WriteState state, Collection<Object> entityValues) {
      LogHelper.error("Query not supported", this, entityValues);
      return null;
    }

    @Override
    public void update(WriteState state, ItemVersionCreator item, EntityPlace place) {
      Object value = place.getValue(myInfo);
      update(state, item, value);
    }

    public void update(WriteState state, ItemVersionCreator item, Object value) {
      if (value == null) return;
      if (value == ValueRow.NULL_VALUE) {
        setValue(item, null);
        return;
      }
      EntityPlace[] array = Util.castNullable(EntityPlace[].class, value);
      if (array == null) {
        LogHelper.error("Wrong value", value, this);
        throw new DBOperationCancelledException();
      }
      setValue(item, convertToItems(state, array));
    }

    private LongArray convertToItems(WriteState state, EntityPlace[] array) {
      if (array == null || array.length == 0) return null;
      LongArray valueItems = new LongArray();
      for (Object obj : array) {
        EntityPlace entity = Util.castNullable(EntityPlace.class, obj);
        if (entity == null) {
          LogHelper.error("Wrong element class", obj, this);
          throw new DBOperationCancelledException();
        }
        long valueItem = state.getItem(entity);
        if (valueItem < 0) {
          LogHelper.error("Unresolved", entity, this);
          throw new DBOperationCancelledException();
        }
        valueItems.add(valueItem);
      }
      valueItems.sortUnique();
      return valueItems;
    }

    @Override
    public Object convertToDB(WriteState state, Object value) {
      if (value == null || value == ValueRow.NULL_VALUE) return null;
      EntityPlace[] array = Util.castNullable(EntityPlace[].class, value);
      if (array == null) {
        LogHelper.error("Wrong value class", this, value);
        return null;
      }
      return convertToItems(state, array);
    }

    @Override
    public boolean equalValue(WriteState state, Object dbValue, Object entityValue) {
      LogHelper.error("Not implemented yet", myInfo, dbValue, entityValue);
      return false;
    }

    protected abstract void setValue(ItemVersionCreator item, @Nullable LongArray valueItems);
  }
  
  private class EntityCollection extends BaseEntityCollection {
    private final DBAttribute<Set<Long>> myAttribute;

    public EntityCollection(KeyInfo info, DBAttribute<Set<Long>> attribute) {
      super(info);
      myAttribute = attribute;
    }

    @Override
    public String toString() {
      return "EntitySet:" + getInfo() + " " + myAttribute;
    }

    @Override
    public DBAttribute<?> getAttribute() {
      return myAttribute;
    }

    @Override
    protected void setValue(ItemVersionCreator item, @Nullable LongArray valueItems) {
      item.setSet(myAttribute, valueItems);
    }
  }

  private class EntityOrder extends BaseEntityCollection {
    private final DBAttribute<List<Long>> myAttribute;

    public EntityOrder(KeyInfo info, DBAttribute<List<Long>> refListAttribute) {
      super(info);
      myAttribute = refListAttribute;
    }

    @Override
    public String toString() {
      return "EntityList:" + getInfo() + " " + myAttribute;
    }

    @Override
    public DBAttribute<?> getAttribute() {
      return myAttribute;
    }

    @Override
    protected void setValue(ItemVersionCreator item, @Nullable LongArray valueItems) {
      item.setList(myAttribute, valueItems);
    }
  }
}
