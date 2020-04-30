package com.almworks.util.models;

import com.almworks.util.components.CollectionRenderer;

import java.util.Comparator;

public class SimpleColumnAccessor<T> extends BaseTableColumnAccessor<T, T> {
  public SimpleColumnAccessor(String name, CollectionRenderer<T> dataRenderer, Comparator<T> tComparator) {
    super(name, dataRenderer, tComparator);
  }

  public SimpleColumnAccessor(String id, String name, CollectionRenderer<T> dataRenderer, Comparator<T> tComparator) {
    super(id, name, dataRenderer, tComparator);
  }

  public SimpleColumnAccessor(String name, CollectionRenderer<T> dataRenderer) {
    super(name, dataRenderer);
  }

  public SimpleColumnAccessor(String name) {
    super(name);
  }

  public T getValue(T object) {
    return object;
  }
}
