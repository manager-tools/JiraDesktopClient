package com.almworks.util.components;

import com.almworks.util.components.renderer.ListCellState;
import com.almworks.util.components.speedsearch.ListSpeedSearch;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
class ListCellRendererAdapter <T> implements ListCellRenderer {
  private final CollectionRenderer<T> myRenderer;

  public ListCellRendererAdapter(CollectionRenderer<? super T> renderer) {
    myRenderer = (CollectionRenderer<T>) renderer;
  }

  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    T item = (T) value;
    cellHasFocus = ListSpeedSearch.fixFocusedState(list, cellHasFocus, index, 0);
    return myRenderer.getRendererComponent(createCellState(list, isSelected, cellHasFocus, index), item);
  }

  public CollectionRenderer<T> getRenderer() {
    return myRenderer;
  }

  public ListCellState createCellState(JList list, boolean isSelected, boolean cellHasFocus, int index) {
    cellHasFocus = ListSpeedSearch.fixFocusedState(list, cellHasFocus, index, 0);
    return new ListCellState(list, isSelected, cellHasFocus, index);
  }
}
