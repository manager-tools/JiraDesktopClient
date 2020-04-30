package com.almworks.util.text;

/**
 * @author dyoma
 */
public class StringCharIterator implements CharIterator {
  private final String myString;
  private int myPos = 0;

  public StringCharIterator(String string) {
    myString = string;
  }

  public boolean hasNext() {
    return myPos < myString.length();
  }

  public char nextChar() {
    char c = myString.charAt(myPos);
    myPos++;
    return c;
  }
}
