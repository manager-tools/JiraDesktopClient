package com.almworks.api.engine.util;

import com.almworks.api.engine.PrimaryItemStructure;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.wrapper.ItemStorageAdaptor;
import com.almworks.util.bool.BoolExpr;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public class FixedPrimaryItemStructure implements PrimaryItemStructure {
  private final BoolExpr<DP> myPrimary;
  private final BoolExpr<DP> myModified;
  private final BoolExpr<DP> myConflict;
  private final BoolExpr<DP> myUploadable;
  private final DBAttribute<Long>[] myMasterAttributes;

  public FixedPrimaryItemStructure(DBItemType primaryItemType, Collection<? extends DBAttribute<Long>> masterAttributes) {
    //noinspection unchecked
    myMasterAttributes = masterAttributes.toArray(new DBAttribute[masterAttributes.size()]);
    myPrimary = DPEqualsIdentified.create(DBAttribute.TYPE, primaryItemType);
    myModified = BoolExpr.and(ItemStorageAdaptor.modified(myMasterAttributes), myPrimary);
    myConflict = BoolExpr.and(ItemStorageAdaptor.inConflict(myMasterAttributes), myPrimary);
    myUploadable = myModified.and(myConflict.negate());
  }

  public FixedPrimaryItemStructure(DBItemType primaryItemType, DBAttribute<Long> ... masterAttributes) {
    this(primaryItemType, Arrays.asList(masterAttributes));
  }

  @NotNull
  @Override
  public BoolExpr<DP> getPrimaryItemsFilter() {
    return myPrimary;
  }

  @NotNull
  @Override
  public BoolExpr<DP> getLocallyChangedFilter() {
    return myModified;
  }

  @NotNull
  @Override
  public BoolExpr<DP> getConflictingItemsFilter() {
    return myConflict;
  }

  @NotNull
  @Override
  public BoolExpr<DP> getUploadableItemsFilter() {
    return myUploadable;
  }

  @NotNull
  @Override
  public LongList loadEditableSlaves(ItemVersion primary) {
    if (myMasterAttributes.length == 0) return LongList.EMPTY;
    LongSetBuilder slaves = new LongSetBuilder();
    for (DBAttribute<Long> master : myMasterAttributes) {
      slaves.addAll(primary.getSlaves(master));
    }
    return slaves.isEmpty() ? LongList.EMPTY : slaves.commitToArray();
  }
}
