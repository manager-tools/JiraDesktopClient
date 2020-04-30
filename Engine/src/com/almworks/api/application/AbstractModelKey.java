package com.almworks.api.application;

import com.almworks.api.engine.Connection;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author : Dyoma
 */
public abstract class AbstractModelKey<T> implements ModelKey<T> {
  private final TypedKey<T> myKey;

  protected AbstractModelKey(String name) {
    myKey = TypedKey.create(name);
  }

  @NotNull public String getName() {
    return getModelKey().getName();
  }

  public T getValue(ModelMap model) {
    return model.get(getModelKey());
  }

  @NotNull protected final TypedKey<T> getModelKey() {
    return myKey;
  }

  public boolean hasValue(ModelMap model) {
    return model.get(getModelKey()) != null;
  }

  public void setValue(PropertyMap values, T value) {
    values.put(getModelKey(), value);
  }

  public T getValue(PropertyMap values) {
    return values.get(getModelKey());
  }

  public boolean isEqualValue(ModelMap models, PropertyMap values) {
    return Util.equals(getValue(models), getValue(values));
  }

  public boolean isEqualValue(PropertyMap values1, PropertyMap values2) {
    return Util.equals(getValue(values1), getValue(values2));
  }

  public void copyValue(ModelMap to, PropertyMap from) {
    to.put(getModelKey(), getValue(from));
  }

  public void takeSnapshot(PropertyMap to, ModelMap from) {
    setValue(to, getValue(from));
  }

  public boolean hasValue(PropertyMap values) {
    return values.containsKey(getModelKey());
  }

  public int compare(T o1, T o2) {
    return 0;
  }

  public boolean isExportable(Collection<Connection> connections) {
    return true;
  }

  public String toString() {
    return myKey.getName();
  }

  public DataPromotionPolicy getDataPromotionPolicy() {
    return DataPromotionPolicy.STANDARD;
  }
}
