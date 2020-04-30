package com.almworks.items.gui.meta.schema.columns.comparators;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.columns.ColumnComparator;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Procedure;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public class ComparatorFeatures {
  public static final SerializableFeature<ColumnComparator> L2 = L2Comparator.FEATURE;
  public static final SerializableFeature<Comparator> REVERSE_ORDER = new SerializableFeature<Comparator>() {
    @Override
    public Comparator restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      Comparator comparator = SerializedObjectAttribute.restore(reader, stream, Comparator.class, invalidate);
      if (comparator == null || !stream.isSuccessfullyAtEnd()) return null;
      return Containers.reverse(comparator);
    }

    @Override
    public Class<Comparator> getValueClass() {
      return Comparator.class;
    }
  };
  public static final SerializableFeature<Comparator> NULL_FIRST = new NullFirst(true);
  public static final SerializableFeature<Comparator> NULL_LAST = new NullFirst(false);
  public static final SerializableFeature<Comparator> LIST_SIZE = new ListSize();
  public static final SerializableFeature<Comparator> LIST_LEXICAL = new ListLexical();
}
