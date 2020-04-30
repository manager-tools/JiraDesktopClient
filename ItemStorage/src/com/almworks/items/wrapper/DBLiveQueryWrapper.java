package com.almworks.items.wrapper;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongCollector;
import com.almworks.items.api.DBLiveQuery;
import com.almworks.util.commons.LongObjFunction2;

/**
 * Even if this class does in fact only wrap ItemStorage methods without adding any extra action, it is here to not forget wrap them once they are added.
 * See note for {@link DatabaseWrapper} about compilation breaking.
 * */
class DBLiveQueryWrapper implements DBLiveQuery {
  private final DBLiveQuery myDelegate;

  public DBLiveQueryWrapper(DBLiveQuery delegate) {
    myDelegate = delegate;
  }

  public boolean isInitialized() {
    return myDelegate.isInitialized();
  }

  public LongArray copyItems() {
    return myDelegate.copyItems();
  }

  public <C extends LongCollector> C copyItems(C collector) {
    return myDelegate.copyItems(collector);
  }

  public <T> T fold(T seed, LongObjFunction2<T> f) {
    return myDelegate.fold(seed, f);
  }

  public int count() {
    return myDelegate.count();
  }

  public boolean isTransactionResultVisible(long icn) {
    return myDelegate.isTransactionResultVisible(icn);
  }
}
