package com.almworks.util.components.renderer;

import org.almworks.util.Log;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
public interface CanvasComponent {
  JComponent getComponent();

  void setCurrentElement(CanvasElement element);

  void setComponentFont(Font font);

  Font getDerivedFont(int fontStyle);

  void clear();

  Pattern getHighlightPattern();

  class Simple implements CanvasComponent {
    private final JComponent myComponent;
    private CanvasImpl myCanvas;

    public Simple(JComponent component) {
      myComponent = component;
    }

    public CanvasImpl useCanvas(CellState state) {
      CanvasImpl canvas = myCanvas;
      myCanvas = null;
      canvas = canvas != null ? canvas : new CanvasImpl();
      canvas.setupProperties(state);
      canvas.clear();
      return canvas;
    }

    public void releaseCanvas(CanvasImpl canvas) {
      canvas.clear();
      if (myCanvas == null) myCanvas = canvas;
    }

    @Override
    public JComponent getComponent() {
      return myComponent;
    }

    @Override
    public void setCurrentElement(CanvasElement element) {
    }

    @Override
    public void setComponentFont(Font font) {
      Log.error("change font should not happen");
    }

    @Override
    public Font getDerivedFont(int fontStyle) {
      return myComponent.getFont().deriveFont(fontStyle);
    }

    @Override
    public void clear() {
    }

    @Override
    public Pattern getHighlightPattern() {
      return null;
    }
  }
}
