package com.almworks.util.components;

import com.almworks.util.commons.Procedure2;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.TextGraphicsUtil;
import org.almworks.util.Log;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.util.regex.Pattern;

/**
 * @author stalex
 */
public interface Highlightable {
  Highlighter.HighlightPainter HIGHLIGH_PAINTER =
    new DefaultHighlighter.DefaultHighlightPainter(GlobalColors.HIGHLIGHT_COLOR);
  DefaultHighlighter.DefaultHighlightPainter SELECTION_PAINTER =
      new DefaultHighlighter.DefaultHighlightPainter(null);

  void setHighlightPattern(Pattern pattern);

  class HighlightUtil {
    public static void changeHighlighterPattern(Highlighter highlighter, String text, Pattern pattern) {
      highlighter.removeAllHighlights();
      addHighlighterByPattern(highlighter, text, pattern);
    }

    public static void addHighlighterByPattern(final Highlighter highlighter, String text, Pattern pattern) {
      if (pattern != null && pattern.pattern().length() > 0 && text != null && text.length() > 0) {
        TextGraphicsUtil.foreachPatternMatch(text, pattern, new Procedure2<Integer, Integer>() {
          public void invoke(Integer start, Integer end) {
            try {
              highlighter.addHighlight(start, end, HIGHLIGH_PAINTER);
            } catch (BadLocationException e) {
              Log.debug(e);
            }
          }
        });
      }
    }

    public static void installHighlighting(JTextComponent field) {
      Highlighter highlighter = new DefaultHighlighter();
      field.setHighlighter(highlighter);
    }
  }
}
