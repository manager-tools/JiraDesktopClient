package com.almworks.util.components.renderer;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.regex.Pattern;

public class SimpleCellState extends CellState {
  private final EmptyBorder myBorder = new EmptyBorder(1, 1, 1, 1);
  private Color myBackground;
  private Color myForeground;
  private Font myFont;
  private Pattern myHighlightPattern;

  public SimpleCellState(Color background, Color foreground, Font font) {
    myBackground = background;
    myForeground = foreground;
    myFont = font;
  }

  public SimpleCellState(JComponent component) {
    this(component.getBackground(), component.getForeground(), component.getFont());
  }

  @Override
  public Color getBackground() {
    return getBackground(true);
  }

  @Override
  public Color getBackground(boolean opaque) {
    return myBackground;
  }

  public Border getBorder() {
    return myBorder;
  }

  @NotNull
  public Color getDefaultBackground() {
    return myBackground != null ? myBackground : Color.WHITE;
  }

  public Color getSelectionBackground() {
    return getDefaultBackground();
  }

  public Color getDefaultForeground() {
    return myForeground;
  }

  public Font getFont() {
    return myFont;
  }

  @NotNull
  public Color getForeground() {
    return myForeground != null ? myForeground : Color.BLACK;
  }

  public boolean isEnabled() {
    return true;
  }

  public boolean isExpanded() {
    return false;
  }

  public boolean isFocused() {
    return false;
  }

  public boolean isLeaf() {
    return true;
  }

  public boolean isSelected() {
    return false;
  }

  public boolean isExtracted() {
    return false;
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
    return myHighlightPattern;
  }

  public void setHighlightPattern(Pattern highlightPattern) {
    myHighlightPattern = highlightPattern;
  }
}
