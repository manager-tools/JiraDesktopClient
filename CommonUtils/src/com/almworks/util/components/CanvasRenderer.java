package com.almworks.util.components;

import com.almworks.util.components.renderer.CellState;

/**
 * @author : Dyoma
 */
public interface CanvasRenderer<T> {
  void renderStateOn(CellState state, Canvas canvas, T item);
}
