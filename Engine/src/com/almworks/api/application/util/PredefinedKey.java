package com.almworks.api.application.util;

import com.almworks.api.application.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.detach.Lifespan;

/**
 * @author : Dyoma
 */
public class PredefinedKey <T> extends AbstractModelKey<T> {
  private PredefinedKey(String name) {
    super(name);
  }

  public <SM>SM getModel(Lifespan lifespan, ModelMap model, Class<SM> aClass) {
    assert false : getName();
    return null;
  }

  public void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values) {
  }

  public static <T> ModelKey<T> create(String name) {
    return new PredefinedKey<T>(name);
  }

  public DataPromotionPolicy getDataPromotionPolicy() {
    return DataPromotionPolicy.ALWAYS;
  }
}
