package com.almworks.util.components.renderer;

import com.almworks.util.ui.swing.AwtUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

public abstract class ComponentCellState extends CellState {
  private final JComponent myComponent;
  private Pattern myHighlightPattern;

  protected ComponentCellState(JComponent component) {
    assert component != null;
    myComponent = component;
  }

  @NotNull
  public Color getDefaultBackground() {
    Color background = myComponent.getBackground();
    return background != null ? background : AwtUtil.getPanelBackground();
  }

  @NotNull
  public Color getDefaultForeground() {
    return myComponent.getForeground();
  }

  public Font getFont() {
    return myComponent.getFont();
  }

  @SuppressWarnings({"RefusedBequest"})
  protected FontMetrics getFontMetrics(Font font) {
    return myComponent.getFontMetrics(font);
  }

  public boolean isEnabled() {
    return myComponent.isEnabled();
  }

  public boolean isExtracted() {
    return false;
  }

  protected JComponent getComponent() {
    return myComponent;
  }

  public Pattern getHighlightPattern() {
    return myHighlightPattern;
  }

  public void setHighlightPattern(Pattern highlightPattern) {
    myHighlightPattern = highlightPattern;
  }
}
