package com.almworks.util.properties;

import org.almworks.util.TypedKey;

/**
 * @author : Dyoma
 */
public interface PropertyChangeListener <T> {
  void propertyChanged(TypedKey<T> key, Object bean, T oldValue, T newValue);

  interface Any {
    void propertyChanged(TypedKey key);
  }
}
