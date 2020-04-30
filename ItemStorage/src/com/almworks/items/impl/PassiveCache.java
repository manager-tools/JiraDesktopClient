package com.almworks.items.impl;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBEvent;
import com.almworks.items.api.DP;
import com.almworks.items.impl.sqlite.DatabaseContext;
import com.almworks.items.impl.sqlite.ExtractionProcessor;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.items.impl.sqlite.filter.ChangedItemsExtractionFunction;
import com.almworks.items.util.DatabaseUtil;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteLongArray;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.arrays.NullableArrayStorageAccessor;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class PassiveCache implements WriteHook {
  private final BoolExpr<DP> myExpr;
  private final Set<DBAttribute> myAttributesAffectingExpr;
  private final DBAttribute[] myAttributes;
  private final AttributeAdapter[] myAdapters;
  private final Object[] myValues;

  private LongArray myItemsSorted;
  private long myIcn = -1;

  private final LongSetBuilder mySuspiciousItems = new LongSetBuilder();
  private long mySuspiciousNotificationIcn = -1;


  public PassiveCache(DatabaseContext context, BoolExpr<DP> expr, DBAttribute... attributes) {
    this(context, expr, Arrays.asList(attributes));
  }

  public PassiveCache(DatabaseContext context, BoolExpr<DP> expr, Collection<DBAttribute> attributes) {
    myExpr = expr;
    myAttributesAffectingExpr = DatabaseUtil.collectAffectingAttributes(expr);
    myAttributes = (attributes == null || attributes.isEmpty()) ? DBAttribute.EMPTY_ARRAY :
      attributes.toArray(new DBAttribute[attributes.size()]);
    myAdapters = new AttributeAdapter[myAttributes.length];
    myValues = new Object[myAttributes.length];
    for (int i = 0, length = myAttributes.length; i < length; i++) {
      myAdapters[i] = context.getAttributeAdapter(myAttributes[i]);
    }
  }

  @Override
  public <T> void onSetValue(TransactionContext context, long item, DBAttribute<T> attribute, T value)
    throws Exception
  {
    long icn = context.getIcn();
    if (mySuspiciousNotificationIcn != icn) {
      flushSuspicious(icn);
    }
    if (myAttributesAffectingExpr == null || myAttributesAffectingExpr.contains(attribute)) {
      mySuspiciousItems.add(item);
    }
  }

  private void flushSuspicious(long icn) {
    mySuspiciousItems.clear(true);
    mySuspiciousNotificationIcn = icn;
  }

  DBEvent validate(TransactionContext context) throws SQLiteException {
    long icn = context.getIcn();
    DBEvent event;
    if (myIcn < 0) {
      event = initialLoad(context);
    } else {
      event = update(context, myIcn);
    }
    flushSuspicious(icn);
    myIcn = icn;
    return event;
  }

  protected Object getValidated(long item, int attributeIndex) {
    // assert validate() has been called
    LongArray items = myItemsSorted;
    if (items == null)
      return null;
    int idx = items.binarySearch(item);
    if (idx < 0) {
      Log.warn(this + " could not find " + item);
      return null;
    }
    return myAdapters[attributeIndex].arrayGet(myValues[attributeIndex], idx);
  }

  private DBEvent update(TransactionContext context, long fromIcn)
    throws SQLiteException
  {
    LongArray items = myItemsSorted;
    if (items == null) {
      assert false : this;
      return DBEvent.EMPTY;
    }

    DBEvent event = createUpdateEvent(context, items, fromIcn);
    if (event.isEmpty())
      return event;
    
    beforeUpdate(event, context);
    processRemoved(event, items);
    processAdded(event, items);
    processValues(context, event, items);
    afterUpdate(event, context);
    return event;
  }

  private DBEvent createUpdateEvent(TransactionContext context, LongList items, long fromIcn) throws SQLiteException
  {
    long icn = context.getIcn();
    if (fromIcn > icn)
      return DBEvent.EMPTY;
    DBEvent event = null;
    if (fromIcn == icn) {
      event = updateChangedItems(context, fromIcn, items);
    }
    if (event == null) {
      event = updateFromChangedItemsTable(context, fromIcn, items);
    }
    if (event == null) {
      assert false : this;
      event = DBEvent.EMPTY;
    }
    return event;
  }

  private DBEvent updateFromChangedItemsTable(TransactionContext context, long fromIcn, LongList items) throws SQLiteException
  {
    LongList changedItemsSorted = context.getChangedItemsSorted(fromIcn);
    if (changedItemsSorted.isEmpty())
      return DBEvent.EMPTY;
    ExtractionProcessor ep = context.search(myExpr);
    LongSetBuilder filtered = new LongSetBuilder();
    ep.loadItems(context, filtered, new ChangedItemsExtractionFunction(fromIcn));
    return DBEvent.create(items, changedItemsSorted, filtered.commitToArray());
  }

  @Nullable
  private DBEvent updateChangedItems(TransactionContext context, long fromIcn, LongList items)
    throws SQLiteException
  {
    long icn = context.getIcn();
    assert fromIcn == icn : fromIcn + " " + context.getIcn();
    if (!context.hasItemChanges())
      return DBEvent.EMPTY;
    if (mySuspiciousNotificationIcn != icn) {
      assert false : this + " " + mySuspiciousNotificationIcn + " " + icn;
      // failover
      return null;
    }
    if (mySuspiciousItems.isEmpty())
      return DBEvent.EMPTY;
    LongList changedItemsSorted = mySuspiciousItems.toList();
    ExtractionProcessor ep = context.search(myExpr);
    LongSetBuilder filtered = new LongSetBuilder();
    ep.filterItems(context, changedItemsSorted, filtered);
    return DBEvent.create(items, changedItemsSorted, filtered.commitToArray());
  }

  protected void beforeUpdate(DBEvent event, TransactionContext context) {
  }

  protected void afterUpdate(DBEvent event, TransactionContext context) throws SQLiteException {
  }

  private void processValues(TransactionContext context, DBEvent event, LongArray itemsSorted) throws SQLiteException {
    LongList changed = event.getAddedAndChangedSorted();
    if (changed.isEmpty())
      return;
    SQLiteLongArray array = context.useArray(changed);
    try {
      for (int j = 0; j < myAdapters.length; j++) {
        AttributeAdapter adapter = myAdapters[j];
        Object storage = adapter.arrayLoad(changed, array, context);
        myValues[j] = mergeValues(adapter.getArrayAccessor(), myValues[j], itemsSorted, storage, changed);
      }
    } finally {
      array.dispose();
    }
  }

  private Object mergeValues(NullableArrayStorageAccessor accessor, Object storage, LongArray itemsSorted,
    Object fromStorage, LongList fromItemsSorted)
  {
    int lastIndex = 0;
    int len = itemsSorted.size();
    for (int i = 0; i < fromItemsSorted.size(); i++) {
      long item = fromItemsSorted.get(i);
      int j = itemsSorted.binarySearch(item, lastIndex, len);
      if (j < 0) {
        Log.warn(this + " cannot update " + item + ": not found?");
        continue;
      }
      storage = accessor.setObjectValue(storage, j, accessor.getObjectValue(fromStorage, i));
    }
    return storage;
  }

  private void processAdded(DBEvent event, LongArray itemsSorted) {
    LongList addedSorted = event.getAddedSorted();
    if (addedSorted.isEmpty())
      return;
    int idx = itemsSorted.size();
    for (int i = addedSorted.size() - 1; i >= 0; i--) {
      long item = addedSorted.get(i);
      int index = itemsSorted.binarySearch(item, 0, idx);
      if (index >= 0) {
        assert false : "inconsistent " + event;
        continue;
      }
      index = -index - 1;
      itemsSorted.insert(index, item);
      idx = index;
      for (int j = 0; j < myAdapters.length; j++) {
        AttributeAdapter adapter = myAdapters[j];
        myValues[j] = adapter.getArrayAccessor().shiftRight(myValues[j], index, 1);
      }
    }
  }

  private void processRemoved(DBEvent event, LongArray itemsSorted) {
    LongList removed = event.getRemovedSorted();
    if (removed.isEmpty())
      return;
    int idx = itemsSorted.size();
    for (int i = removed.size() - 1; i >= 0; i--) {
      long item = removed.get(i);
      int index = itemsSorted.binarySearch(item, 0, idx);
      if (index < 0) {
        Log.warn(this + ": processRemoved(" + item + "," + index + ")");
        continue;
      }
      itemsSorted.removeAt(index);
      for (int j = 0; j < myAdapters.length; j++) {
        AttributeAdapter adapter = myAdapters[j];
        myValues[j] = adapter.getArrayAccessor().shiftLeft(myValues[j], index, 1);
      }
      idx = index;
    }
  }

  private DBEvent initialLoad(TransactionContext context) throws SQLiteException {
    ExtractionProcessor ep = context.search(myExpr);
    LongSetBuilder builder = new LongSetBuilder();
    ep.loadItems(context, builder);
    LongArray items = builder.commitToArray();
    myItemsSorted = items;
    if (items.isEmpty())
      return DBEvent.EMPTY;
    DBEvent event = DBEvent.create(LongList.EMPTY, items, items);
    beforeUpdate(event, context);
    SQLiteLongArray array = context.useArray(items);
    for (int i = 0; i < myAdapters.length; i++) {
      AttributeAdapter adapter = myAdapters[i];
      myValues[i] = adapter.arrayLoad(items, array, context);
    }
    array.dispose();
    afterUpdate(event, context);
    return event;
  }
}
