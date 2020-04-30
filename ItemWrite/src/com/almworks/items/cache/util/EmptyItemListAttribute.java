package com.almworks.items.cache.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;

import java.util.List;

/**
 * Converts list with single zero element to empty list, returns null for no value (null).
 */
public class EmptyItemListAttribute extends BaseItemListAttribute {
  public EmptyItemListAttribute(DBAttribute<List<Long>> attribute) {
    super(attribute);
  }

  @Override
  public String toString() {
    return "EmptyItemListAttribute[" + getAttribute() + "]";
  }

  @Override
  protected LongList convert(List<Long> values) {
    if (values == null || values.isEmpty()) return null;
    if (values.size() == 1 && values.get(0) == 0) return LongList.EMPTY;
    return LongArray.create(values);
  }
}
