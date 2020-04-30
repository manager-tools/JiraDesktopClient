package com.almworks.items.cache.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlobLongListAttribute implements DataLoader<LongList> {
  private final DBAttribute<byte[]> myAttribute;

  public BlobLongListAttribute(DBAttribute<byte[]> attribute) {
    myAttribute = attribute;
  }

  @Override
  public List<LongList> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate) {
    List<LongList> result = Collections15.arrayList();
    for (byte[] array : myAttribute.collectValues(items, reader)) {
      result.add(loadArray(array));
    }
    return result;
  }

  @NotNull
  public LongList getValue(DBReader reader, long item) {
    return loadArray(reader.getValue(item, myAttribute));
  }

  @NotNull
  public LongList getValue(ItemVersion item) {
    return loadArray(item.getValue(myAttribute));
  }

  @NotNull
  private static LongList loadArray(byte[] array) {
    if (array == null || array.length == 0) return LongList.EMPTY;
    if (array.length % 8 != 0) {
      LogHelper.error("Wrong array length", ArrayUtil.toString(array));
      return LongList.EMPTY;
    }
    int count = array.length / 8;
    LongArray result = new LongArray(count);
    for (int i = 0; i < count; i++) result.add(ByteArray.getLong(array, 8 * i));
    return result;
  }

  @Override
  public String toString() {
    return "long list@" + myAttribute;
  }

  public void setValue(ItemVersionCreator creator, List<? extends ItemProxy> value) {
    creator.setSequence(myAttribute, ScalarSequence.create(value));
  }
}
