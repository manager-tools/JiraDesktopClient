package com.almworks.items.cache.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;

import java.util.List;

/**
 * Reference to list of items
 */
public class ItemListAttribute extends BaseItemListAttribute {

  public ItemListAttribute(DBAttribute<List<Long>> attribute) {
    super(attribute);
  }

  @Override
  public String toString() {
    return "ItemListAttribute[" + getAttribute() + "]";
  }

  @Override
  protected LongList convert(List<Long> values) {
    if (values == null || values.isEmpty()) return LongList.EMPTY;
    return LongArray.create(values);
  }
}
