package com.almworks.api.container;

import com.almworks.util.exec.ContextDataProvider;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

public class ComponentContainerDataProvider extends ContextDataProvider {
  private final ComponentContainer myContainer;

  public ComponentContainerDataProvider(@NotNull ComponentContainer container) {
    myContainer = container;
  }

  public <T> T getObject(TypedKey<T> key, int depth) {
    return myContainer.getActor(key);
  }

  public <T> T getObject(Class<T> objectClass, int depth) {
    return myContainer.getActor(objectClass);
  }
}
