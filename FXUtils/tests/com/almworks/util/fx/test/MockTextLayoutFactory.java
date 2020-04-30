package com.almworks.util.fx.test;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.scene.text.*;
import javafx.scene.shape.PathElement;

class MockTextLayoutFactory implements TextLayoutFactory {
  @Override
  public TextLayout createLayout() {
    return new TextLayout() {
      @Override
      public boolean setContent(TextSpan[] spans) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public boolean setContent(String string, Object font) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public boolean setAlignment(int alignment) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public boolean setWrapWidth(float wrapWidth) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public boolean setLineSpacing(float spacing) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public boolean setDirection(int direction) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public boolean setBoundsType(int type) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public BaseBounds getBounds() {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public BaseBounds getBounds(TextSpan filter, BaseBounds bounds) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public BaseBounds getVisualBounds(int type) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public TextLine[] getLines() {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public GlyphList[] getRuns() {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public Shape getShape(int type, TextSpan filter) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public HitInfo getHitInfo(float x, float y) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public PathElement[] getCaretShape(int offset, boolean isLeading, float x, float y) {
        throw new RuntimeException("Not implemented mock");
      }

      @Override
      public PathElement[] getRange(int start, int end, int type, float x, float y) {
        throw new RuntimeException("Not implemented mock");
      }
    };
  }

  @Override
  public TextLayout getLayout() {
    throw new RuntimeException("Not implemented mock");
  }

  @Override
  public void disposeLayout(TextLayout layout) {
    throw new RuntimeException("Not implemented mock");
  }
}
