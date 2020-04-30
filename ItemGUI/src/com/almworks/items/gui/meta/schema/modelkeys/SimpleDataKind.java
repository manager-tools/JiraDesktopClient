package com.almworks.items.gui.meta.schema.modelkeys;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.DataHolder;
import com.almworks.util.Break;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

class SimpleDataKind<T> extends ModelKeyLoader {
  private static final TypedKey<DBAttribute<?>> ATTRIBUTE = TypedKey.create("attribute");
  private static final TypedKey<?>[] KEYS = {ATTRIBUTE};

  public SimpleDataKind(DBAttribute<T> attribute) {
    super(new DataHolder(KEYS).setValue(ATTRIBUTE, attribute));
  }

  public static <V> SerializableFeature<ModelKeyLoader> scalarValue(final Class<V> scalarClass) {
    return new SerializableFeature<ModelKeyLoader>() {
      @Override
      public ModelKeyLoader restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        try {
          DBAttribute<V> attribute = readAttribute(stream, reader, scalarClass);
          if (!stream.isSuccessfullyAtEnd()) return null;
          return new SimpleDataKind(attribute);
        } catch (Break err) { return null; }
      }

      @Override
      public Class<ModelKeyLoader> getValueClass() {
        return ModelKeyLoader.class;
      }
    };
  }

  @SuppressWarnings( {"unchecked"})
  @Override
  public boolean loadKey(LoadedModelKey.Builder<?> b, GuiFeaturesManager guiFeatures) {
    DBAttribute<T> attribute = getAttribute();
    LoadedModelKey.Builder<T> builder = prepareSimpleBuilder(b, attribute);
    return builder != null;
  }

  protected DBAttribute<T> getAttribute() {
    return (DBAttribute<T>) getData(ATTRIBUTE);
  }
}
