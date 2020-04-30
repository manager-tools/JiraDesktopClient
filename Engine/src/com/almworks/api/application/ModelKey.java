package com.almworks.api.application;

import com.almworks.api.application.util.PredefinedKey;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.collections.Convertor;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.external.BitSet2;

/**
 * @author : Dyoma
 */
public interface ModelKey<V> {
  Convertor<ModelKey<?>, String> GET_NAME = new GetNameConvertor();
  /**
   * Stores bit set of used model keys
   * IMPORTANT: Do not put bitsets into model manually, use ModelKeySetUtil
   */
  ModelKey<BitSet2> ALL_KEYS = PredefinedKey.create("#allKeys");

  @NotNull
  String getName();

  V getValue(ModelMap model);

  @Nullable
  <SM> SM getModel(Lifespan lifespan, ModelMap model, Class<SM> aClass);

  boolean hasValue(ModelMap model);

  void setValue(PropertyMap values, V value);

  V getValue(PropertyMap values);

  boolean isEqualValue(ModelMap models, PropertyMap values);

  boolean isEqualValue(PropertyMap values1, PropertyMap values2);

  void copyValue(ModelMap to, PropertyMap from);

  boolean hasValue(PropertyMap values);

  void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values);

  void takeSnapshot(PropertyMap to, ModelMap from);

  /**
   * This policy is used by frozen table mode to transparently update frozen item (to avoid "has changes" mark)
   */
  DataPromotionPolicy getDataPromotionPolicy();


  public static class GetNameConvertor extends Convertor<ModelKey<?>, String> {
    public String convert(ModelKey<?> value) {
      return value.getName();
    }
  }
}
