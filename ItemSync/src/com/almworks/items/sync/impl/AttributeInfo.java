package com.almworks.items.sync.impl;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.util.BadUtil;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AttributeInfo {
  private static final TypedKey<AttributeInfo> KEY = TypedKey.create("attributeInfo");
  private final Set<String> myShadowable = Collections15.hashSet();
  private final Set<String> myNotShadowable = Collections15.hashSet();
  private final Set<String> myNotMaterialized = Collections15.hashSet();
  private final TLongObjectHashMap<DBAttribute<?>> myAttributes = new TLongObjectHashMap<>();
  private final DBReader myReader;
  private DBWriter myWriter;

  public AttributeInfo(DBReader reader) {
    assert reader != null;
    myReader = reader;
  }

  public static AttributeInfo instance(DBReader reader) {
    if (reader instanceof DBWriter) return instance((DBWriter)reader);
    return priInstance(reader);
  }

  @SuppressWarnings({"unchecked"})
  private static AttributeInfo priInstance(DBReader reader) {
    Map cache = reader.getTransactionCache();
    AttributeInfo info = KEY.getFrom(cache);
    if (info == null) {
      info = new AttributeInfo(reader);
      KEY.putTo(cache, info);
    }
    return info;
  }

  public static AttributeInfo instance(DBWriter writer) {
    AttributeInfo info = priInstance(writer);
    if (info.myWriter == null) {
      info.myWriter = writer;
      info.myNotMaterialized.clear();
    }
    return info;
  }

  @NotNull
  public DBReader getReader() {
    return myReader;
  }

  public boolean isShadowable(DBAttribute<?> attribute) {
    return isSure(attribute, true);
  }

  public boolean isWriteThrough(DBAttribute<?> attribute) {
    return isSure(attribute, false);
  }

  private boolean isSure(DBAttribute<?> attribute, boolean isShadowable) {
    if (attribute == null) return false;
    String id = attribute.getId();
    Set<String> yes = isShadowable ? myShadowable : myNotShadowable;
    Set<String> no = isShadowable ? myNotShadowable : myShadowable;
    if (yes.contains(id)) return true;
    if (no.contains(id)) return false;
    if (myWriter == null && myNotMaterialized.contains(id)) return false;
    long attrItem = myReader.findMaterialized(attribute);
    if (attrItem <= 0) {
      if (myWriter != null) attrItem = myWriter.materialize(attribute);
      else {
        myNotMaterialized.add(id);
        return false;
      }
    }
    myAttributes.put(attrItem, attribute);
    return loadIsShadowable(attrItem, id) == isShadowable;
  }

  private boolean loadIsShadowable(long attrItem, String id) {
    VersionHolder attrHolder = HolderCache.instance(myReader).getHolder(attrItem, null, true);
    boolean shadowable = Boolean.TRUE.equals(attrHolder.getValue(SyncSchema.IS_SHADOWABLE));
    (shadowable ? myShadowable : myNotShadowable).add(id);
    return shadowable;
  }

  public DBAttribute<?> getAttribute(long attrItem) {
    DBAttribute<?> attribute = myAttributes.get(attrItem);
    if (attribute == null) {
      attribute = BadUtil.getAttribute(myReader, attrItem);
      if (attribute == null) return null;
      loadIsShadowable(attrItem, attribute.getId());
      myAttributes.put(attrItem, attribute);
    }
    return attribute;
  }

  public Collection<DBAttribute<?>> getAllAttributes() {
    LongArray allAttributes =
      myReader.query(DPEqualsIdentified.create(DBAttribute.TYPE, DBItemType.ATTRIBUTE)).copyItemsSorted();
    List<DBAttribute<?>> result = Collections15.arrayList();
    for (int i = 0; i < allAttributes.size(); i++) {
      long attrItem = allAttributes.get(i);
      DBAttribute<?> attr = getAttribute(attrItem);
      if (attr == null) Log.error("Failed to load attribute " + attrItem + " " + myReader.getAttributeMap(attrItem));
      else result.add(attr);
    }
    return result;
  }

  @Nullable
  public static DBAttribute<?> getAttribute(VersionSource db, long attr) {
    AttributeInfo info = instance(db.getReader());
    DBAttribute<?> attribute = info.getAttribute(attr);
    if (attribute == null) Log.error("Attribute not found for " + attr);
    return attribute;
  }

  public static DBAttribute<?> getAttribute(ItemVersion attr) {
    return getAttribute(attr, attr.getItem());
  }
}
