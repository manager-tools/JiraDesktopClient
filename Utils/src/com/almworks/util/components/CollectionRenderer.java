package com.almworks.util.components;

import com.almworks.util.components.renderer.CellState;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public interface CollectionRenderer <T> {
  JComponent getRendererComponent(CellState state, T item);
}
