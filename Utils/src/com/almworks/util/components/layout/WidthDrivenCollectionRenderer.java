package com.almworks.util.components.layout;

import com.almworks.util.components.CollectionRenderer;
import com.almworks.util.components.renderer.CellState;

public interface WidthDrivenCollectionRenderer<T> extends CollectionRenderer<T> {
  int getPreferredHeight(CellState state, T item, int width);

}
