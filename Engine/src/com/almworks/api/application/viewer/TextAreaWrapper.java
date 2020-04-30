package com.almworks.api.application.viewer;

import com.almworks.util.components.renderer.CellState;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

/**
 * @author dyoma
*/
public interface TextAreaWrapper {

  JComponent getComponent();

  TextAreaWrapper createEditorWrapper();

  @Nullable
  Object setText(String text);

  void selectAll();

  void scrollToBeginning();

  int getPreferedHeight(CellState state, int width);

  void setCachedTextData(Object cachedData, String text);

  @Nullable
  String getTooltipAt(int x, int y);

  void paintAt(Graphics g, int x, int y);

  void setTextForeground(Color foreground);

  void setHighlightPattern(Pattern pattern);

  boolean processMouse(MouseEvent e);
}
