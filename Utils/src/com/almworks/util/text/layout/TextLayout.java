package com.almworks.util.text.layout;

import com.almworks.util.collections.ConvertingList;
import com.almworks.util.collections.FlattenList;
import com.almworks.util.text.LineTokenizer;
import org.almworks.util.Util;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author : Dyoma
 */
public class TextLayout {
  private String myText = "";
  private List<ParagraphImpl> myParagraphs = null;
  private final FontMetrics myFontMetrics;

  public TextLayout(FontMetrics fontMetrics) {
    myFontMetrics = fontMetrics;
  }

  public void setText(String text) {
    if (Util.equals(text, myText)) return;
    myText = text != null ? text : "";
    myParagraphs = null;
  }

  public List<String> getLines() {
    return getTextLines();
  }

  public List<String> getTextLines() {
    return FlattenList.create(ConvertingList.create(getParagraphs(), Paragraph.TEXT_LINES));
  }

  public int getLineCount() {
    return getTextLines().size();
  }

  public int paintText(double x, double y, Graphics2D g) {
    FontMetrics metrics = g.getFontMetrics();
    double baseLine = y + metrics.getAscent();
    List<String> lines = getLines();
    for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
      String line = iterator.next();
      g.drawString(line, (float) x, (float) baseLine);
      baseLine += metrics.getHeight();
    }
    return metrics.getHeight() * lines.size();
  }

  public double updatePixelWidth(double width) {
    double maxPixelWidth = 0;
    for (Iterator<ParagraphImpl> iterator = getParagraphsImpl().iterator(); iterator.hasNext();) {
      ParagraphImpl paragraph = iterator.next();
      double updatedWidth = paragraph.updatePixelWidth(width);
      maxPixelWidth = Math.max(updatedWidth, maxPixelWidth);
    }
    return maxPixelWidth;
  }

  public double getPixelWidth() {
    List<? extends Paragraph> paragraphs = getParagraphs();
    double result = 0;
    for (Iterator<? extends Paragraph> iterator = paragraphs.iterator(); iterator.hasNext();) {
      Paragraph paragraph = iterator.next();
      result = Math.max(result, paragraph.getPixelWidth());
    }
    return result;
  }

  public List<Paragraph> getParagraphs() {
    return Collections.<Paragraph>unmodifiableList(getParagraphsImpl());
  }

  private List<ParagraphImpl> getParagraphsImpl() {
    if (myParagraphs == null) {
      myParagraphs = new ArrayList<ParagraphImpl>();
      LineTokenizer tokenizer = new LineTokenizer(myText);
      while (tokenizer.hasNext()) {
        String paragraph = tokenizer.next();
        myParagraphs.add(new ParagraphImpl(paragraph, getFontMetrics()));
      }
    }
    return myParagraphs;
  }

  public FontMetrics getFontMetrics() {
    return myFontMetrics;
  }
}
