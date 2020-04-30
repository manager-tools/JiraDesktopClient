package com.almworks.util.components.renderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

/**
 * @author : Dyoma
 */
public abstract class CellState {
  public static final CellState LABEL = new LabelCellState();
  
  /**
   * @return background color, or null if renderer should not fillRect
   */
  @Nullable
  public abstract Color getBackground();

  public abstract Color getBackground(boolean opaque);

  public final Color getOpaqueBackground() {
    Color bg = getBackground();
    if (bg == null) bg = getBackground(true);
    assert bg != null;
    return bg;
  }

  @NotNull
  public abstract Color getForeground();

  @Nullable
  public abstract Border getBorder();

  @NotNull
  public abstract Color getDefaultBackground();

  public abstract Color getDefaultForeground();

  public abstract Color getSelectionBackground();

  public abstract Font getFont();

  public abstract boolean isEnabled();

  public abstract boolean isExpanded();

  public abstract boolean isFocused();

  public abstract boolean isLeaf();

  public abstract boolean isSelected();

  public abstract int getCellColumn();

  /**
   * Returns true if cell has been extracted from collection to be shown aside, as, for example,
   * selected item in a checkbox. Returns false by default.
   */
  public abstract boolean isExtracted();

  public abstract int getCellRow();

  public abstract int getComponentCellWidth();

  public abstract int getComponentCellHeight();

  public abstract Pattern getHighlightPattern();

  public FontMetrics getFontMetrics(int fontStyle) {
    Font font = getFont();
    if (font.getStyle() != fontStyle)
      font = font.deriveFont(fontStyle);
    return getFontMetrics(font);
  }

  public FontMetrics getFontMetrics() {
    return getFontMetrics(getFont());
  }

  protected FontMetrics getFontMetrics(Font font) {
    //noinspection Deprecation
    return Toolkit.getDefaultToolkit().getFontMetrics(font);
  }

  @Nullable
  public MouseEvent getMouseEvent() {
    return null;
  }

  @Nullable
  public Point getMousePoint(Component renderer) {
    return getMousePoint(getMouseEvent(), renderer);
  }

  public void setFeedbackTooltip(String tooltip) {
  }

  public void setFeedbackCursor(Cursor cursor) {
  }

  public void setHighlightPattern(Pattern pattern) {
  }

  public void setBackgroundTo(JComponent component, boolean changeOpaque) {
    if (component != null) {
      Color background = getBackground();
      boolean opaque = background != null;
      if (opaque) {
        component.setBackground(background);
      }
      if (changeOpaque && opaque != component.isOpaque()) {
        component.setOpaque(opaque);
      }
    }
  }

  public void setToLabel(JLabel label, Border innerBorder) {
    label.setFont(getFont());
    setBackgroundTo(label, true);
    label.setForeground(getForeground());
    label.setEnabled(isEnabled());
    Border border = getBorder();
    if (innerBorder != null) {
      if (border == null) {
        border = innerBorder;
      } else {
        border = new CompoundBorder(border, innerBorder);
      }
    }
    label.setBorder(border);
  }

  @Nullable
  public static Point getMousePoint(MouseEvent event, Component renderer) {
    if (event == null)
      return null;
    Point p = event.getPoint();
    Component root = event.getComponent();
    int dx = 0;
    int dy = 0;
    for (Component c = renderer; c != null; c = c.getParent()) {
      if (c == root)
        break;
      dx += c.getX();
      dy += c.getY();
    }
    return new Point(p.x - dx, p.y - dy);
  }
}
