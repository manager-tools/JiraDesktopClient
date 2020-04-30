package com.almworks.util.components;

import com.almworks.util.components.plaf.macosx.combobox.MacPrettyComboBox;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ComboBoxUI;
import java.awt.*;

/**
 * @author dyoma
 */
public class ColumnComboBox extends MacPrettyComboBox {
  private int myColumns = 10;
  private Dimension myPreferredSize = null;
  private Dimension myMinSize = null;
  private Dimension myMaxSize = null;

  public Dimension getMinimumSize() {
    if (myMinSize != null) return new Dimension(myMinSize);
    Dimension minimumSize = super.getMinimumSize();
    if (myColumns > 0) {
      int preferredWidth = getPreferredWidth();
      if (minimumSize.width > preferredWidth)
        minimumSize = new Dimension(preferredWidth, minimumSize.height);
    }
    if (isDisplayable() && minimumSize != null) myMinSize = new Dimension(minimumSize);
    return minimumSize;
  }

  @Override
  public Dimension getMaximumSize() {
    if (myMaxSize != null) return new Dimension(myMaxSize);
    Dimension size = super.getMaximumSize();
    if (isDisplayable() && size != null) myMaxSize = new Dimension(size);
    return size;
  }

  public Dimension getPreferredSize() {
    if (myPreferredSize != null) return new Dimension(myPreferredSize);
    Dimension preferredSize = super.getPreferredSize();
    if (myColumns > 0)
      preferredSize = new Dimension(getPreferredWidth(), preferredSize.height);
    if (isDisplayable()) myPreferredSize = new Dimension(preferredSize);
    return preferredSize;
  }

  private int getPreferredWidth() {
    return UIUtil.getColumnWidth(this) * myColumns;
  }

  public int getColumns() {
    return myColumns;
  }

  public void setColumns(int columns) {
    if (columns != myColumns) {
      myColumns = columns;
      invalidate();
      clearSizeCache();
    }
  }

  @Override
  public void addNotify() {
    if (!isDisplayable()) clearSizeCache();
    super.addNotify();
  }

  @Override
  public void setRenderer(ListCellRenderer aRenderer) {
    super.setRenderer(aRenderer);
    clearSizeCache();
  }

  @Override
  public void setPrototypeDisplayValue(Object prototypeDisplayValue) {
    super.setPrototypeDisplayValue(prototypeDisplayValue);
    clearSizeCache();
  }

  @Override
  public void setModel(ComboBoxModel aModel) {
    super.setModel(aModel);
    clearSizeCache();
  }

  @Override
  public void setFont(Font font) {
    super.setFont(font);
    clearSizeCache();
  }

  @Override
  public void setUI(ComboBoxUI ui) {
    super.setUI(ui);
    clearSizeCache();
  }

  private void clearSizeCache() {
    myPreferredSize = null;
  }
}
