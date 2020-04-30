package com.almworks.items.cache.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.api.ItemReference;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Reference to item.
 */
public class ItemAttribute implements DataLoader<Long> {
  private final DBAttribute<Long> myAttribute;

  public ItemAttribute(DBAttribute<Long> attribute) {
    myAttribute = attribute;
  }

  @Override
  public List<Long> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate) {
    return myAttribute.collectValues(items, reader);
  }

  @Override
  public String toString() {
    return "ItemAttribute[" + myAttribute + "]";
  }

  @NotNull
  public LongList collectValues(DBReader reader, LongList items) {
    List<Long> dbValues = myAttribute.collectValues(items, reader);
    if (dbValues == null || dbValues.isEmpty()) return LongList.EMPTY;
    long[] host = new long[dbValues.size()];
    for (int i = 0, dbValuesSize = dbValues.size(); i < dbValuesSize; i++) {
      Long value = dbValues.get(i);
      host[i] = value != null ? value : 0;
    }
    return new LongArray(host);
  }

  public ItemVersion readValue(ItemVersion item) {
    return item.readValue(myAttribute);
  }

  public long getValue(CachedItem known) {
    if (known == null) return 0;
    Long value = known.getValue(this);
    return value != null && value > 0 ? value : 0;
  }

  public long getValue(ItemVersion item) {
    Long value = item.getValue(myAttribute);
    return value != null && value > 0 ? value : 0;
  }

  public long getValue(DBReader reader, long item) {
    Long value = reader.getValue(item, myAttribute);
    return value != null && value > 0 ? value : 0;
  }

  public DBAttribute<Long> getAttribute() {
    return myAttribute;
  }

  public BoolExpr<DP> queryEqual(ItemReference object) {
    if (object == null) return BoolExpr.FALSE();
    return DPEqualsIdentified.create(myAttribute, object);
  }

  public BoolExpr<DP> queryEqual(long item) {
    if (item <= 0) return BoolExpr.FALSE();
    return DPEquals.create(myAttribute, item);
  }

  public void setValue(ItemVersionCreator creator, ItemVersion value) {
    creator.setValue(myAttribute, value);
  }

  public void setValue(ItemVersionCreator creator, Long value) {
    setValue(creator, value != null && value > 0 ? value.longValue() : 0l);
    creator.setValue(myAttribute, value);
  }

  public void setValue(ItemVersionCreator creator, long value) {
    creator.setValue(myAttribute, value > 0 ? value : null);
  }
}
