package com.almworks.util.components;

public interface CollectionCommandListener<T> {
  void onCollectionCommand(ACollectionComponent<T> component, int index, T element);
}
