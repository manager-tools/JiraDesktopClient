package com.almworks.items.sync.edit;

import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.sync.*;
import com.almworks.items.sync.impl.*;
import com.almworks.items.sync.util.ItemDiffImpl;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import com.almworks.util.exec.ThreadGate;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectIterator;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

abstract class BaseEditDrain extends BaseDBDrain implements EditDrain {
  private final EditCommit myCommit;
  @Nullable
  private final EditorLock myLock;
  private final LongSet myJustCreated = new LongSet();
  private final LongSet myJustCreatedLocal = new LongSet();
  private final Map<EditCounterpart, TLongObjectHashMap<AttributeMap>> myChanges = Collections15.hashMap();
  private final LongSet myKnownUnsafe = new LongSet();
  private Map<EditCounterpart, TLongObjectHashMap<ItemValues>> myNewValues = null;
  private TLongObjectHashMap<AttributeMap> myBases;

  public BaseEditDrain(SyncManagerImpl manager, @Nullable EditorLock lock, EditCommit commit) {
    super(manager, Branch.TRUNK);
    myLock = lock;
    myCommit = commit;
  }

  @Override
  protected void performTransaction() throws DBOperationCancelledException {
    myBases = getBases();
    if (myBases == null) throw new DBOperationCancelledException();
    try {
      myCommit.performCommit(this);
    } finally {
      myBases = null;
    }
    collectChanges();
  }

  private void collectChanges() {
    assert myNewValues == null;
    List<DBAttribute<?>> changedAttrs = Collections15.arrayList();
    Map<EditCounterpart, TLongObjectHashMap<ItemValues>> newValuesMap = Collections15.hashMap();
    for (Map.Entry<EditCounterpart, TLongObjectHashMap<AttributeMap>> entry : myChanges.entrySet()) {
      TLongObjectHashMap<AttributeMap> changedItems = entry.getValue();
      TLongObjectHashMap<ItemValues> newValues = new TLongObjectHashMap<>();
      for (TLongObjectIterator<AttributeMap> it = changedItems.iterator(); it.hasNext();) {
        it.advance();
        AttributeMap original = it.value();
        long item = it.key();
        AttributeMap values = getTrunkValues(item);
        ItemValues itemDiff;
        if (values == null) itemDiff = null;
        else {
          changedAttrs.clear();
          ItemDiffImpl.collectChanges(getReader(), original, values, changedAttrs);
          if (changedAttrs.isEmpty()) itemDiff = null;
          else itemDiff = ItemValues.collect(changedAttrs, values);
        }
        if (itemDiff != null) newValues.put(item, itemDiff);
      }
      newValuesMap.put(entry.getKey(), newValues);
    }
    myChanges.clear();
    myNewValues = newValuesMap;
  }

  private AttributeMap getTrunkValues(long item) {
    VersionHolder trunk = HolderCache.instance(getWriter()).getHolder(item, null, false);
    if (trunk == null) return null;
    return trunk.getAllShadowableMap();
  }

  @Nullable
  protected abstract TLongObjectHashMap<AttributeMap> getBases();

  @Override
  public ItemVersionCreator createItem() {
    ItemVersionCreator item = super.createItem();
    myJustCreated.add(item.getItem());
    return item;
  }

  @Override
  public ItemVersionCreator createLocalItem() {
    ItemVersionCreator creator = createItem();
    long item = creator.getItem();
    myJustCreatedLocal.add(item);
    HolderCache holders = HolderCache.instance(getWriter());
    holders.setBase(item, SyncSchema.getInvisible());
    return creator;
  }

  @Override
  public void beforeShadowableChanged(long item, boolean isNew) {
    super.beforeShadowableChanged(item, isNew);
    HolderCache holders = HolderCache.instance(getWriter());
    AttributeMap base;
    if (myJustCreated.contains(item)) {
      assert isNew : item;
      myJustCreated.remove(item);
      if (myJustCreatedLocal.remove(item)) return;
      assert holders.getBase(item) == null;
      base = SyncSchema.getInvisible();
    } else {
      base = myBases.get(item);
      AttributeMap dbBase = holders.getBase(item);
      if (base != null && dbBase != null) return;
      if (base == null) {
        Boolean existing = getWriter().getValue(item, SyncAttributes.EXISTING);
        if (existing == null || !existing) base = SyncSchema.getInvisible(); // the item just revived
        else {
          LogHelper.assertError(myKnownUnsafe.contains(item));
          base = getBaseFromCurrentEditor(item, base);
          if (dbBase != null) return;
          if (base == null) base = loadBase(getReader(), item);
        }
      }
      assert !isNew : item;
    }
    if (base == null) LogHelper.error("Missing base while changing item", item);
    else holders.setBase(item, base);
  }

