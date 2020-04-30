package com.almworks.util.components.completion;

import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.CanvasSection;
import com.almworks.util.components.renderer.CanvasImpl;
import com.almworks.util.components.renderer.CellState;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

class BoldingCanvasRenderer<T> implements CanvasRenderer<T> {
  private final CanvasImpl myCanvas;
  private CanvasRenderer<? super T> myRenderer;
  private String myBoldText = null;

  public BoldingCanvasRenderer() {
    myCanvas = new CanvasImpl();
  }

  public void setRenderer(CanvasRenderer<? super T> renderer) {
    myRenderer = renderer;
  }

  public void setBoldText(@Nullable String boldText) {
    myBoldText = boldText;
  }

  public void renderStateOn(CellState state, com.almworks.util.components.Canvas canvas, T item) {
    if (myRenderer == null) return;
    CellState rendererState = state;
    if (myBoldText == null || myBoldText.length() == 0) {
      myRenderer.renderStateOn(rendererState, canvas, item);
      return;
    }
    myCanvas.clear();
    canvas.copyAttributes(myCanvas);
    myRenderer.renderStateOn(rendererState, myCanvas, item);
    myCanvas.copyAttributes(canvas);
    boolean firstLine = true;
    String substr = Util.lower(myBoldText);
    for (com.almworks.util.components.Canvas.Line line : myCanvas.getLines()) {
      if (firstLine) firstLine = false;
      else canvas.newLine().newSection();
      for (CanvasSection section : line.getSections()) {
        CanvasSection target = canvas.emptySection();
        String sectionText = section.getText();
        section.copyAttributes(target);
        int index = Util.lower(sectionText).indexOf(substr);
        if (index < 0) target.appendText(sectionText);
        else {
          if (index > 0) {
            target.appendText(sectionText.substring(0, index));
            target = canvas.emptySection();
            section.copyAttributes(target);
          }
          int endIndex = index + substr.length();
          target.appendText(sectionText.substring(index, endIndex));
          target.setFontStyle(Font.BOLD);
//          target.setBackground(GlobalColors.HIGHLIGHT_COLOR);
          if (endIndex < sectionText.length()) {
            target = canvas.emptySection();
            section.copyAttributes(target);
            target.appendText(sectionText.substring(endIndex));
          }
        }
      }
    }
  }
}
