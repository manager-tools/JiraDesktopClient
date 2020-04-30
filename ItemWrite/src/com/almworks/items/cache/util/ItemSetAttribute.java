package com.almworks.items.cache.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reference to set of items
 */
public class ItemSetAttribute implements DataLoader<LongList> {
  private final DBAttribute<Set<Long>> myAttribute;

  public ItemSetAttribute(DBAttribute<Set<Long>> attribute) {
    myAttribute = attribute;
  }

  @Override
  public List<LongList> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate) {
    List<Set<Long>> values = myAttribute.collectValues(items, reader);
    ArrayList<LongList> result = Collections15.arrayList();
    for (Set<Long> value : values) {
      if (value == null || value.isEmpty()) result.add(LongList.EMPTY);
      else {
        LongArray array = LongArray.create(value);
        array.sortUnique();
        result.add(array);
      }
    }
    return result;
  }

  public DBAttribute<Set<Long>> getAttribute() {
    return myAttribute;
  }

  @NotNull
  public LongList getValue(ItemVersion item) {
    if (item == null) return LongList.EMPTY;
    Set<Long> value = item.getValue(myAttribute);
    if (value == null ||value.isEmpty()) return LongList.EMPTY;
    LongArray array = LongArray.create(value);
    array.sortUnique();
    return array;
  }

  public void setValue(ItemVersionCreator creator, LongList value) {
    if (value == null) value = LongList.EMPTY;
    creator.setSet(myAttribute, value);
  }
}
