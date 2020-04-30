package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongCollector;
import com.almworks.util.commons.LongObjFunction2;

/**
 * Live version of DBQuery. Can be called from any thread. <br>
 * Is updated after write transactions finish successfully; it implies that one should avoid using it inside write transaction and instead use DBWriter to query database. 
 */
public interface DBLiveQuery {
  DBLiveQuery DEAF = new NoData();

  boolean isInitialized();

  LongArray copyItems();

  <C extends LongCollector> C copyItems(C collector);

  <T> T fold(T seed, LongObjFunction2<T> f);

  int count();

  boolean isTransactionResultVisible(long icn);

  interface Listener extends DBListener {
    Listener DEAF = new Deaf();

    void onICNPassed(long icn);

    class Deaf extends DBListener.Deaf implements Listener {
      @Override
      public void onICNPassed(long icn) {
      }
    }
  }

  final class NoData implements DBLiveQuery {
    @Override
    public boolean isInitialized() {
      return false;
    }

    @Override
    public LongArray copyItems() {
      return new LongArray();
    }

    @Override
    public <C extends LongCollector> C copyItems(C collector) {
      return collector;
    }

    @Override
    public int count() {
      return 0;
    }

    @Override
    public boolean isTransactionResultVisible(long icn) {
      return true;
    }

    @Override
    public <T> T fold(T seed, LongObjFunction2<T> f) {
      return seed;
    }

    @Override
    public String toString() {
      return "DBLQ[]";
    }
  }
}
