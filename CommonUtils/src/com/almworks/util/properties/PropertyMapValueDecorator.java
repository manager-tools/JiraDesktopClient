package com.almworks.util.properties;

import org.almworks.util.TypedKey;

public interface PropertyMapValueDecorator {
  <T> T decorate(PropertyMap map, TypedKey<T> key, T value);
}
