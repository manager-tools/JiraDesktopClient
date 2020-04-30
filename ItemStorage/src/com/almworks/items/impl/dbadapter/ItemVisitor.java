package com.almworks.items.impl.dbadapter;

import com.almworks.integers.LongCollector;
import com.almworks.integers.LongIterable;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongNativeArrayIterator;
import com.almworks.sqlite4java.SQLiteException;

/**
 * An interface to get items loaded from some source.
 * <p>
 * The items passed via vist... methods are not guaranteed to come in any particular order, nor be unique.
 */
public interface ItemVisitor {
  boolean visitItems(long[] items, int offset, int length) throws SQLiteException;

  boolean visitItems(LongIterable items) throws SQLiteException;

  abstract class ForEachItem implements ItemVisitor {
    protected abstract boolean visitItem(long item) throws SQLiteException;

    @Override
    public boolean visitItems(long[] items, int offset, int length) throws SQLiteException {
      for (int i = 0; i < length; i++) {
        if (!visitItem(items[offset + i])) return false;
      }
      return true;
    }

    @Override
    public boolean visitItems(LongIterable items) throws SQLiteException {
      for (LongIterator ii = items.iterator(); ii.hasNext(); ) {
        if (!visitItem(ii.nextValue())) return false;
      }
      return true;
    }
  }
  
  class Single extends ForEachItem {
    private long myItem = -1;

    @Override
    protected boolean visitItem(long item) {
      assert item != -1;
      myItem = item;
      return false;
    }

    public long getItem() {
      return myItem;
    }

    public boolean hasItem() {
      return myItem >= 0;
    }
  }

  class Collector implements ItemVisitor {
    private final LongCollector myCollector;

    public Collector(LongCollector collector) {
      myCollector = collector;
    }

    public boolean visitItems(long[] items, int offset, int length) throws SQLiteException {
      if (length > 0) {
        myCollector.addAll(new LongNativeArrayIterator(items, offset, offset + length));
      }
      return true;
    }

    @Override
    public boolean visitItems(LongIterable items) throws SQLiteException {
      myCollector.addAll(items.iterator());
      return true;
    }
  }
}
