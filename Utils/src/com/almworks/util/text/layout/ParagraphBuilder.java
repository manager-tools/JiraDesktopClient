package com.almworks.util.text.layout;

import java.util.ArrayList;

/**
 * @author : Dyoma
 */
public class ParagraphBuilder {
  private final int myMaxWidth;
  private final ArrayList<String> myLines = new ArrayList<String>();
  private final StringBuffer myCurrentLine = new StringBuffer();
  private int myLineWidth = 0;
  private int myLongestLine = 0;
  private int myLastWordEnd = 0;
  private boolean myHasWord = false;

  public ParagraphBuilder(double maxWidth) {
    myMaxWidth = (int)maxWidth;
  }

  public void addWord(String word, int width) {
    if (word.trim().length() == 0) {
      addSpace(word, width);
      return;
    }
    if (myLineWidth + width > myMaxWidth && myHasWord) flushLine();
    appendString(word, width);
    myHasWord = true;
    myLastWordEnd = myLineWidth;
  }

  private void appendString(String word, int width) {
    myLineWidth += width;
    myCurrentLine.append(word);
  }

  private void addSpace(String space, int width) {
    appendString(space, width);
  }

  public void finishBuilding() {
    flushLine();
  }

  public ArrayList<String> getLines() {
    return myLines;
  }

  private void flushLine() {
    myLines.add(myCurrentLine.toString());
    myCurrentLine.delete(0, myCurrentLine.length());
    myLongestLine = Math.max(myLongestLine, myLastWordEnd);
    myLineWidth = 0;
    myLastWordEnd = 0;
  }

  public int getLongestLine() {
    return myLongestLine;
  }
}
