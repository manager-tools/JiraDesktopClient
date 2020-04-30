package com.almworks.util.components.renderer;

import org.jetbrains.annotations.NotNull;

import javax.swing.border.Border;
import java.awt.*;
import java.util.regex.Pattern;

public class DelegatingCellState extends CellState {
  private final CellState myState;

  public DelegatingCellState(CellState state) {
    super();
    myState = state;
  }

  @Override
  public Color getBackground(boolean opaque) {
    return myState.getBackground(opaque);
  }

  public Color getBackground() {
    return myState.getBackground();
  }

  public Border getBorder() {
    return myState.getBorder();
  }

  @NotNull
  public Color getDefaultBackground() {
    return myState.getDefaultBackground();
  }

  public Color getSelectionBackground() {
    return myState.getSelectionBackground();
  }

  public Color getDefaultForeground() {
    return myState.getDefaultForeground();
  }

  protected FontMetrics getFontMetrics(Font font) {
    return myState.getFontMetrics(font);
  }

  public Font getFont() {
    return myState.getFont();
  }

  public Color getForeground() {
    return myState.getForeground();
  }

  public boolean isEnabled() {
    return myState.isEnabled();
  }

  public boolean isExpanded() {
    return myState.isExpanded();
  }

  public boolean isFocused() {
    return myState.isFocused();
  }

  public Pattern getHighlightPattern() {
    return myState.getHighlightPattern();
  }

  public boolean isLeaf() {
    return myState.isLeaf();
  }

  public boolean isSelected() {
    return myState.isSelected();
  }

  public boolean isExtracted() {
    return myState.isExtracted();
  }

  public int getCellColumn() {
    return myState.getCellColumn();
  }

  public int getCellRow() {
    return myState.getCellRow();
  }

  public int getComponentCellWidth() {
    return myState.getComponentCellWidth();
  }

  public int getComponentCellHeight() {
    return myState.getComponentCellHeight();
  }
}
