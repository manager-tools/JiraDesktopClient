package com.almworks.api.application;

import org.almworks.util.TypedKey;

public interface ModelKeyWithOptionalBehaviors {
  public <T> T getOptionalBehavior(TypedKey<T> key);
}
