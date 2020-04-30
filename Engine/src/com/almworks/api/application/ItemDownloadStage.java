package com.almworks.api.application;

import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

public enum ItemDownloadStage {
  NEW(0),
  DUMMY(1),
  STALE(2),
  QUICK(3),
  FULL(4);

  public static final ItemDownloadStage DEFAULT = QUICK;
  public static final BoolExpr<DP> IS_NEW = DPEquals.create(SyncAttributes.ITEM_DOWNLOAD_STAGE, NEW.getDbValue());

  private final Integer myDbValue;

  private ItemDownloadStage(Integer dbValue) {
    myDbValue = dbValue;
  }

  /**
   * Fixes stage for UI purposes, to avoid UI code analyze too many variants of download stage<br>
   * there is no difference between STALE and FULL (full data already available).<br>
   * NEW is equal to QUICK (at least some data is available)
   * @see #hasValueForUI(ItemDownloadStage, ItemDownloadStage)
   */
  public static ItemDownloadStage fixForUI(ItemDownloadStage stage) {
    if (stage == null) return DUMMY;
    switch (stage) {
    case DUMMY:
    case QUICK:
    case FULL: return stage;
    case NEW: return QUICK;
    case STALE: return FULL;
    default: return DUMMY;
    }
  }

  @NotNull
  public Integer getDbValue() {
    return myDbValue;
  }

  @NotNull
  public static ItemDownloadStage fromDbValue(Integer dbValue) {
    if (dbValue == null) return DEFAULT;
    for (ItemDownloadStage s : values()) {
      if (s.myDbValue == dbValue.intValue()) return s;
    }
    assert false : dbValue;
    Log.warn("unknown stage id " + dbValue);
    return DEFAULT;
  }

  public boolean wasFull() {
    return this == STALE || this == FULL;
  }

  public void setTo(ItemVersionCreator creator) {
    creator.setValue(SyncAttributes.ITEM_DOWNLOAD_STAGE, getDbValue());
  }

  @NotNull
  public static ItemDownloadStage getValue(ItemVersion item) {
    Integer value = item.getValue(SyncAttributes.ITEM_DOWNLOAD_STAGE);
    return fromDbValue(value);
  }

  /**
   * Checks if the item in actualStage has value that is available in minStage.
   * @param minStage minimum stage when data is available (should be preprocessed with {@link #fixForUI(ItemDownloadStage)}
   * @param actualStage actual stage of the item
   */
  public static boolean hasValueForUI(ItemDownloadStage minStage, ItemDownloadStage actualStage) {
    if (actualStage == null) return true;
    switch (actualStage) {
    case DUMMY: return minStage == ItemDownloadStage.DUMMY;
    case QUICK: return minStage != ItemDownloadStage.FULL;
    case NEW:
    case STALE:
    case FULL: return true;
    default: 
      LogHelper.error("Unknown stage", actualStage);
      return true;
    }
  }
}
