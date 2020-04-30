package com.almworks.items.util;

import com.almworks.items.api.DBAttribute;

public interface AttributeValueFunction<V> {
  <T> V f(DBAttribute<T> attribute, T value, V parameter);
}
