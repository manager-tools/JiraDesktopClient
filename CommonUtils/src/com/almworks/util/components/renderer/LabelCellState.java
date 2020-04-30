package com.almworks.util.components.renderer;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.regex.Pattern;

class LabelCellState extends CellState {
  @Override
  public Color getBackground() {
    return getBackground(true);
  }

  @Override
  public Color getBackground(boolean opaque) {
    Color color = UIManager.getColor("Label.background");
    return color != null ? color : Color.LIGHT_GRAY;
  }

  @NotNull
  public Color getForeground() {
    Color color = UIManager.getColor("Label.foreground");
    return color != null ? color : Color.BLACK;
  }

  public Font getFont() {
    return UIManager.getDefaults().getFont("Label.font");
  }

  public boolean isEnabled() {
    return true;
  }

  public boolean isSelected() {
    return false;
  }

  public boolean isExtracted() {
    return false;
  }

  public boolean isFocused() {
    return false;
  }

  public Border getBorder() {
    return null;
  }

  public boolean isExpanded() {
    return false;
  }

  public boolean isLeaf() {
    return false;
  }

  @NotNull
  public Color getDefaultBackground() {
    Color color = UIManager.getColor("Label.background");
    return color != null ? color : Color.WHITE;
  }

  public Color getSelectionBackground() {
    return getDefaultBackground();
  }

  public Color getDefaultForeground() {
    return getForeground();
  }

  public int getCellColumn() {
    return 0;
  }

  public int getCellRow() {
    return 0;
  }

  public int getComponentCellWidth() {
    return 0;
  }

  public int getComponentCellHeight() {
    return 0;
  }

  public Pattern getHighlightPattern() {
    return null;
  }
}
