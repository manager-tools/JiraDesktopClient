package com.almworks.util.models;

import com.almworks.util.components.CollectionRenderer;

/**
 * @author dyoma
 */
public interface ColumnAccessor<T> {
  CollectionRenderer<? super T> getDataRenderer();
}
