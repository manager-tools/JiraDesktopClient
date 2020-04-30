package com.almworks.util.models;

import com.almworks.util.components.CollectionRenderer;

import java.util.Comparator;

public abstract class DeafTableColumnAccessor <E, V> extends BaseTableColumnAccessor<E, V> {
  public DeafTableColumnAccessor(String name) {
    super(name);
  }

  public DeafTableColumnAccessor(String name, CollectionRenderer dataRenderer) {
    super(name, dataRenderer);
  }

  public DeafTableColumnAccessor(String name, CollectionRenderer dataRenderer, Comparator<V> comparator) {
    super(name, dataRenderer, comparator);
  }

}
