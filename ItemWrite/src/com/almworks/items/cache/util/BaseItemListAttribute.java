package com.almworks.items.cache.util;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseItemListAttribute implements DataLoader<LongList> {
  private final DBAttribute<List<Long>> myAttribute;

  public BaseItemListAttribute(DBAttribute<List<Long>> attribute) {
    myAttribute = attribute;
  }

  @Override
  public final List<LongList> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate) {
    ArrayList<LongList> result = Collections15.arrayList();
    for (int i = 0; i < items.size(); i++) {
      long item = items.get(i);
      result.add(getValue(reader, item));
    }
    return result;
  }

  public final LongList getValue(DBReader reader, long item) {
    return convert(reader.getValue(item, myAttribute));
  }

  public final LongList getValue(ItemVersion item) {
    return convert(item.getValue(myAttribute));
  }

  public final DBAttribute<List<Long>> getAttribute() {
    return myAttribute;
  }

  protected abstract LongList convert(List<Long> values);
}
