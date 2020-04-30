package com.almworks.util.properties;

import org.almworks.util.TypedKey;

import java.util.Set;

/**
 * @author : Dyoma
 */
public class PropertyModelMap extends PropertyMap {
  private static final Object MODEL_INSTALLED = new Object();

  public PropertyModelMap(Object sourceBean) {
    super(sourceBean);
  }

  public <M> void installProperty(PropertyKey<M, ?> key) {
    // TODOIJ#39926 degenerator bug: changSupport variable should not be inlined. Degenerator doesn't add imports required for added casts.
    key.installModel(getChangeSupport(), this);
  }

  public <M> M put(TypedKey<M> key, M value) {
    assert value != null;
    assert key instanceof PropertyKey;
    return super.put(key, value);
  }

  public <T> T get(TypedKey<T> key) {
    T model = super.get(key);
    assert model != MODEL_INSTALLED : key.getName();
    return model;
  }

  public <M, T> M getOrCreateCopy(PropertyKey<M, T> key, PropertyMap copyFrom) {
    if (!containsKey(key)) {
      installProperty(key);
      key.tryCopyValue(this, copyFrom);
    }
    return get(key);
  }

  public void ensureInstalled(PropertyKey<?, ?> key) {
    if (!containsKey(key))
      installProperty(key);
  }

  public <M> void markIntalled(PropertyKey<M, ?> key) {
    super.put((TypedKey<Object>) key, MODEL_INSTALLED);
  }

  public Set<? extends PropertyKey<?, ?>> keySet() {
    //noinspection RedundantCast
    return (Set) super.keySet();
  }
}
