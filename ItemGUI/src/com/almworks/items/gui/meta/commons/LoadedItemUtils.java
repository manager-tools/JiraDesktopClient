package com.almworks.items.gui.meta.commons;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Function2;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

public class LoadedItemUtils {
  private LoadedItemUtils() {}

  public static <T> T getModelKeyValue(ItemWrapper item, DBStaticObject keyIdentity, Class<T> valueClass) {
    ModelKey<?> modelKey = getModelKey(item, keyIdentity);
    Object value = modelKey == null ? null : modelKey.getValue(item.getLastDBValues());
    T typedValue = Util.castNullable(valueClass, value);
    if (typedValue == null && value != null) LogHelper.error("Wrong class", keyIdentity, modelKey, value, valueClass);
    return typedValue;
  }

  public static <T> Function2<ItemWrapper, DBStaticObject, T> getModelKeyValue(final Class<T> valueClass) {
    return new Function2<ItemWrapper, DBStaticObject, T>() {
      @Override
      public T invoke(ItemWrapper item, DBStaticObject key) {
        return getModelKeyValue(item, key, valueClass);
      }
    };
  }

  @Nullable
  public static LoadedModelKey<?> getModelKey(ItemWrapper item, DBStaticObject keyIdentity) {
    GuiFeaturesManager features = getFeatures(item);
    if (features == null) return null;
    LoadedModelKey<?> modelKey = features.findModelKey(keyIdentity);
    if (modelKey == null) LogHelper.error("Missing key", keyIdentity);
    return modelKey;
  }

  @Nullable
  public static GuiFeaturesManager getFeatures(ItemWrapper item) {
    GuiFeaturesManager features = item.services().getActor(GuiFeaturesManager.ROLE);
    if (features == null) LogHelper.error("No features manager");
    return features;
  }

  /**
   * Either returns the issues' common value for the specified key or throws {@link com.almworks.util.ui.actions.CantPerformException} if for any two issues the values differ.
   * @return model key value common for all specified items. null means all have nulls.
   * */
  @Nullable
  public static <T> T getCommonModelKeyValue(Iterable<ItemWrapper> items, DBStaticObject keyIdentity, Class<T> valueClass) throws CantPerformException {
    T commonValue = null;
    boolean initialized = false;
    for (ItemWrapper issue : items) {
      T value = getModelKeyValue(issue, keyIdentity, valueClass);
      if (initialized) CantPerformException.ensure(Util.equals(value, commonValue));
      else {
        commonValue = value;
        initialized = true;
      }
    }
    return commonValue;
  }
}
