package com.almworks.util;

import org.almworks.util.TypedKey;

public interface TypedMap {
  <T> T getValue(TypedKey<T> key);
}
