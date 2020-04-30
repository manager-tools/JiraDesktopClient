package com.almworks.util.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author : Dyoma
 */
public class LineTokenizer implements Iterator<String> {
  private final String myText;
  private boolean myIncludeLineSeparators = false;
  private int myPosition = 0;

  public LineTokenizer(String text) {
    if (text == null) throw new NullPointerException("text");
    myText = text;
  }

  public boolean hasMoreLines() {
    return myText.length() > myPosition;
  }

  public String nextLine() {
    int length = 0;
    int newPosition;
    for (newPosition = myPosition; newPosition < myText.length(); newPosition++) {
      char aChar = myText.charAt(newPosition);
      if (aChar == '\n') {
        length = addLineSeparator(length);
        break;
      }
      if (aChar == '\r') {
        length = addLineSeparator(length);
        if (newPosition == myText.length() - 1) break;
        if (myText.charAt(newPosition + 1) == '\n') {
          newPosition++;
          length = addLineSeparator(length);
        }
        break;
      }
      length++;
    }
    String result = myText.substring(myPosition, myPosition + length);
    myPosition = newPosition + 1;
    return result;
  }

  public void setIncludeLineSeparators(boolean includeLineSeparators) {
    myIncludeLineSeparators = includeLineSeparators;
  }

  private int addLineSeparator(int length) {
    if (myIncludeLineSeparators) length++;
    return length;
  }

  public boolean hasNext() {
    return hasMoreLines();
  }

  public String next() {
    return nextLine();
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  public static List<String> getLines(String s) {
    ArrayList<String> result = new ArrayList<String>();
    LineTokenizer tokenizer = new LineTokenizer(s);
    tokenizer.setIncludeLineSeparators(false);
    while (tokenizer.hasNext()) {
      String line = tokenizer.next();
      result.add(line);
    }
    return result;
  }

  public static void prependLines(String text, String prefix, StringBuffer buffer) {
    List<String> lines = LineTokenizer.getLines(text);
    for (String line : lines) {
      buffer.append(prefix);
      buffer.append(line);
      buffer.append("\n");
    }
  }

  public static String prependLines(String text, String prefix) {
    StringBuffer buffer = new StringBuffer();
    prependLines(text, prefix, buffer);
    return buffer.toString();
  }

  public static String replaceLineSeparators(String text, String separator) {
    StringBuilder builder = new StringBuilder();
    LineTokenizer tokenizer = new LineTokenizer(text);
    tokenizer.setIncludeLineSeparators(false);
    boolean first = true;
    while (tokenizer.hasNext()) {
      String line = tokenizer.next();
      if (!first) builder.append(separator);
      first = false;
      builder.append(line);
    }
    return builder.toString();
  }

  public static String firstLine(String text) {
    LineTokenizer tokenizer = new LineTokenizer(text);
    tokenizer.setIncludeLineSeparators(false);
    if (!tokenizer.hasNext()) return "";
    return tokenizer.next();
  }
}