  private AttributeMap getBaseFromCurrentEditor(long item, AttributeMap base) {
    EditCounterpart prev = null;
    EditCounterpart counterpart;
    while (true) {
      counterpart = getManager().findLock(item);
      if (counterpart == null || !counterpart.isAlive()) break;
      if (counterpart == prev) {
        Log.error("Cannot get base from " + counterpart + " (" + item + ")");
        break;
      }
      prev = counterpart;
      base = counterpart.getItemBase(item);
      if (base != null) break;
    }
    if (counterpart != null && base != null) {
      TLongObjectHashMap<AttributeMap> changes = myChanges.get(counterpart);
      if (changes == null) {
        changes = new TLongObjectHashMap<>();
        myChanges.put(counterpart, changes);
      }
      AttributeMap trunkValues = getTrunkValues(item);
      if (trunkValues != null) changes.put(item, trunkValues);
    }
    return base;
  }

  @Override
  public ItemVersionCreator markMerged(long item) {
    HolderCache holders = HolderCache.instance(getReader());
    AttributeMap conflict = holders.getConflict(item);
    if (conflict != null) {
      holders.setBase(item, conflict);
      holders.setConflict(item, null);
    }
    return changeItem(item);
  }

  @Override
  public boolean discardChanges(long root) {
    DBWriter writer = getWriter();
    LongList subtree = SyncUtils.getSlavesSubtree(writer, root);
    if (!getManager().lockOrMergeLater(writer, subtree, myLock, new LongList.Single(root))) {
      // todo: implement discard in any case
      return false;
    }
    LongSet toClear = new LongSet();
    BranchUtil util = BranchUtil.instance(getReader());
    for (int i = 0; i < subtree.size(); i++) {
      long slave = subtree.get(i);
      ItemVersion serverSlave = util.readServerIfExists(slave);
      if (serverSlave == null) continue;
      if (serverSlave.isInvisible()) toClear.add(slave);
    }
    LongList allToClear = SyncUtils.getSlavesSubtrees(writer, toClear);
    for (int i = 0; i < allToClear.size(); i++) writer.clearItem(allToClear.get(i));
    for (int i = 0; i < subtree.size(); i++) {
      long item = subtree.get(i);
      if (allToClear.contains(item)) continue;
      SyncSchema.discardSingle(writer, item);
    }
    return true;
  }

  public static TLongObjectHashMap<AttributeMap> collectBases(DBReader reader, LongList items) {
    TLongObjectHashMap<AttributeMap> result = new TLongObjectHashMap<AttributeMap>();
    collectBases(reader, items, result);
    return result;
  }

  public static void collectBases(DBReader reader, LongList items, TLongObjectHashMap<AttributeMap> result) {
    HolderCache holders = HolderCache.instance(reader);
    for (int i = 0; i < items.size(); i++) {
      long item = items.get(i);
      if (result.containsKey(item)) continue;
      AttributeMap base = holders.getBase(item);
      if (base == null) base = loadBase(reader, item);
      result.put(item, base);
    }
  }

  protected static AttributeMap loadBase(DBReader reader, long item) {
    return SyncUtils.readTrunk(reader, item).getAllShadowableMap();
  }

  @Override
  public ItemVersionCreator unsafeChange(long item) {
    myKnownUnsafe.add(item);
    return super.changeItem(item);
  }

  @Override
  protected void onTransactionFinished(DBResult<?> result) {
    boolean success = result.isSuccessful();
    onFinish(myCommit, success);
    myCommit.onCommitFinished(success);
    final Map<EditCounterpart, TLongObjectHashMap<ItemValues>> newValues = myNewValues;
    if (newValues == null) return;
    for (Iterator<Map.Entry<EditCounterpart, TLongObjectHashMap<ItemValues>>> it = newValues.entrySet().iterator();
      it.hasNext();)
      if (it.next().getValue().isEmpty()) it.remove();
    if (!newValues.isEmpty())
      ThreadGate.AWT.execute(new Runnable() {
        @Override
        public void run() {
          for (Map.Entry<EditCounterpart, TLongObjectHashMap<ItemValues>> entry : newValues.entrySet()) {
            entry.getKey().notifyConcurrentEdit(entry.getValue());
          }
        }
      });
  }

  protected void onFinish(EditCommit commit, boolean success) {}
}
