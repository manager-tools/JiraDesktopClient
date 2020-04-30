package com.almworks.items.gui.meta;

import com.almworks.api.application.DataPromotionPolicy;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.QueryImageSlice;
import com.almworks.items.cache.util.ItemImageCollector;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeyLoader;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeys;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.util.LogHelper;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModelKeyCollector implements ItemImageCollector.ImageFactory<LoadedModelKey<?>> {
  private static final DataLoader<DataPromotionPolicy> DATA_PROMOTION =  SerializedObjectAttribute.create(
    DataPromotionPolicy.class, ModelKeys.DATA_PROMOTION_POLICY);
  private final ItemImageCollector<LoadedModelKey<?>> myKeys;
  private final ItemImageCollector.GetUpToDate<LoadedModelKey<?>> myUpToDateKeys;

  private ModelKeyCollector(QueryImageSlice slice) {
    myKeys = ItemImageCollector.create(slice, this, false);
    myUpToDateKeys = ItemImageCollector.GetUpToDate.create(myKeys);
  }

  public static ModelKeyCollector create(DBImage image) {
    return new ModelKeyCollector(image.createQuerySlice(
      DPEqualsIdentified.create(DBAttribute.TYPE, ModelKeys.DB_TYPE)));
  }

  public static ModelKeyCollector getInstance(DBReader reader) {
    return GuiFeaturesManager.getInstance(reader).getModelKeyCollector();
  }

  public void start(Lifespan life) {
    ImageSlice slice = myKeys.getSlice();
    slice.addAttributes(ModelKeys.NAME);
    slice.addData(ModelKeyLoader.LOADER, DataLoader.IDENTITY_LOADER, DATA_PROMOTION);
    myKeys.start(life);
  }

  @Override
  public boolean update(LoadedModelKey<?> image, long item) {
    LogHelper.error("Attempt to update model key", item, image);
    return false;
  }

  @Override
  public void onRemoved(LoadedModelKey<?> image) {
    LogHelper.warning("ModelKey removed", image);
  }

  @SuppressWarnings( {"unchecked"})
  @Override
  public LoadedModelKey<?> create(long item) {
    LoadedModelKey.Builder<?> builder = createBuilder(item);
    return builder == null ? null : builder.createKey();
  }

  @Nullable
  private LoadedModelKey.Builder<?> createBuilder(long item) {
    ImageSlice slice = myKeys.getSlice();
    String name = slice.getValue(item, ModelKeys.NAME);
    ModelKeyLoader keyLoader = slice.getValue(item, ModelKeyLoader.LOADER);
    DataPromotionPolicy promotionPolicy = slice.getValue(item, DATA_PROMOTION);
    if (promotionPolicy == null) promotionPolicy = DataPromotionPolicy.STANDARD;

    if (name == null || keyLoader == null) {
      LogHelper.error("Missing data for model key", item, "name", name, "attribute", keyLoader);
      return null;
    }
    LoadedModelKey.Builder<?> builder = LoadedModelKey.Builder.create();
    builder.setName(name);
    builder.setPromotionPolicy(promotionPolicy);
    boolean valid = keyLoader.loadKey(builder, GuiFeaturesManager.getInstance(slice.getImage()));
    return valid ? builder : null;
  }

  public LoadedModelKey<?> getKey(long keyItem) {
    return myKeys.getImage(keyItem);
  }

  public LoadedModelKey<?> findKey(DBStaticObject keyId) {
    if (keyId == null) return null;
    LoadedModelKey<?> key = myKeys.findImageByValue(DataLoader.IDENTITY_LOADER, keyId.getIdentity());
    LogHelper.assertError(key != null, "Key not found", keyId);
    return key;
  }

  @ThreadSafe
  public List<LoadedModelKey<?>> getAllKeys() {
    return myUpToDateKeys.get();
  }
}
