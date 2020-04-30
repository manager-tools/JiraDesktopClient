package com.almworks.util.components;

import com.almworks.util.ui.ComponentProperty;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
public interface Canvas extends CanvasRenderable {
  ComponentProperty<Pattern> PATTERN_PROPERTY = ComponentProperty.createProperty("highlight.pattern");

  void setIcon(Icon icon);

  void setToolTipText(String s);

  void setEnabled(boolean enabled);

  void setBackground(Color bg);

  void setForeground(Color fg);

  void setCanvasBorder(@Nullable Border border);

  @Nullable
  Border getCanvasBorder();

  void appendText(String text);

  void copyTo(Canvas canvas);

  void setFontStyle(int style);

  void clear();

  void setFullyOpaque(boolean opaque);

  CanvasSection newSection();

  CanvasSection emptySection();

  CanvasSection getCurrentSection();

  Line newLine();

  void setIconMargin(Insets margin);

  void appendInt(int value);

  void appendLong(long value);

  void copyAttributes(Canvas canvas);

  void setCanvasBackground(@Nullable Color background);

  @Nullable
  Color getCanvasBackground();

  Line getCurrentLine();

  interface Line {
    void appendText(String text);

    CanvasSection newSection();

    CanvasSection getCurrentSection();

    CanvasSection[] getSections();

    void setHorizontalAlignment(float alignment);
  }
}
