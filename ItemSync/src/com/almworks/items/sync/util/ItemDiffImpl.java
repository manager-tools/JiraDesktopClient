package com.almworks.items.sync.util;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemDiff;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ModifiableDiff;
import com.almworks.items.util.AttributeMap;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemDiffImpl implements ModifiableDiff {
  private final ItemVersion myElder;
  private final ItemVersion myNewer;
  private final Set<DBAttribute<?>> myChanges = Collections15.hashSet();
  private final MergeHistoryState myHistoryState;

  private ItemDiffImpl(ItemVersion elder, ItemVersion newer, HistoryRecord[] history, int commonHistory) {
    myHistoryState = MergeHistoryState.create(history, commonHistory);
    myElder = elder;
    myNewer = newer;
  }

  public long getItem() {
    return myNewer.getItem();
  }

  public boolean hasChanges() {
    boolean hasHistory = hasHistory();
    return hasHistory || !myChanges.isEmpty();
  }

  @Override
  public boolean hasHistory() {
    return myHistoryState.hasHistory();
  }

  @Override
  public List<HistoryRecord> getHistory() {
    return myHistoryState.getHistory();
  }

  @Override
  public ItemVersion getNewerVersion() {
    return myNewer;
  }

  @Override
  public ItemVersion getElderVersion() {
    return myElder;
  }

  public Collection<? extends DBAttribute<?>> getChanged() {
    return Collections.unmodifiableCollection(myChanges);
  }

  public DBReader getReader() {
    return myNewer.getReader();
  }

  public <T> T getNewerValue(DBAttribute<? extends T> attribute) {
    return myNewer.getValue(attribute);
  }

  @Override
  public final <T> T getNewerNNVale(DBAttribute<? extends T> attribute, T nullValue) {
    return Util.NN(getNewerValue(attribute), nullValue);
  }

  @Override
  public <T> T getElderValue(DBAttribute<? extends T> attribute) {
    return myElder.getValue(attribute);
  }

  @Override
  public final <T> T getElderNNValue(DBAttribute<? extends T> attribute, T nullValue) {
    return Util.NN(getElderValue(attribute), nullValue);
  }

  public boolean isChanged(DBAttribute<?> attribute) {
    return myChanges.contains(attribute);
  }

  public static void collectChanges(DBReader reader, AttributeMap values1, AttributeMap values2, Collection<? super DBAttribute<?>> target) {
    for (DBAttribute<?> attribute : values1.keySet())
      if (!SyncUtils.isEqualValueInMap(reader, attribute, values1, values2)) target.add(attribute);
    for (DBAttribute<?> attribute : values2.keySet()) {
      if (!target.contains(attribute) && !SyncUtils.isEqualValueInMap(reader, attribute, values1, values2))
        target.add(attribute);
    }
  }

  public static ItemDiffImpl createServerDiff(ItemVersion elder, ItemVersion newer) {
    if (elder == null && newer == null) throw new NullPointerException();
    if (elder == null || newer == null) {
      Log.error("Some version is null " + elder + " " + newer);
      ItemVersion nn = elder == null ? newer : elder;
      return new ItemDiffImpl(nn, nn, null, 0);
    }
    return createAndCollectChanges(elder, newer, null, 0);
  }

  public static ItemDiffImpl createToTrunk(ItemVersion elder, int commonHistory) {
    if (elder == null) throw new NullPointerException("No version");
    ItemVersion trunk = elder.switchToTrunk();
    return createAndCollectChanges(elder, trunk, ItemVersionCommonImpl.getHistory(trunk), commonHistory);
  }

  private static ItemDiffImpl createAndCollectChanges(@NotNull ItemVersion elder, @NotNull ItemVersion newer, HistoryRecord[] history,
    int commonHistory) {
    ItemDiffImpl diff = new ItemDiffImpl(elder, newer, history, commonHistory);
    AttributeMap elderValues = elder.getAllShadowableMap();
    AttributeMap newerValues = newer.getAllShadowableMap();
    collectChanges(diff.getReader(), elderValues, newerValues, diff.myChanges);
    return diff;
  }

  /**
   * @return null if history not updated otherwise not null. Empty array for update to empty history
   */
  @Nullable
  public List<HistoryRecord> getUpdatedHistory() {
    return myHistoryState.getUpdatedHistory();
  }

  @Override
  public void removeHistoryRecord(int recordId) {
    myHistoryState.removeHistoryRecord(recordId);
  }

  @Override
  public void addChange(DBAttribute<?>... attributes) {
    addChange(Arrays.asList(attributes));
  }

  @Override
  public void addChange(Collection<? extends DBAttribute<?>> attributes) {
    if (attributes != null) myChanges.addAll(attributes);
  }

  public static <T> boolean isEqualNewer(ItemDiff diff1, ItemDiff diff2, DBAttribute<T> attribute) {
    T val1 = diff1.getNewerValue(attribute);
    T val2 = diff2.getNewerValue(attribute);
    return SyncUtils.isEqualValue(diff1.getReader(), attribute, val1, val2);
  }
}
