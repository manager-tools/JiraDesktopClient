package com.almworks.util.components.renderer;

import com.almworks.util.components.Canvas;
import com.almworks.util.ui.TextGraphicsUtil;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
public interface FontStyle {
  FontStyle PLAIN = new Modifier(Font.PLAIN);
  FontStyle BOLD = new Modifier(Font.BOLD);

  int getHeight(RendererContext context);

  int getStringWidth(RendererContext context, String text);

  void paint(Graphics g, int x, int y, RendererContext context, String text);

  int getAscent(RendererContext context);

  class Modifier implements FontStyle {
    private final int myStyle;

    public Modifier(int style) {
      myStyle = style;
    }

    public int getHeight(RendererContext context) {
      return context.getFontMetrics(myStyle).getHeight();
    }

    public int getStringWidth(RendererContext context, String text) {
      return context.getFontMetrics(myStyle).stringWidth(Util.NN(text));
    }

    public void paint(Graphics g, int x, int y, RendererContext context, String text) {
      Font font = context.getFont(myStyle);
      g.setFont(font);
      Pattern pattern = context.getValue(Canvas.PATTERN_PROPERTY);
      if (pattern != null && pattern.pattern() != "") {
        TextGraphicsUtil.drawMatchedTextHighlight(g, x, pattern, g.getFontMetrics(), text, y);
      }
      y += getAscent(context);
      g.drawString(text, x, y);
    }

    public int getAscent(RendererContext context) {
      return context.getFontBaseLine(myStyle);
    }
  }


  abstract class Decorator implements FontStyle {
    private final FontStyle myBase;

    public Decorator(FontStyle base) {
      myBase = base;
    }

    public int getHeight(RendererContext context) {
      return myBase.getHeight(context);
    }

    public int getStringWidth(RendererContext context, String text) {
      return myBase.getStringWidth(context, text);
    }

    public int getAscent(RendererContext context) {
      return myBase.getAscent(context);
    }

    protected FontStyle getBase() {
      return myBase;
    }

    public static FontStyle underline(FontStyle base) {
      return new Decorator(base) {
        public void paint(Graphics g, int x, int y, RendererContext context, String text) {
          getBase().paint(g, x, y, context, text);
          y += getAscent(context) + 1;
          int width = getStringWidth(context, text);
          g.drawLine(x, y, x + width, y);
        }
      };
    }

    public static FontStyle colored(FontStyle base, final Color color) {
      return new Decorator(base) {
        public void paint(Graphics g, int x, int y, RendererContext context, String text) {
          Color savedColor = g.getColor();
          g.setColor(color);
          getBase().paint(g, x, y, context, text);
          g.setColor(savedColor);
        }
      };
    }

    public static FontStyle coloredByName(FontStyle base, final String colorName) {
      return new Decorator(base) {
        public void paint(Graphics g, int x, int y, RendererContext context, String text) {
          Color savedColor = g.getColor();
          g.setColor(UIManager.getColor(colorName));
          getBase().paint(g, x, y, context, text);
          g.setColor(savedColor);
        }
      };
    }
  }
}
