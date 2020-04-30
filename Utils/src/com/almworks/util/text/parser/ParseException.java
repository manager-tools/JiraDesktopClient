package com.almworks.util.text.parser;

/**
 * @author : Dyoma
 */
public class ParseException extends Exception {
  private final String myText;
  private final int myStart;
  private final int myEnd;

  public ParseException(String message, int start, int end, String text) {
    super(message + " (" + text +") at " + start + "-" + end);
    myText = text;
    myStart = start;
    myEnd = end;
  }

  public ParseException(String message) {
    super(message);
    myText = null;
    myStart = -1;
    myEnd = -1;
  }

  public static ParseException semanticError(String message) {
    return new ParseException(message);
  }
}
