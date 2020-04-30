package com.almworks.items.impl.dbadapter.util;

import com.almworks.integers.LongIterator;
import com.almworks.items.impl.dbadapter.ItemAccessor;
import com.almworks.items.impl.dbadapter.ItemSetAccessor;
import org.almworks.util.Collections15;

import java.util.Iterator;

public class SingleItemSetAccessor implements ItemSetAccessor {
  private static final boolean[] DONT_CARE = {false};
  private final ItemAccessor myValue;

  public SingleItemSetAccessor(ItemAccessor value) {
    myValue = value;
  }

  public String toString() {
    return "[" + myValue + "]";
  }

  public <T> T visit(Visitor<T> visitor) {
    return visitor.visit(myValue, DONT_CARE);
  }

  public LongIterator getItems() {
    return new LongIterator.Single(myValue.getItem());
  }

  public int getCount() {
    return 1;
  }

  public ItemAccessor getFirst() {
    return myValue;
  }

  public boolean isEmpty() {
    return false;
  }

  public Iterator<ItemAccessor> iterator() {
    return Collections15.singletonIterator(myValue);
  }
}
