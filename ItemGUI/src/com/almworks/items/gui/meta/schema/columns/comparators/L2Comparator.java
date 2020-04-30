package com.almworks.items.gui.meta.schema.columns.comparators;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.ItemDownloadStageKey;
import com.almworks.api.application.LoadedItem;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.columns.ColumnComparator;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Procedure;
import com.almworks.util.models.TableColumnBuilder;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * Sequence: [l2Comparator: (subSequence or empty), (minStage : (byte, -1 for null), valueComparator: (not empty subSequence))?]
 */
final class L2Comparator extends ColumnComparator {
  public static final SerializableFeature<ColumnComparator> FEATURE = new SerializableFeature<ColumnComparator>() {
    @SuppressWarnings({"unchecked"})
    @Override
    public ColumnComparator restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      int length = stream.nextInt();
      if (length <= 0 || stream.isErrorOccurred()) return null;
      ByteArray.Stream l2Stream = stream.subStream(length);
      Comparator l2 = length == 0 ? null : SerializedObjectAttribute.restore(reader, l2Stream, Comparator.class,
        invalidate);
      if (l2 == null || !l2Stream.isSuccessfullyAtEnd()) return null;
      if (stream.isAtEnd()) return new L2Comparator(null, l2, null);
      int minStageValue = stream.nextByte();
      ItemDownloadStage minStage = minStageValue < 0 ? null : ItemDownloadStage.fromDbValue(minStageValue);
      int l1Length = stream.nextInt();
      ByteArray.Stream l1Stream = stream.subStream(l1Length);
      if (!stream.isSuccessfullyAtEnd()) return null;
      Comparator l1 = SerializedObjectAttribute.restore(reader, l1Stream, Comparator.class, invalidate);
      if (l1 == null || !l1Stream.isSuccessfullyAtEnd()) return null;
      return new L2Comparator(l1, l2, minStage);
    }

    @Override
    public Class<ColumnComparator> getValueClass() {
      return ColumnComparator.class;
    }
  };
  private final Comparator<Object> myL1;
  private final Comparator<LoadedItem> myL2;
  @Nullable
  private final ItemDownloadStage myMinStage;

  L2Comparator(Comparator<Object> l1, Comparator<LoadedItem> l2, @Nullable ItemDownloadStage minStage) {
    myL1 = l1;
    myL2 = l2;
    myMinStage = minStage != null ? ItemDownloadStage.fixForUI(minStage) : null;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    L2Comparator other = Util.castNullable(L2Comparator.class, obj);
    return other != null && Util.equals(myL1, other.myL1) && Util.equals(myL2, other.myL2);
  }

  @Override
  public int hashCode() {
    return Util.hashCode(myL1, myL2) ^ L2Comparator.class.hashCode();
  }

  @Override
  public <T> boolean setupComparator(TableColumnBuilder<LoadedItem, T> builder, @Nullable Convertor<LoadedItem, T> convertor) {
    if (convertor == null && myL1 != null) return false;
    //noinspection unchecked
    builder.setComparator(createComparator((Convertor<LoadedItem, Object>) convertor));
    return true;
  }

  private Comparator<LoadedItem> createComparator(@Nullable final Convertor<LoadedItem, Object> convertor) {
    return new MyComparator(convertor, this);
  }

  private final static class MyComparator implements Comparator<LoadedItem> {
    private final L2Comparator myData;
    private final Convertor<LoadedItem, Object> myConvertor;

    public MyComparator(Convertor<LoadedItem, Object> convertor, L2Comparator data) {
      myConvertor = convertor;
      myData = data;
    }

    @Override
    public int compare(LoadedItem o1, LoadedItem o2) {
      if (o1 == o2) return 0;
      int result = 0;
      if (myConvertor != null && myData.myL1 != null) result = compareByValue(o1, o2);
      if (result == 0 && myData.myL2 != null) result = myData.myL2.compare(o1, o2);
      if (result != 0) return result;
      if (o1 == null || o2 == null) return o1 == null ? -1 : 1;
      return Util.compareLongs(o1.getItem(), o2.getItem());
    }

    private int compareByValue(LoadedItem o1, LoadedItem o2) {
      if (myData.myMinStage != null) {
        ItemDownloadStage stage1 = ItemDownloadStageKey.retrieveValue(o1);
        ItemDownloadStage stage2 = ItemDownloadStageKey.retrieveValue(o2);
        boolean hasValue1 = ItemDownloadStage.hasValueForUI(myData.myMinStage, stage1);
        boolean hasValue2 = ItemDownloadStage.hasValueForUI(myData.myMinStage, stage2);
        if (!hasValue1 || !hasValue2) {
          if (!hasValue1 && !hasValue2) return 0;
          return hasValue1 ? -1 : 1;
        }
      }
      int result = 0;
      Object v1 = getValue(myConvertor, o1);
      Object v2 = getValue(myConvertor, o2);
      if (v1 != v2) result = myData.myL1.compare(v1, v2);
      return result;
    }

    private Object getValue(@NotNull Convertor<LoadedItem, Object> convertor, LoadedItem item) {
      return item != null ? convertor.convert(item) : null;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      MyComparator other = Util.castNullable(MyComparator.class, obj);
      return other != null && Util.equals(myConvertor, other.myConvertor) && Util.equals(myData, other.myData);
    }

    @Override
    public int hashCode() {
      return Util.hashCode(myConvertor, myData);
    }
  }
}
