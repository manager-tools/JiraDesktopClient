package com.almworks.util.text.parser;

import com.almworks.util.collections.IntArray;
import org.almworks.util.Collections15;

import java.util.Map;

/**
 * @author : Dyoma
 */
class Tokenize {
  public static final int OPEN_BRACE = 0;
  public static final int CLOSE_BRACE = 1;
  private final String myText;
  private final char[] myChars;
  private final IntArray myTokens = new IntArray();
  private final Map<String, Integer> myTokenIds = Collections15.hashMap();
  private int myNextTokenId = CLOSE_BRACE + 1;
  private int myIndex;
  private int mySymbolStart = -1;

  public Tokenize(String text) {
    myText = text;
    myChars = myText.toCharArray();
    myTokenIds.put("(", OPEN_BRACE);
    myTokenIds.put(")", CLOSE_BRACE);
  }

  public void doTokenize() throws ParseException {
    for (myIndex = 0; myIndex < myChars.length; myIndex++) {
      if (isWhitespace(myIndex)) {
        finishSymbol();
        continue;
      }
      if (atBrace('(', OPEN_BRACE))
        continue;
      if (atBrace(')', CLOSE_BRACE)) {
        finishSymbol();
        continue;
      }
      if (isLetterOrDigit()) {
        finishSymbol();
        int start = myIndex;
        skipLettersAndDigits();
        addToken(start);
        continue;
      }
      if (myChars[myIndex] == '"') {
        finishSymbol();
        addToken(skipQuoted());
        continue;
      }
      if (mySymbolStart == -1)
        mySymbolStart = myIndex;
    }
    finishSymbol();
  }

  private String skipQuoted() throws ParseException {
    int start = myIndex;
    myIndex++;
    StringBuffer buffer = new StringBuffer();
    while (myIndex < myChars.length) {
      char aChar = myChars[myIndex];
      if (aChar == '"')
        return buffer.toString();
      if (aChar == '\\') {
        myIndex++;
        if (myIndex == myChars.length)
          break;
        aChar = nextEscaped();
      }
      buffer.append(aChar);
      myIndex++;
    }
    throw new ParseException("Unterminated string", start, myIndex - 1, new String(myChars));
  }

  private char nextEscaped() throws ParseException {
    assert myIndex < myChars.length;
    char aChar = myChars[myIndex];
    switch (aChar) {
    case '\\': return '\\';
    case '"': return '"';
      default: throw new ParseException("Wrong escape sequence", myIndex - 1, myIndex, new String(myChars));
    }
  }

  private void addToken(int start) {
    assert start <= myIndex;
    assert myIndex < myChars.length : getStringState();
    addToken(myText.substring(start, myIndex + 1));
  }

  private String getStringState() {
    return "index: " + myIndex + " length: " + myChars.length + " text: " + new String(myChars);
  }

  private void addToken(String token) {
    Integer id = myTokenIds.get(token);
    if (id == null) {
      id = myNextTokenId;
      myNextTokenId++;
      myTokenIds.put(token, id);
    }
    myTokens.add(id.intValue());
  }

  private void finishSymbol() {
    if (mySymbolStart == -1)
      return;
    myIndex--;
    addToken(mySymbolStart);
    myIndex++;
    mySymbolStart = -1;
  }

  private boolean isLetterOrDigit() {
    char aChar = myChars[myIndex];
    return Character.isLetter(aChar) || Character.isDigit(aChar);
  }

  private void skipLettersAndDigits() {
    while (myIndex < myChars.length) {
      if (!isLetterOrDigit()) {
        myIndex--;
        return;
      }
      myIndex++;
    }
    myIndex--;
  }

  private boolean atBrace(char brace, int tokenId) {
    if (myChars[myIndex] != brace)
      return false;
    finishSymbol();
    myTokens.add(tokenId);
    return true;
  }

  private boolean isWhitespace(int position) {
    return Character.isWhitespace(myChars[position]);
  }

  public int[] getSequence() {
    return myTokens.toNativeArray();
  }

  public Map<String, Integer> getTokenIds() {
    return myTokenIds;
  }

}
