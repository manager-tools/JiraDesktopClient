package com.almworks.util.ui;

import com.almworks.util.commons.Function2;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.components.renderer.CellState;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextGraphicsUtil {
  public static void drawMatchedTextHighlight(final Graphics g, final int x, final Pattern pattern,
    final FontMetrics fontMetrics, final String text, final int y)
  {
    if (pattern != null && pattern.pattern().length() != 0 && text != null && text.length() > 0) {
      Color oldColor = g.getColor();
      g.setColor(GlobalColors.HIGHLIGHT_COLOR);
      // todo optimize
      foreachPatternMatch(text, pattern, new Procedure2<Integer, Integer>() {
        public void invoke(Integer start, Integer end) {
          g.fillRect(x + fontMetrics.stringWidth(text.substring(0, start)), y,
            fontMetrics.stringWidth(text.substring(start, end)), fontMetrics.getHeight());
        }
      });
      g.setColor(oldColor);
    }
  }

  public static void foreachPatternMatch2(String text, Pattern pattern, Function2<String, Boolean, String> everySubsr) {
    String preprocessedText = preprocessText(text);
    Matcher m = pattern.matcher(preprocessedText);
    int start = 0;
    int end = 0;
    while (m.find(start)) {

      end = m.end(0);
      int startIndex = m.start(0);

      if (startIndex != start) {
        everySubsr.invoke(text.substring(start, startIndex), false);
      }

      everySubsr.invoke(text.substring(startIndex, end), true);

      if (start == end) {
        break;
      }
      start = end;
    }
    if (end != text.length()) {
      everySubsr.invoke(text.substring(end, text.length()), false);
    }
  }

  private static String preprocessText(String text) {
    return text.replace('\u2212', '-');
  }

  public static void foreachPatternMatch(String text, Pattern pattern, Procedure2<Integer, Integer> matchProc) {
    String preprocessedText = preprocessText(text);
    Matcher m = pattern.matcher(preprocessedText);
    int start = 0;
    while (m.find(start)) {
      int end = m.end(0);
      int startIndex = m.start(0);
      matchProc.invoke(startIndex, end);
      if (start == end) {
        break;
      }
      start = end;
    }
  }

  public static void renderHighlightedStateOn(final CellState state, final com.almworks.util.components.Canvas canvas,
    Pattern pattern, String text)
  {
    TextGraphicsUtil.foreachPatternMatch2(text, pattern, new Function2<String, Boolean, String>() {
      public String invoke(String s, Boolean matched) {
        canvas.newSection().setBackground(matched ? GlobalColors.HIGHLIGHT_COLOR : state.getBackground());
        canvas.appendText(s);
        return null;
      }
    });
    canvas.newSection().setBackground(state.getBackground());
  }
}
