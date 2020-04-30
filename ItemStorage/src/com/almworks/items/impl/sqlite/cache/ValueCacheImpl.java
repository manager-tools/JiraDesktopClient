package com.almworks.items.impl.sqlite.cache;

import com.almworks.integers.*;
import com.almworks.items.impl.dbadapter.*;
import com.almworks.util.Pair;
import com.almworks.util.collections.arrays.ArrayStorageAccessor;
import com.almworks.util.collections.arrays.NullableArrayStorageAccessor;
import com.almworks.util.collections.arrays.PrimitiveIntArrayAccessor;
import com.almworks.util.collections.arrays.PrimitiveLongArrayAccessor;
import com.almworks.util.commons.LongObjFunction;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;
import util.external.BitSet2;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

class ValueCacheImpl implements ValueCache {
  private final ValueCacheManager myManager;
  private final Procedure<LongList> myCallback;
  private final List<Attribute> myAttributes = Collections15.arrayList();
  private final LongParallelList myItemsAndReindex = new LongParallelList(new LongArray(), 2);
  private final LongList myItems = myItemsAndReindex.createListAccessor(0);
  private final List<Object> myValues = Collections15.arrayList();
  private final List<LongArray> myOutdated = Collections15.arrayList();
  private final IntArray myEmptyRows = new IntArray();

  public ValueCacheImpl(ValueCacheManager manager, Procedure<LongList> callback) {
    myCallback = callback;
    myManager = manager;
  }

