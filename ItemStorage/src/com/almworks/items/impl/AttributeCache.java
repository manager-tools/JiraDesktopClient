package com.almworks.items.impl;

import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.impl.sqlite.DatabaseContext;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.items.util.DatabaseUtil;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class AttributeCache extends PassiveCache {
  private static final TypedKey<AttributeCache> CACHE = TypedKey.create("attributeCache");

  private static final BoolExpr<DP> ATTRIBUTES = DPEqualsIdentified.create(DBAttribute.TYPE, DBItemType.ATTRIBUTE);

  private final Map<Long, DBAttribute> myAttributes = Collections15.hashMap();
  private final List<DBAttribute> myPropagatingAttributes = Collections15.arrayList();
  private final List<DBAttribute> myPropagatingAttributesRO = Collections.unmodifiableList(myPropagatingAttributes);
  private final Set<DBAttribute> myAttributeSet = new CopyOnWriteArraySet<DBAttribute>();

  public AttributeCache(DatabaseContext context) {
    super(context, ATTRIBUTES);
  }

  public static AttributeCache get(TransactionContext context) {
    Map map = context.getSessionContext().getSessionCache();
    AttributeCache cache = CACHE.getFrom(map);
    if (cache == null) {
      cache = new AttributeCache(context.getDatabaseContext());
      CACHE.putTo(map, cache);
    }
    return cache;
  }

  @Override
  protected void beforeUpdate(DBEvent event, TransactionContext context) {
    LongList removed = event.getRemovedAndChangedSorted();
    for (int i = 0; i < removed.size(); i++) {
      removeCachedAttribute(removed.get(i));
    }
  }

  @Override
  protected void afterUpdate(DBEvent event, TransactionContext context) throws SQLiteException {
    LongList attributes = event.getAddedAndChangedSorted();
    DBReaderImpl reader = new DBReaderImpl(context);
    for (int i = 0; i < attributes.size(); i++) {
      long item = attributes.get(i);
      removeCachedAttribute(item);
      DBAttribute attribute = DatabaseUtil.attributeFromArtifact(item, reader);
      if (attribute == null) {
        Log.warn("cannot read attribute [" + item + "]");
        continue;
      }
      addAttribute(item, attribute);
    }
  }

  private void addAttribute(long item, DBAttribute attribute) {
    DBAttribute expunged = myAttributes.put(item, attribute);
    if (expunged != null) {
      if (!expunged.equals(attribute)) {
        Log.warn("attribute " + attribute + " expunged " + expunged + " on item " + item);
      }
      myPropagatingAttributes.remove(expunged);
      myAttributeSet.remove(expunged);
    }
    myAttributeSet.add(attribute);
    if (attribute.isPropagatingChange())
      myPropagatingAttributes.add(attribute);
  }

  private void removeCachedAttribute(long item) {
    DBAttribute attribute = myAttributes.remove(item);
    myPropagatingAttributes.remove(attribute);
    myAttributeSet.remove(attribute);
  }

  public DBAttribute getAttributeById(String id, TransactionContext context) {
    long item = IdentifiedObjectCache.get(context).getItemById(id, context);
    return getAttributeByItem(item, context);
  }

  @Nullable
  public DBAttribute getAttributeByItem(long item, TransactionContext context) {
    if (item <= 0)
      return null;
    try {
      validate(context);
      return myAttributes.get(item);
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }

  private Set<DBAttribute> getAttributes0(TransactionContext context) {
    try {
      validate(context);
      return myAttributeSet;
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }

  public List<DBAttribute> getPropagatingAttributes(TransactionContext context) throws SQLiteException {
    validate(context);
    return myPropagatingAttributesRO;
  }

  public static DBAttribute getAttribute(String id, TransactionContext context) {
    return AttributeCache.get(context).getAttributeById(id, context);
  }

  public static Set<DBAttribute> getAttributes(TransactionContext context) {
    return AttributeCache.get(context).getAttributes0(context);
  }
}
