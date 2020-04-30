package com.almworks.util.components.renderer;

import com.almworks.util.Env;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ListCellState extends ComponentCellState {
  private static final boolean IS_MAC = Env.isMac();
  
  private final boolean mySelected;
  private final boolean myCellHasFocus;
  private final int myIndex;
  protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

  public ListCellState(JList list, boolean selected, boolean cellHasFocus, int index) {
    super(list);
    mySelected = selected;
    myCellHasFocus = cellHasFocus;
    myIndex = index;
  }

  protected JList getComponent() {
    return (JList) super.getComponent();
  }

  @Override
  public Color getBackground() {
    return getBackground(getComponent().isOpaque());
  }

  public Color getBackground(boolean opaque) {
    JList list = getComponent();
    return getBackground(mySelected, isEnabled(), opaque, list.getSelectionBackground(), getDefaultBackground());
  }

  public static Color getBackground(boolean selected, boolean enabled, boolean opaque, Color selectionBg, Color defaultBg) {
    if (!Env.isWindows() && !Env.isMacLeopardOrNewer() && !opaque) return null;
    if (!selected) return defaultBg;
    return enabled ? selectionBg : UIManager.getColor("Label.background").brighter();
  }

  public Color getSelectionBackground() {
    return getComponent().getSelectionBackground();
  }

  @NotNull
  public Color getForeground() {
    return getForeground(isEnabled(), mySelected, getComponent().getSelectionForeground(), getDefaultForeground());
  }

  public static Color getForeground(boolean enabled, boolean selected, Color selectionFg, Color defaultFg) {
    if (!enabled) return UIManager.getColor("Label.background").darker();
    return selected ? selectionFg : defaultFg;
  }

  public Border getBorder() {
    return getBorder(myCellHasFocus);
  }

  public static Border getBorder(boolean hasFocus) {
    return IS_MAC
      ? noFocusBorder
      : (hasFocus ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
  }

  public boolean isExpanded() {
    return false;
  }

  public boolean isLeaf() {
    return true;
  }

  public boolean isSelected() {
    return mySelected;
  }

  public boolean isExtracted() {
    return myIndex == -1;
  }

  public boolean isFocused() {
    return myCellHasFocus;
  }

  public int getCellColumn() {
    return 0;
  }

  public int getCellRow() {
    return myIndex;
  }

  public int getComponentCellWidth() {
    return getComponent().getWidth();
  }

  public int getComponentCellHeight() {
    int fixedCellHeight = getComponent().getFixedCellHeight();
    if (fixedCellHeight > 0)
      return fixedCellHeight;
    return getComponent().getCellBounds(myIndex, myIndex).height;
  }
}
