package com.almworks.api.application;

import com.almworks.api.engine.Connection;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.detach.Lifespan;

import java.util.Collection;

/**
 * @author : Dyoma
 */
public abstract class SystemKey<T> extends AbstractModelKey<T> {
  public SystemKey(String name) {
    super(name);
  }

  public <SM>SM getModel(Lifespan lifespan, ModelMap model, Class<SM> aClass) {
    assert false : getName();
    return null;
  }

  public void setValue(PropertyMap values, T value) {
    assert false : getName();
  }

  public void takeSnapshot(PropertyMap to, ModelMap from) {
    super.setValue(to, getValue(from));
  }

  public boolean isExportable(Collection<Connection> connections) {
    return false;
  }

  public DataPromotionPolicy getDataPromotionPolicy() {
    return DataPromotionPolicy.ALWAYS;
  }
}
