package com.almworks.itemsync;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemAutoMerge;
import com.almworks.items.sync.util.merge.*;
import com.almworks.items.util.SyncAttributes;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class MergeOperationsManagerImpl implements MergeOperationsManager {
  private final List<OperationsHolder> myHolders = Collections15.arrayList();

  public ItemAutoMerge getOperations(DBReader reader, long item) {
    Long connection = reader.getValue(item, SyncAttributes.CONNECTION);
    if (connection == null || connection <= 0) {
      Log.warn("Cannot automerge item without connection " + item);
      return null;
    }
    Long type = reader.getValue(item, DBAttribute.TYPE);
    Map<DBItemType, ItemAutoMerge> typeToOperation = collectByType();
    if (typeToOperation == null) typeToOperation = Collections.emptyMap();
    for (Map.Entry<DBItemType, ItemAutoMerge> entry : typeToOperation.entrySet()) {
      assert type != null;
      long t = reader.findMaterialized(entry.getKey());
      if (t == type) return entry.getValue();
    }
    Log.warn("AutoMerge operations not found for " + item + " connection " + connection + " type:" + getItemType(reader, item) );
    return null;
  }

  @Nullable
  private static String getItemType(DBReader reader, long item) {
    Long type = reader.getValue(item, DBAttribute.TYPE);
    if (type <= 0) return null;
    return reader.getValue(type, DBAttribute.ID);
  }

  private Map<DBItemType, ItemAutoMerge> collectByType() {
    Map<DBItemType, ItemAutoMerge> byType = Collections15.hashMap();
    synchronized (myHolders) {
      for (OperationsHolder holder : myHolders) {
        for (DBItemType t : holder.myTypes) byType.put(t, holder.myOperations);
      }
    }
    return byType;
  }

  @Override
  public void addMergeOperation(ItemAutoMerge operations, DBItemType... types) {
    if (operations == null) return;
    if (types == null || types.length == 0) {
      Log.error("Missing types " + operations);
      return;
    }
    synchronized (myHolders) {
      Map<DBItemType, ItemAutoMerge> byType = collectByType();
      for (DBItemType type : types) {
        ItemAutoMerge known = byType.get(type);
        if (known != null) Log.error(type + " already registered " + known + " " + operations);
      }
      myHolders.add(new OperationsHolder(operations, types));
    }
  }

  @Override
  public Builder buildOperation(DBItemType type) {
    return new MyBuilder(this, type);
  }

  public void clear() {
    synchronized (myHolders) {
      myHolders.clear();
    }
  }

  private static class OperationsHolder {
    private final ItemAutoMerge myOperations;
    private final DBItemType[] myTypes;

    private OperationsHolder(ItemAutoMerge operations, DBItemType[] types) {
      myOperations = operations;
      myTypes = types != null && types.length > 0 ? ArrayUtil.arrayCopy(types) : DBItemType.EMPTY_ARRAY;
    }
  }


  private static class MyBuilder implements Builder {
    private final MergeOperationsManagerImpl myManager;
    private final DBItemType myType;
    private final List<ItemAutoMerge> myMerges = Collections15.arrayList();

    public MyBuilder(MergeOperationsManagerImpl manager, DBItemType type) {
      myManager = manager;
      myType = type;
    }

    @Override
    public void finish() {
      myManager.addMergeOperation(new CompositeMerge(myMerges), myType);
    }

    @Override
    public void uniteSetValues(DBAttribute<? extends Collection<? extends Long>> ... attributes) {
      myMerges.add(UniteSets.create(attributes));
    }

    @Override
    public Builder discardEdit(DBAttribute<?>... attributes) {
      myMerges.add(new CopyRemoteOperation(attributes));
      return this;
    }

    @Override
    public Builder addCustom(final ItemAutoMerge merge) {
      myMerges.add(merge);
      return this;
    }

    @Override
    public Builder mergeLongSets(DBAttribute<? extends Collection<? extends Long>>... attributes) {
      myMerges.add(new AutoMergeLongSets(attributes));
      return this;
    }

    @Override
    public void mergeStringSets(DBAttribute<? extends Collection<? extends String>> attribute) {
      myMerges.add(new AutoMergeStringSets(attribute));
    }

    @Override
    public Builder addConflictGroup(DBAttribute<?>... attrGroup) {
      myMerges.add(new AutoMergeConflictGroup(attrGroup));
      return this;
    }
  }
}
