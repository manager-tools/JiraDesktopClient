package com.almworks.util.ui.widgets;

import org.almworks.util.TypedKey;

import javax.swing.*;
import java.awt.*;

/**
 * Providers access to global host data and properties
 */
public interface WidgetHost {
  FontMetrics getFontMetrics();

  FontMetrics getFontMetrics(int style);

  /**
   * @return Swing component hosting the cells
   */
  JComponent getHostComponent();

  <T> T getWidgetData(TypedKey<T> key);

  <T> void putWidgetData(TypedKey<T> key, T data);
}
