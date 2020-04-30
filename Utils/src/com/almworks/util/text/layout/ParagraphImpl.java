package com.almworks.util.text.layout;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author : Dyoma
 */
class ParagraphImpl implements Paragraph {
  private final String myText;
  private FontMetrics myFontMetrics;
  private List<String> myLines = null;
  private double myWidth = -1;

  public ParagraphImpl(String text, FontMetrics fontMetrics) {
    if (fontMetrics == null)
      throw new NullPointerException("fontMetrics");
    if (text == null)
      throw new NullPointerException("text");
    myText = text;
    myFontMetrics = fontMetrics;
  }

  public double updatePixelWidth(double width) {
    buildLines(width);
    return getPixelWidth();
  }

  public double getPixelWidth() {
    if (myWidth < 0)
      buildLines(-1);
    return myWidth;
  }

  public List<String> getLines() {
    if (myLines == null)
      buildLines(myWidth);
    return myLines;
  }

  private void buildLines(double maxWidth) {
    FontMetrics metrics = getFontMetrics();
    if (maxWidth <= 0) {
      myLines = Collections.singletonList(myText);
      myWidth = metrics.stringWidth(myText);
    } else {
      ParagraphBuilder builder = new ParagraphBuilder(maxWidth);
      StringTokenizer tokenizer = new StringTokenizer(myText, " \t\n\r\f", true);
      while (tokenizer.hasMoreTokens()) {
        String word = tokenizer.nextToken();
        builder.addWord(word, metrics.stringWidth(word));
      }
      builder.finishBuilding();
      myLines = Collections.unmodifiableList(builder.getLines());
      myWidth = builder.getLongestLine();
    }
  }

  private FontMetrics getFontMetrics() {
    return myFontMetrics;
  }

  public void setFontMetrics(FontMetrics fontMetrics) {
    myFontMetrics = fontMetrics;
    updatePixelWidth(myWidth);
  }
}
