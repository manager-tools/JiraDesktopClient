package com.almworks.util.components;

import com.almworks.util.components.renderer.CellState;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public interface CanvasRenderable {
  void renderOn(Canvas canvas, CellState state);

  CanvasRenderable EMPTY = new CanvasRenderable() {
    public void renderOn(Canvas canvas, CellState state) {
    }
  };

  abstract class TextWithIcon implements CanvasRenderable {
    private Icon myOpenIcon;
    private Icon myClosedIcon;

    public TextWithIcon(Icon openIcon, Icon closedIcon) {
      myOpenIcon = openIcon;
      myClosedIcon = closedIcon;
    }

    public void renderOn(Canvas canvas, CellState state) {
      canvas.setIcon(state.isExpanded() ? myOpenIcon : myClosedIcon);
      canvas.appendText(getText());
    }

    public boolean setIcon(Icon open, Icon closed) {
      if (Util.equals(myOpenIcon, open) && Util.equals(myClosedIcon, closed))
        return false;
      myOpenIcon = open;
      myClosedIcon = closed;
      return true;
    }

    public Icon getOpenIcon() {
      return myOpenIcon;
    }

    public boolean setIcon(Icon icon) {
      return setIcon(icon, icon);
    }

    @NotNull
    public abstract String getText();
  }

  class FixedText extends TextWithIcon {
    private final String myText;

    public FixedText(String text, Icon openIcon, Icon closedIcon) {
      super(openIcon, closedIcon);
      myText = text;
    }

    public FixedText(String text, Icon icon) {
      this(text, icon, icon);
    }

    public FixedText(String text) {
      this(text, null, null);
    }

    public String getText() {
      return myText;
    }

    public static CanvasRenderable folder(String text, Icon openIcon, Icon closedIcon) {
      return new FixedText(text, openIcon, closedIcon);
    }
  }


  class TextRenderable implements CanvasRenderable {
    private final String myText;
    private final int myFontStyle;

    public TextRenderable(String text) {
      this(Font.PLAIN, text);
    }

    public TextRenderable(int fontStyle, String text) {
      myFontStyle = fontStyle;
      myText = text;
    }

    public void renderOn(Canvas canvas, CellState state) {
      CanvasSection section = canvas.getCurrentSection();
      if (!section.getText().isEmpty()) section = canvas.emptySection();
      section.setFontStyle(myFontStyle);
      section.appendText(myText);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      TextRenderable other = Util.castNullable(TextRenderable.class, obj);
      return other != null && Util.equals(myText, other.myText) && myFontStyle == other.myFontStyle;
    }

    @Override
    public int hashCode() {
      return Util.hashCode(myText) ^ myFontStyle ^ TextRenderable.class.hashCode();
    }
  }
}
