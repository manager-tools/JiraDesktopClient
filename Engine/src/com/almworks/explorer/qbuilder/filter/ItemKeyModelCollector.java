package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.*;
import com.almworks.api.explorer.util.ItemKeys;
import com.almworks.integers.IntArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.util.advmodel.*;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.swing.EventDispatchCache;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectProcedure;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.concurrent.Synchronized;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public class ItemKeyModelCollector<T extends ResolvedItem> {
  public static final Convertor<ResolvedItem, ItemKey> DERESOLVER =
    new Convertor<ResolvedItem, ItemKey>() {
      public ItemKey convert(ResolvedItem value) {
        return ItemKeys.unresolvedCopy(value);
      }
    };

  @Nullable
  private ItemKeyCache myKeyCache;
  private final ResolvedFactory<T> myKeyFactory;
  private final BoolExpr<DP> myVariantsExpr;
  private final TLongObjectHashMap<T> myItemKeys = new TLongObjectHashMap<>();
  private final TLongObjectHashMap<T> myAllItemKeys = new TLongObjectHashMap<>();

  private final MultiMap<String, T> myIdsMap = MultiMap.create();
  private final OrderListModel<T> myVariantsModel = OrderListModel.create();
  private final AListModel<ItemKey> myUnresolvedModel;

  private final String myDebugName;
  private final Synchronized<Boolean> myDatabaseScanned = Synchronized.create(false);


  public ItemKeyModelCollector(ResolvedFactory<T> keyFactory, String debugName, BoolExpr<DP> variantsExpr, ItemKeyCache keyCache) {
    myKeyFactory = keyFactory;
    myDebugName = debugName;
    myUnresolvedModel = createUnresolvedUniqueModel(Lifespan.FOREVER, myVariantsModel);
    myVariantsExpr = variantsExpr;
    myKeyCache = keyCache;
  }

  public static <T extends ResolvedItem> ItemKeyModelCollector<T> create(ResolvedFactory<T> factory, DBItemType type, String name, ItemKeyCache keyCache) {
    return new ItemKeyModelCollector<T>(factory, name, DPEqualsIdentified.create(DBAttribute.TYPE, type), keyCache);
  }

  public static <T extends ResolvedItem> ItemKeyModelCollector<T> create(ResolvedFactory<T> factory, BoolExpr<DP> filter, String name, ItemKeyCache keyCache) {
    return new ItemKeyModelCollector<T>(factory, name, filter, keyCache);
  }

  public ResolvedFactory<T> getKeyFactory() {
    return myKeyFactory;
  }

  public void waitForInitialization() throws InterruptedException {
    myDatabaseScanned.waitForValue(true);
  }

  public AListModel<ItemKey> getUnresolvedUniqueModel() {
    return myUnresolvedModel;
  }


  public String toString() {
    return myDebugName;
  }

  public AListModel<T> getModel() {
    return myVariantsModel;
  }

  @Nullable
  public List<T> getAllResolvedListCopyOrNull(String artifactId) {
    synchronized (myIdsMap) {
      return myIdsMap.getWritableCopyOrNull(artifactId);
    }
  }

  public void start(Lifespan life, Database db) {
    start(life, db, null);
  }

  public void start(Lifespan life, Database db, ChangeListener listener) {
    db.liveQuery(life, myVariantsExpr, new MyDBListener(listener));
  }

  public static AListModel<ItemKey> createUnresolvedUniqueModel(
    Lifespan life, AListModel<? extends ResolvedItem> source)
  {
    AListModel<ResolvedItem> sorted = SortedListDecorator.create(life, source, ResolvedItem.comparator());
    AListModel<ItemKey> deresolved = ConvertingListDecorator.create(sorted, DERESOLVER);
    return UniqueListDecorator.create(life, deresolved);
  }

  @ThreadSafe
  public T findForItem(long item) {
    synchronized (myAllItemKeys) {
      return myAllItemKeys.get(item);
    }
  }

  @ThreadSafe
  @NotNull
  public List<T> getAllItemKeys() {
    synchronized (myAllItemKeys) {
      //noinspection unchecked
      return (List<T>)(List<?>) Arrays.asList(myAllItemKeys.getValues());
    }
  }

  public T ensureKnown(DBReader reader, long item) throws BadItemException {
    assert !(reader instanceof DBWriter) : reader;
    synchronized (myAllItemKeys) {
      T key = myAllItemKeys.get(item);
      if (key != null)
        return key;
    }
    ItemKeyCache cache = getKeyCache();
    if (cache == null)
      throw new BadItemException("No key cache", item);
    T key = cache.getItemKey(item, reader, myKeyFactory);
    synchronized (myAllItemKeys) {
      myAllItemKeys.put(item, key);
    }
    return key;
  }

  private ItemKeyCache getKeyCache() {
    if (myKeyCache == null) {
      myKeyCache = Context.require(NameResolver.ROLE).getCache();
    }
    return myKeyCache;
  }

  private class MyDBListener implements DBLiveQuery.Listener {
    private final ChangeListener myListener;

    public MyDBListener(ChangeListener listener) {
      myListener = listener;
    }

    @Override
    public void onICNPassed(long icn) {}

    @Override
    public void onDatabaseChanged(DBEvent event, DBReader reader) {
      KeysUpdate update = new KeysUpdate(event.getRemovedSorted());
      LongList items = event.getAddedAndChangedSorted();
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        if (item <= 0) {
          Log.error("null item changed " + item);
          continue;
        }
        T key;
        try {
          ItemKeyCache keyCache = getKeyCache();
          if (keyCache == null) continue;
          key = keyCache.getItemKey(item, reader, myKeyFactory);
        } catch (BadItemException e) {
          continue;
        }
        update.add(item, key);
        synchronized (myAllItemKeys) {
          myAllItemKeys.put(item, key);
        }
      }
      if (!update.isEmpty() || !myDatabaseScanned.get()) {
        ThreadGate.AWT.execute(update);
        if (myListener != null) {
          myListener.onChange();
        }
      }
    }
  }

  private class KeysUpdate implements Runnable, TLongObjectProcedure<T> {
    private final LongList myRemoved;
    private final TLongObjectHashMap<T> myUpdated = new TLongObjectHashMap<>();

    private final List<T> myAdd = Collections15.arrayList();
    private final IntArray myIndexesToRemove = new IntArray();
    private final List<T> myKeysRemove = Collections15.arrayList();
    private final Map<Integer, T> myReplace = Collections15.hashMap();

    private KeysUpdate(LongList removed) {
      myRemoved = removed;
    }

    public void add(long item, T key) {
      myUpdated.put(item, key);
    }

    @Override
    public void run() {
      Object mark = null;
      try {
        mark = EventDispatchCache.startEventDispatch();
        doUpdate();
      } finally {
        EventDispatchCache.stopEventDispatch(mark);
        myDatabaseScanned.set(true);
      }
    }

    private void doUpdate() {
      for(int i = 0; i < myRemoved.size(); i++) {
        final T key = myItemKeys.remove(myRemoved.get(i));
        if(key != null) {
          myKeysRemove.add(key);
          final int index = myVariantsModel.indexOf(key, ResolvedItem.BY_RESOLVED_ITEM);
          if(index >= 0) {
            myIndexesToRemove.add(index);
          }
        }
      }

      myUpdated.forEachEntry(this);

      synchronized(myIdsMap) {
        for(final T key : myKeysRemove) {
          removeFromIdMap(key.getId(), key);
        }
        for(final T key : myAdd) {
          myIdsMap.add(key.getId(), key);
        }
        for(final T key : myReplace.values()) {
          myIdsMap.add(key.getId(), key);
        }
      }

      for(final Map.Entry<Integer, T> e : myReplace.entrySet()) {
        myVariantsModel.replaceAt(e.getKey(), e.getValue());
      }
      myVariantsModel.removeAll(myIndexesToRemove.toNativeArray());
      myVariantsModel.addAll(myAdd);
    }

    private void removeFromIdMap(String id, T key) {
      List<T> keys = myIdsMap.getAllEditable(id);
      if (keys == null) return;
      for (Iterator<T> i = keys.iterator(); i.hasNext(); ) {
        if (ResolvedItem.BY_RESOLVED_ITEM.areEqual(i.next(), key)) {
          i.remove();
        }
      }
    }

    @Override
    public boolean execute(long item, T key) {
      final T prev = myItemKeys.get(item);
      if(notSame(prev, key)) {
        myItemKeys.put(item, key);
        final int index = prev != null ? myVariantsModel.indexOf(prev, ResolvedItem.BY_RESOLVED_ITEM) : -1;
        if(index >= 0) {
          myReplace.put(index, key);
        } else {
          myAdd.add(key);
        }
        if(prev != null) {
          myKeysRemove.add(prev);
        }
      }
      return true;
    }

    private boolean notSame(T t1, T t2) {
      if((t1 == null) != (t2 == null)) {
        return true;
      }
      if(t1 == null) {
        return false;
      }
      return !t1.isSame(t2);
    }

    public boolean isEmpty() {
      return myRemoved != null && myRemoved.isEmpty() && myUpdated.isEmpty();
    }
  }
}
