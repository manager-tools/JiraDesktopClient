package com.almworks.items.gui.meta.schema.modelkeys;

import com.almworks.api.application.util.DataAccessor;
import com.almworks.api.application.util.DataIO;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.DataHolder;
import com.almworks.items.util.BadUtil;
import com.almworks.util.Break;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

public abstract class ModelKeyLoader {
  public static final DataLoader<ModelKeyLoader> LOADER = SerializedObjectAttribute.create(ModelKeyLoader.class,
    ModelKeys.DATA_LOADER);

  private final DataHolder myData;

  protected ModelKeyLoader(DataHolder data) {
    myData = data;
  }

  public <T> T getData(TypedKey<T> key) {
    return myData.getValue(key);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    ModelKeyLoader other = Util.castNullable(ModelKeyLoader.class, obj);
    return other != null && myData.equals(other.myData);
  }

  @Override
  public int hashCode() {
    return myData.hashCode();
  }

  public abstract boolean loadKey(LoadedModelKey.Builder<?> builder, GuiFeaturesManager guiFeatures);

  static <V> DBAttribute<V> readAttribute(ByteArray.Stream stream, DBReader reader, Class<V> scalarClass) throws Break {
    long attrItem = stream.nextLong();
    DBAttribute<V> attribute = BadUtil.getScalarAttribute(reader, attrItem, scalarClass);
    if (attribute == null) {
      LogHelper.error("Missing scalar attribute", attrItem, stream);
      throw new Break();
    }
    return attribute;
  }

  static <V> LoadedModelKey.Builder<V> prepareSimpleBuilder(LoadedModelKey.Builder<?> b, DBAttribute<V> attribute) {
    Class<V> scalarClass = BadUtil.getScalarAttributeClass(attribute);
    if (scalarClass == null) {
      LogHelper.error("Not scalar attribute", attribute);
      return null;
    }
    LoadedModelKey.Builder<V> builder = b.setDataClass(scalarClass);

    builder.setIO(new DataIO.SimpleIO<V>(attribute));
    builder.setAccessor(new DataAccessor.SimpleDataAccessor<V>(builder.getName()));
//      builder.setValueRenderer(Renderers.<V>defaultCanvasRenderer());
    return builder;
  }
}
