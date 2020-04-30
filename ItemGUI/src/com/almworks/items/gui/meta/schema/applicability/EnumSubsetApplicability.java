package com.almworks.items.gui.meta.schema.applicability;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.BadUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class EnumSubsetApplicability implements Applicability {
  public static final SerializableFeature<Applicability> FEATURE = new SerializableFeature<Applicability>() {
    @Override
    public Applicability restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      long attrItem = stream.nextLong();
      long modelKey = stream.nextLong();
      int subsetCount = stream.nextInt();
      if (attrItem <= 0 || subsetCount < 0 || modelKey <= 0) {
        LogHelper.error("Wrong data", attrItem, subsetCount, stream);
        return null;
      }
      DBAttribute<Long> primaryAttribute = BadUtil.getScalarAttribute(reader, attrItem, Long.class);
      if (primaryAttribute == null) {
        LogHelper.error("Wrong attribute", attrItem, stream);
        return null;
      }
      LongArray subset = new LongArray();
      for (int i = 0; i < subsetCount; i++) {
        long enumItem = stream.nextLong();
        if (enumItem < 0 || stream.isErrorOccurred()) {
          LogHelper.error("Failed to load", attrItem, stream, primaryAttribute);
          return null;
        }
        subset.add(enumItem);
      }
      if (!stream.isSuccessfullyAtEnd()) {
        LogHelper.error("Not at end", primaryAttribute, stream);
        return null;
      }
      subset.sortUnique();
      return create(primaryAttribute, modelKey, subset);
    }

    @Override
    public Class<Applicability> getValueClass() {
      return Applicability.class;
    }
  };

  private final DBAttribute<Long> myPrimaryAttribute;
  private final long myModelKey;
  private final long[] mySubset;

  private EnumSubsetApplicability(DBAttribute<Long> primaryAttribute, long modelKey, long[] subset) {
    myPrimaryAttribute = primaryAttribute;
    myModelKey = modelKey;
    mySubset = subset;
    Arrays.sort(mySubset);
  }

  public static Applicability create(DBAttribute<Long> primaryAttribute, long modelKey, LongList subset) {
    return new EnumSubsetApplicability(primaryAttribute, modelKey, subset.toNativeArray());
  }

  @Override
  public boolean isApplicable(ItemWrapper item) {
    if (mySubset.length == 0) return true;
    GuiFeaturesManager manager = item.services().getActor(GuiFeaturesManager.ROLE);
    LoadedModelKey<?> mk = manager.getModelKeyCollector().getKey(myModelKey);
    if (mk == null) {
      LogHelper.error("Missing model key", myModelKey, myPrimaryAttribute);
      return false;
    }
    LoadedModelKey<ItemKey> modelKey = mk.castScalar(ItemKey.class);
    if (modelKey == null) {
      LogHelper.error("Wrong model key", mk, myPrimaryAttribute);
      return false;
    }
    ItemKey value = item.getModelKeyValue(modelKey);
    long itemValue = value != null ? value.getItem() : 0;
    return Arrays.binarySearch(mySubset, itemValue) >= 0;
  }

  @Override
  public boolean isApplicable(EditModelState model) {
    if (mySubset.length == 0) return true;
    Pair<LongList, LongList> values = model.getCubeAxis(myPrimaryAttribute);
    return values == null || ItemHypercubeUtils.matches(LongArray.create(mySubset), values.getFirst(), values.getSecond());
  }

  @Override
  public boolean isApplicable(ItemVersion item) {
    if (mySubset.length == 0) return true;
    long value = item.getNNValue(myPrimaryAttribute, 0l);
    return Arrays.binarySearch(mySubset, value) >= 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return false;
    EnumSubsetApplicability other = Util.castNullable(EnumSubsetApplicability.class, obj);
    return other != null && Util.equals(myPrimaryAttribute, other.myPrimaryAttribute) && myModelKey == other.myModelKey && Arrays.equals(mySubset, other.mySubset);
  }

  @Override
  public int hashCode() {
    return myPrimaryAttribute.hashCode() ^ (int)myModelKey + mySubset.length;
  }

  @Override
  public String toString() {
    return "EnumSubsetApplicability(" + myPrimaryAttribute + " in " + ArrayUtil.toString(mySubset) + ")";
  }
}
