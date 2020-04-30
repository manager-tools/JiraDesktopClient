package com.almworks.items.impl.sqlite.cache;

import com.almworks.integers.LongCollector;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DBReader;
import com.almworks.items.impl.DBReaderImpl;
import com.almworks.items.impl.dbadapter.Attribute;
import com.almworks.items.impl.dbadapter.CompoundValueCache;
import com.almworks.items.impl.dbadapter.SyncValueLoader;
import com.almworks.items.impl.sqlite.DatabaseJob;
import com.almworks.items.impl.sqlite.QueryProcessor;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.Break;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ValueCacheManager implements QueryProcessor.Client {
  private final List<ValueCacheImpl> myCaches = Collections15.arrayList();
  private final Object myLock = new Object();
  private long myLastIcn = -1;
  private final Map<Attribute, Collection<ValueCacheImpl>> myAttributeHolders = Collections15.hashMap();

  // todo value caches should work in a different session from queries
  // todo just have a tighter integration of value cache and dbconnectionsqlite
  private final QueryProcessor myProcessor;

  public ValueCacheManager(QueryProcessor processor) {
    myProcessor = processor;
  }

  public DatabaseJob createJob() {
    return new DatabaseJob() {
      protected void dbrun(TransactionContext context) throws Exception {
        updateCache(context, this);
      }

      public TransactionType getTransactionType() {
        return TransactionType.READ_ROLLBACK;
      }

      public Object getIdentity() {
        return ValueCacheManager.this;
      }
    };
  }

  public void attach() {
    synchronized (myLock) {
      assert myCaches.size() == 0;
      assert myAttributeHolders.size() == 0;
      if (myLastIcn != -1) {
        assert false;
        return;
      }
    }
    myProcessor.attach(Lifespan.FOREVER, this);
  }

  ValueCacheImpl createNewCache(Procedure<LongList> callback) {
    assert Thread.holdsLock(myLock);
    ValueCacheImpl cache = new ValueCacheImpl(this, callback);
    myCaches.add(cache);
    return cache;
  }

  public CompoundValueCache createCache(Procedure<LongList> callback) {
    return new CompoundCacheImpl(this, callback);
  }

  private void updateCache(TransactionContext context, DatabaseJob jobState) throws SQLiteException {
    long contextIcn = context.getIcn();
    long lastIcn = 0;
    AttributeJob job = null;
    LongSetBuilder items = new LongSetBuilder();
    DBReader access = new DBReaderImpl(context);
    SQLiteException error = null;
    while (true) {
      synchronized (myLock) {
        if (myLastIcn == -1) {
          myLastIcn = contextIcn;
          lastIcn = -1;
        }
        if (lastIcn != -1)
          lastIcn = myLastIcn;
        else
          job = chooseJob(jobState, items);
      }
      if (lastIcn != -1) {
        markOutOfDate(context, lastIcn);
        lastIcn = -1;
        continue;
      }
      if (job == null || jobState.isCancelled() || jobState.isHurried())
        break;
      try {
        assert !items.isEmpty();
        job.perform(access, items.toArray());
        items.clear(true);
      } catch (SQLiteException e) {
        error = e;
        break;
      } catch (Break aBreak) {
        break;
      }
    }
    if (error != null)
      throw error;
  }

  private void markOutOfDate(TransactionContext context, long lastIcn) throws SQLiteException {
    long icn = context.getIcn();
    if (lastIcn >= icn)
      return;
    LongList change = context.getChangedItemsSorted(lastIcn);
    synchronized (myLock) {
      for (ValueCacheImpl cache : myCaches)
        cache.markOutofdate(change);
      myLastIcn = icn;
    }
  }

  @Nullable
  private AttributeJob chooseJob(DatabaseJob jobState, LongCollector items)
  {
    assert Thread.holdsLock(myLock);
    SyncValueLoader attribute = null;
    for (ValueCacheImpl cache : myCaches) {
      attribute = cache.chooseAttribute();
      if (attribute != null)
        break;
    }
    if (attribute == null)
      return null;
    boolean hasItemsToUpdate = false;
    for (ValueCacheImpl cache : myCaches)
      if (cache.addItemsToUpdate(attribute, items))
        hasItemsToUpdate = true;
    if (!hasItemsToUpdate)
      return null;
    return new AttributeJob(this, attribute, jobState);
  }

  Object getLock() {
    return myLock;
  }

  Map<Attribute, Collection<ValueCacheImpl>> getAttributeHolders() {
    assert Thread.holdsLock(myLock);
    return myAttributeHolders;
  }

  void attributesAdded(ValueCacheImpl cache, Collection<? extends Attribute> attributes) {
    assert Thread.holdsLock(myLock);
    for (Attribute attribute : attributes) {
      Collection<ValueCacheImpl> holders = myAttributeHolders.get(attribute);
      if (holders == null) {
        holders = Collections15.hashSet();
        myAttributeHolders.put(attribute, holders);
      }
      assert !holders.contains(cache);
      holders.add(cache);
    }
  }

  void attributeRemoved(ValueCacheImpl cache, SyncValueLoader attribute) {
    assert Thread.holdsLock(myLock);
    Collection<ValueCacheImpl> holders = myAttributeHolders.get(attribute);
    if (holders == null) {
      assert false;
      return;
    }
    assert holders.contains(cache);
    holders.remove(cache);
  }

  void requestLoad() {
    myProcessor.processClient(this);
  }

  public boolean hasAttribute(ValueCacheImpl cache, SyncValueLoader attribute) {
    Collection<ValueCacheImpl> holders = myAttributeHolders.get(attribute);
    return holders != null && holders.contains(cache);
  }

  void removeCache(ValueCacheImpl cache) {
    assert Thread.holdsLock(myLock);
    myCaches.remove(cache);
    for (
      Iterator<Map.Entry<Attribute, Collection<ValueCacheImpl>>> it = myAttributeHolders.entrySet().iterator();
      it.hasNext();)
    {
      Map.Entry<Attribute, Collection<ValueCacheImpl>> entry = it.next();
      Collection<ValueCacheImpl> holders = entry.getValue();
      if (holders == null) {
        it.remove();
        continue;
      }
      holders.remove(cache);
      if (holders.isEmpty())
        it.remove();
    }
  }

  void onValuesLoaded(SyncValueLoader attr, LongList requestedItems, LongList loadedItems, Object data) {
    LongList[] updates;
    ValueCacheImpl[] caches;
    synchronized (myLock) {
      Collection<ValueCacheImpl> cachesSet = myAttributeHolders.get(attr);
      if (cachesSet == null || cachesSet.isEmpty())
        return;
      caches = cachesSet.toArray(new ValueCacheImpl[cachesSet.size()]);
      updates = new LongList[caches.length];
      for (int i = 0; i < caches.length; i++) {
        ValueCacheImpl cache = caches[i];
        updates[i] = cache.updateValues(requestedItems, loadedItems, attr, data);
      }
    }
    for (int i = 0; i < updates.length; i++) {
      LongList update = updates[i];
      if (update != null)
        caches[i].invokeCallback(update);
    }
  }

  private static class AttributeJob implements SyncValueLoader.Sink {
    private static final long TRY_FINISH_TIME = 30;
    private final ValueCacheManager myManager;
    private final SyncValueLoader myAttribute;
    private final DatabaseJob myJobState;
    private long myHurried = -1;

    private AttributeJob(ValueCacheManager manager, SyncValueLoader attribute, DatabaseJob jobState)
    {
      myManager = manager;
      myAttribute = attribute;
      myJobState = jobState;
    }

    public void onLoaded(LongList requestedItems, LongList loadedItems, Object data) throws Break {
      myManager.onValuesLoaded(myAttribute, requestedItems, loadedItems, data);
      if (myJobState.isCancelled())
        throw new Break();
      if (myJobState.isHurried()) {
        if (myHurried == -1)
          myHurried = System.currentTimeMillis();
        else
          if (System.currentTimeMillis() - myHurried > TRY_FINISH_TIME)
            throw new Break();
      }
    }

    public void perform(DBReader access, LongList items) throws SQLiteException, Break {
      if (items.isEmpty()) {
        assert false;
        return;
      }
      myAttribute.load(access, items.iterator(), this);
    }
  }
}
