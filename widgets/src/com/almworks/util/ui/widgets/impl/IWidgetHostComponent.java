package com.almworks.util.ui.widgets.impl;

import org.almworks.util.TypedKey;

import javax.swing.*;
import java.awt.*;

public interface IWidgetHostComponent {
  /**
   * Color to paint selection background, provided by host
   */
  TypedKey<Color> SELECTION_BACKGROUND = TypedKey.create("selection.background");
  /**
   * Color to paint selection foreground, provided by host
   */
  TypedKey<Color> SELECTION_FOREGROUND = TypedKey.create("selection.foreground");

  void setCursor(Cursor cursor);

  void repaint(int x, int y, int width, int height);

  void updateAll();

  JComponent getHostComponent();

  void fullRefresh();

  void repaintAll();

  int getWidth();

  int getHeight();

  void setRemovingComponent(Component component);

  void revalidate();

  void widgetRequestsFocus();
}
