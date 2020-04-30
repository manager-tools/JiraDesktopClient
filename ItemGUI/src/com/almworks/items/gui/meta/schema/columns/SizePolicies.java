package com.almworks.items.gui.meta.schema.columns;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import com.almworks.util.models.ColumnSizePolicy;
import org.jetbrains.annotations.Nullable;

public class SizePolicies {
  private static final DBNamespace NS_FEATURES = Columns.NS_FEATURES.subNs("sizePolicies");
  public static final DBIdentity FREE_LETTER_M_WIDTH_FEATURE = feature("freeLetterMWidth");
  public static final DBIdentity FIXED_PIXELS_FEATURE = feature("fixedPixels");

  public static ScalarSequence freeLetterMWidth(int charCount) {
    return new ScalarSequence.Builder().append(FREE_LETTER_M_WIDTH_FEATURE).append(charCount).create();
  }

  public static ScalarSequence fixedPixels(int px) {
    return new ScalarSequence.Builder().append(FIXED_PIXELS_FEATURE).append(px).create();
  }

  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURES.object(id));
  }

  static void registerFeatures(FeatureRegistry reg) {
    reg.register(FREE_LETTER_M_WIDTH_FEATURE, FREE_LETTER_M_WIDTH);
    reg.register(FIXED_PIXELS_FEATURE, FIXED_PIXELS);
  }

  private static final SerializableFeature<ColumnSizePolicy> FREE_LETTER_M_WIDTH = new SerializableFeature<ColumnSizePolicy>() {
    @Override
    public ColumnSizePolicy restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      int charCount = stream.nextInt();
      if (!stream.isSuccessfullyAtEnd()) return null;
      return ColumnSizePolicy.Calculated.freeLetterMWidth(charCount);
    }

    @Override
    public Class<ColumnSizePolicy> getValueClass() {
      return ColumnSizePolicy.class;
    }
  };

  private static final SerializableFeature<ColumnSizePolicy> FIXED_PIXELS = new SerializableFeature<ColumnSizePolicy>() {
    @Override
    public ColumnSizePolicy restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      int px = stream.nextInt();
      if (!stream.isSuccessfullyAtEnd()) return null;
      return ColumnSizePolicy.Calculated.fixedPixels(px);
    }

    @Override
    public Class<ColumnSizePolicy> getValueClass() {
      return ColumnSizePolicy.class;
    }
  };

  private SizePolicies() {}
}