  public void addItems(LongIterable items) {
    LongIterator it = items.iterator();
    if (!it.hasNext()) return;
    synchronized (myManager.getLock()) {
      ValueSearch search = new ValueSearch(myManager);
      long[] inserter = new long[2];
      while (it.hasNext()) {
        long item = it.nextValue();
        addItem(item, search, inserter);
      }
      assert checkItems();
    }
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myOutdated.size(); i++) {
      WritableLongList outdated = myOutdated.get(i);
      if (!outdated.isEmpty()) {
        myManager.requestLoad();
        break;
      }
    }
  }

  void addItem(long item, ValueSearch search, long[] inserter) {
    assert Thread.holdsLock(myManager.getLock());
    if (search == null || inserter == null) {
      assert false;
      search = new ValueSearch(myManager);
      inserter = new long[2];
    }
    int index = myItems.binarySearch(item);
    if (index >= 0)
      return;
    index = -index - 1;
    int dataRow = myEmptyRows.isEmpty() ? myItems.size() : myEmptyRows.removeLast();
    inserter[0] = item;
    inserter[1] = dataRow;
    myItemsAndReindex.insert(index, inserter);
    search.reset(item, this);
    for (int i = 0; i < myAttributes.size(); i++) {
      SyncValueLoader attr = myAttributes.get(i);
      search.setAttribute(attr);
      if (!search.isValueFound()) {
        myOutdated.get(i).addSorted(item);
        continue;
      }
      myValues.set(i, search.copyValue(myValues.get(i), dataRow));
      if (search.isOutOfDate())
        myOutdated.get(i).addSorted(item);
    }
  }

  private boolean checkItems() {
    assert Thread.holdsLock(myManager.getLock());
    LongParallelList.Iterator it = myItemsAndReindex.iterator(0);
    long[] itemDataRow = new long[2];
    long prevItem = -1;
    LongArray dataRows = new LongArray();
    while (it.hasNext()) {
      it.next(itemDataRow);
      long item = itemDataRow[0];
      assert item > prevItem;
      prevItem = item;
      int dataRow = (int) itemDataRow[1];
      assert dataRows.binarySearch(dataRow) < 0;
      dataRows.addSorted(dataRow);
    }
    return true;
  }

  public void removeItems(LongIterable items) {
    LongIterator it = items.iterator();
    if (!it.hasNext()) return;
    synchronized (myManager.getLock()) {
      while (it.hasNext()) {
        long item = it.nextValue();
        int row = myItems.binarySearch(item);
        if (row < 0)
          continue;
        int dataRow = (int)myItemsAndReindex.get(row, 1);
        for (int i = 0; i < myAttributes.size(); i++)
          myOutdated.get(i).removeSorted(item);
        myEmptyRows.add(dataRow);
        myItemsAndReindex.removeAt(row);
      }
      assert checkItems();
    }
  }

  public void setItems(LongIterable items) {
    synchronized (myManager.getLock()) {
      Pair<LongArray, LongList> pair = selectAddRemove(items);
      LongArray addItems = pair.getFirst();
      LongList toRemove = pair.getSecond();
      removeItems(toRemove);
      addItems(addItems);
    }
  }

  Pair<LongArray, LongList> selectAddRemove(LongIterable items) {
    assert Thread.holdsLock(myManager.getLock());
    BitSet2 holdItems = new BitSet2(myItems.size());
    LongArray addItems = new LongArray();
    for (LongIterator it = items.iterator(); it.hasNext();) {
      long item = it.nextValue();
      int row = myItems.binarySearch(item);
      if (row < 0) {
        addItems.add(item);
        continue;
      }
      holdItems.set(row);
    }
    addItems.sortUnique();
    LongArray toRemove = new LongArray(myItems.size() - holdItems.cardinality());
    int index = holdItems.prevClearBit(myItems.size() - 1);
    while (index >= 0 && index < myItems.size()) {
      toRemove.add(myItems.get(index));
      index = holdItems.prevClearBit(index - 1);
    }
    return Pair.create(addItems, (LongList) toRemove);
  }

  public void addAttributes(List<? extends Attribute> attributes) {
    synchronized (myManager.getLock()) {
      assert myAttributes.size() == myValues.size();
      assert myAttributes.size() == myOutdated.size();
      int firstAttr = myAttributes.size();
      attributes = allocForNewAttributes(attributes);
      if (attributes.isEmpty())
        return;
      ValueSearch search = new ValueSearch(myManager);
      long[] itemReindex = new long[2];
      for (LongParallelList.Iterator it = myItemsAndReindex.iterator(0); it.hasNext();) {
        it.next(itemReindex);
        long item = itemReindex[0];
        int dataRow = (int) itemReindex[1];
        search.reset(item, this);
        for (int i = 0; i < attributes.size(); i++) {
          SyncValueLoader attr = attributes.get(i);
          search.setAttribute(attr);
          int attrIndex = firstAttr + i;
          boolean outOfDate = true;
          if (search.isValueFound()) {
            myValues.set(attrIndex, search.copyValue(myValues.get(attrIndex), dataRow));
            outOfDate = search.isOutOfDate();
          }
          if (outOfDate)
            myOutdated.get(attrIndex).addSorted(item);
        }
      }
      myManager.attributesAdded(this, attributes);
    }
    myManager.requestLoad();
  }

  private List<? extends Attribute> allocForNewAttributes(List<? extends Attribute> attributes) {
    int i = 0;
    boolean copied = false;
    while (i < attributes.size()) {
      Attribute attribute = attributes.get(i);
      if (myManager.hasAttribute(this, attribute)) {
        assert myAttributes.indexOf(attribute) >= 0 : attribute;
        if (!copied) {
          attributes = Collections15.arrayList(attributes);
          copied = true;
        }
        attributes.remove(i);
        continue;
      }
      myAttributes.add(attribute);
      myOutdated.add(new LongArray());
      myValues.add(null);
      i++;
    }
    return attributes;
  }

  public void removeAttribute(SyncValueLoader attribute) {
    synchronized (myManager.getLock()) {
      assert myAttributes.size() == myValues.size();
      assert myAttributes.size() == myOutdated.size();
      int column = myAttributes.indexOf(attribute);
      if (column < 0)
        return;
      myAttributes.remove(column);
      myValues.remove(column);
      myOutdated.remove(column);
      myManager.attributeRemoved(this, attribute);
    }
  }

  @Nullable
  public Object getObjectValue(int item, SyncValueLoader attr, @Nullable int[] uptodate) {
    synchronized (myManager.getLock()) {
      int column = myAttributes.indexOf(attr);
      int row = myItems.binarySearch(item);
      int state;
      Object result;
      if (column < 0 || row < 0) {
        state = NO_VALUE;
        result = null;
      } else {
        state = isOutDated(item, column) ? OUT_OF_DATE : UP_TO_DATE;
        int dataRow = (int) myItemsAndReindex.get(row, 1);
        result = attr.getArrayAccessor().getObjectValue(myValues.get(column), dataRow);
      }
      if (uptodate != null)
        uptodate[0] = state;
      return result;
    }
  }

  @Nullable
  public ItemAccessor getItemAccessor(final long item) {
    synchronized (myManager.getLock()) {
      int row = myItems.binarySearch(item);
      if (row < 0)
        return null;
      MyItemAccessor accessor = new MyItemAccessor(item);
      accessor.myRow = row;
      accessor.myDataRow = (int) myItemsAndReindex.get(row, 1);
      return accessor;
    }
  }

  public ItemSetAccessor getItemSetAccessor(LongList items) {
    return items == null || items.isEmpty() ? ItemSetAccessor.EMPTY : new MyItemSetAccessor(items);
  }

  private boolean isOutDated(long item, int column) {
    assert Thread.holdsLock(myManager.getLock());
    return myOutdated.get(column).binarySearch(item) >= 0;
  }

  private Object copyCell(int row, int column, Object storage, int index) {
    assert Thread.holdsLock(myManager.getLock());
    ArrayStorageAccessor accessor = myAttributes.get(column).getArrayAccessor();
    int dataRow = (int) myItemsAndReindex.get(row, 1);
    return accessor.copyValue(myValues.get(column), dataRow, storage, index);
  }

  LongList updateValues(LongList requestedItems, LongList loadedItems, SyncValueLoader attribute, Object storage)
  {
    LongArray updated = updateValueTable(requestedItems, loadedItems, attribute, storage);
    Procedure<LongList> callback = myCallback;
    if (callback == null) {
      updated = null;
    }
    if (updated != null) {
      updated.sort();
    }
    return updated;
  }

  void invokeCallback(LongList updated) {
    Procedure<LongList> callback = myCallback;
    if (callback != null)
        callback.invoke(updated);
    else
      assert false;
  }

  @Nullable
  private LongArray updateValueTable(LongList requestedItems, LongList loadedItems, SyncValueLoader attribute, Object storage)
  {
    assert Thread.holdsLock(myManager.getLock());
    assert requestedItems.isSortedUnique();
    assert loadedItems.isSortedUnique();
    if (myItems.isEmpty())
      return null;
    NullableArrayStorageAccessor accessor = attribute.getArrayAccessor();
    int column = myAttributes.indexOf(attribute);
    if (column < 0)
      return null;
    int loadedIndex = 0;
    int ownIndex = 0;
    long ownItem = myItems.get(0);
    LongArray result = null;
    for (LongIterator it = requestedItems.iterator(); it.hasNext();) {
      long item = it.nextValue();
      if (item > ownItem) {
        ownIndex = myItems.binarySearch(item, ownIndex + 1, myItems.size());
        if (ownIndex < 0) {
          ownIndex = -ownIndex - 1;
          if (ownIndex == myItems.size())
            return result;
          ownItem = myItems.get(ownIndex);
        } else
          ownItem = item;
      }
      if (item == ownItem) {
        int dataRow = (int) myItemsAndReindex.get(ownIndex, 1);
        if (loadedItems.size() > loadedIndex && item == loadedItems.get(loadedIndex))
          myValues.set(column, accessor.copyValue(storage, loadedIndex, myValues.get(column), dataRow));
        else
          myValues.set(column, accessor.setNull(myValues.get(column), dataRow));
        myOutdated.get(column).removeSorted(item);
        if (result == null)
          result = new LongArray();
        result.add(item);
        ownIndex++;
        if (ownIndex >= myItems.size())
          return result;
        ownItem = myItems.get(ownIndex);
      } else
        assert item <= ownItem;
      if (loadedIndex != loadedItems.size() && item >= loadedItems.get(loadedIndex)) {
        assert item == loadedItems.get(loadedIndex);
        loadedIndex++;
      }
    }
    return result;
  }

  @Nullable
  SyncValueLoader chooseAttribute() {
    assert Thread.holdsLock(myManager.getLock());
    int maxOutdated = 0;
    SyncValueLoader attr = null;
    for (int i = 0; i < myAttributes.size(); i++) {
      int outdated = myOutdated.get(i).size();
      if (outdated > maxOutdated) {
        maxOutdated = outdated;
        attr = myAttributes.get(i);
      }
    }
    return attr;
  }

  boolean addItemsToUpdate(SyncValueLoader attribute, LongCollector items) {
    assert Thread.holdsLock(myManager.getLock());
    int column = myAttributes.indexOf(attribute);
    if (column < 0)
      return false;
    LongArray outdated = myOutdated.get(column);
    if (outdated.isEmpty())
      return false;
    items.addAll(outdated);
    return true;
  }

  void markOutofdate(LongList items) {
    assert Thread.holdsLock(myManager.getLock());
    if (myItems.isEmpty())
      return;
    int ownIndex = 0;
    long ownItem = myItems.get(ownIndex);
    for (LongListIterator it = items.iterator(); it.hasNext();) {
      long item = it.nextValue();
      if (item > ownItem) {
        ownIndex = myItems.binarySearch(item, ownIndex + 1, myItems.size());
        if (ownIndex < 0) {
          ownIndex = -ownIndex - 1;
          if (ownIndex == myItems.size())
            return;
          ownItem = myItems.get(ownIndex);
        } else
          ownItem = item;
      }
      if (item == ownItem) {
        // todo mark outdated only updated attributes
        for (LongArray outdated : myOutdated)
          outdated.addSorted(item);
      }
    }
  }

  void markAllOutofdate() {
    assert Thread.holdsLock(myManager.getLock());
    if (myItems.isEmpty() || myAttributes.isEmpty())
      return;
    for (LongArray outdated : myOutdated) {
      outdated.clear();
      outdated.addAll(myItems);
      outdated.sortUnique();
    }
  }

  int getItemCount() {
    assert Thread.holdsLock(myManager.getLock());
    return myItems.size();
  }

  long getItemAt(int itemRow) {
    assert Thread.holdsLock(myManager.getLock());
    return myItems.get(itemRow);
  }

  static class ValueSearch {
    private final ValueCacheManager myManager;
    private long myItem = -1;
    private ValueCacheImpl myIgnoreCache;
    private final List<ValueCacheImpl> myFoundCaches = Collections15.arrayList();
    private final LongArray myItemRow = new LongArray();
    private SyncValueLoader myAttribute = null;
    private int myValueHolder = -1;
    private int myColumn = -1;

    ValueSearch(ValueCacheManager manager) {
      myManager = manager;
    }

    public void reset(long item, @Nullable ValueCacheImpl ignoreCache) {
      if (myItem == item && myIgnoreCache == ignoreCache)
        return;
      myItem = item;
      myIgnoreCache = ignoreCache;
      myFoundCaches.clear();
      myItemRow.clear();
      myAttribute = null;
      myValueHolder = -1;
      myColumn = -1;
    }

    public void setAttribute(SyncValueLoader attribute) {
      if (myAttribute == attribute)
        return;
      myAttribute = attribute;
      myValueHolder = -1;
      Collection<ValueCacheImpl> caches = myManager.getAttributeHolders().get(attribute);
      if (caches == null)
        return;
      for (int i = 0; i < myFoundCaches.size(); i++) {
        ValueCacheImpl cache = myFoundCaches.get(i);
        if (caches.contains(cache)) {
          cacheFound(i);
          return;
        }
      }
      for (ValueCacheImpl cache : caches) {
        if (cache == myIgnoreCache || myFoundCaches.contains(cache))
          continue;
        int row = cache.myItems.binarySearch(myItem);
        if (row >= 0) {
          myFoundCaches.add(cache);
          myItemRow.add(row);
          cacheFound(myFoundCaches.size() - 1);
          return;
        }
      }
    }

    public boolean isValueFound() {
      return myValueHolder >= 0;
    }

    public Object copyValue(Object storage, int index) {
      assert isValueFound();
      ValueCacheImpl cache = myFoundCaches.get(myValueHolder);
      return cache.copyCell((int) myItemRow.get(myValueHolder), myColumn, storage, index);
    }

    private void cacheFound(int index) {
      myValueHolder = index;
      myColumn = myFoundCaches.get(index).myAttributes.indexOf(myAttribute);
      assert myColumn >= 0;
    }

    public boolean isOutOfDate() {
      assert isValueFound();
      ValueCacheImpl cache = myFoundCaches.get(myValueHolder);
      return cache.isOutDated(myItem, myColumn);
    }
  }


  private class MyItemAccessor implements ItemAccessor {
    private int myRow = 0;
    private final long myItem;
    private int myDataRow = -1;

    public MyItemAccessor(long item) {
      myItem = item;
    }

    @Nullable
      public Object getValue(SyncValueLoader attr) {
      if (myRow < 0)
        return null;
      synchronized (myManager.getLock()) {
        if (!findRow())
          return null;
        int index = myAttributes.indexOf(attr);
        if (index < 0)
          return null;
        return attr.getArrayAccessor().getObjectValue(myValues.get(index), myDataRow);
      }
    }

    public int getInt(SyncValueLoader attr, int missingValue) {
      if (myRow < 0)
        return missingValue;
      synchronized (myManager.getLock()) {
        if (!findRow())
          return missingValue;
        int index = myAttributes.indexOf(attr);
        if (index < 0)
          return missingValue;
        ArrayStorageAccessor accessor = attr.getArrayAccessor();
        if (!(accessor instanceof PrimitiveIntArrayAccessor)) {
          assert false : accessor + " " + attr;
          return missingValue;
        }
        return ((PrimitiveIntArrayAccessor) accessor).getIntValue(myValues.get(index), myDataRow);
      }
    }

    public long getItem() {
      return myItem;
    }

    public long getLong(SyncValueLoader attr, long missingValue) {
      if (myRow < 0)
        return missingValue;
      synchronized (myManager.getLock()) {
        if (!findRow())
          return missingValue;
        int index = myAttributes.indexOf(attr);
        if (index < 0)
          return missingValue;
        ArrayStorageAccessor accessor = attr.getArrayAccessor();
        if (!(accessor instanceof PrimitiveLongArrayAccessor)) {
          assert false : accessor + " " + attr;
          return missingValue;
        }
        return ((PrimitiveLongArrayAccessor) accessor).getLongValue(myValues.get(index), myDataRow);
      }
    }

    public boolean hasValues() {
      return myRow >= 0;
    }

    public boolean hasUptodateValue(SyncValueLoader attribute) {
      if (myRow < 0)
        return false;
      synchronized (myManager.getLock()) {
        int idx = myAttributes.indexOf(attribute);
        return idx >= 0 && !isOutDated(myItem, idx);
      }
    }

    private boolean findRow() {
      assert Thread.holdsLock(myManager.getLock());
      if (myRow < 0)
        return false;
      else if (myRow < myItems.size() && myItems.get(myRow) == myItem && myItemsAndReindex.get(myRow, 1) == myDataRow)
        return true;
      myRow = myItems.binarySearch(myItem);
      if (myRow < 0) {
        myRow = -1;
        return false;
      }
      myDataRow = (int) myItemsAndReindex.get(myRow, 1);
      return true;
    }
  }


  private class MyItemSetAccessor implements ItemSetAccessor, LongObjFunction<ItemAccessor> {
    private final LongList myItemSet;

    public MyItemSetAccessor(LongList itemSet) {
      myItemSet = itemSet;
    }

    public <T> T visit(Visitor<T> visitor) {
      boolean[] wantMore = {true};
      T result = null;
      for (LongIterator ii = myItemSet.iterator(); ii.hasNext();) {
        long item = ii.nextValue();
        result = visitor.visit(ValueCacheImpl.this.getItemAccessor(item), wantMore);
        if (!wantMore[0])
          break;
      }
      //noinspection ConstantConditions
      return result;
    }

    public LongIterator getItems() {
      return myItemSet.iterator();
    }

    public int getCount() {
      return myItemSet.size();
    }

    @Nullable
    public ItemAccessor getFirst() {
      if (myItemSet.isEmpty()) return null;
      return invoke(myItemSet.get(0));
    }

    public boolean isEmpty() {
      return myItemSet.isEmpty();
    }

    public Iterator<ItemAccessor> iterator() {
      return new ItemCollectionIterator(myItemSet.iterator(), this);
    }

    public ItemAccessor invoke(long item) {
      return ValueCacheImpl.this.getItemAccessor(item);
    }
  }
}
