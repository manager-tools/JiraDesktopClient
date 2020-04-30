package com.almworks.util.text.html;

import com.almworks.util.text.CharIterator;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;

import java.util.HashMap;
import java.util.Set;

/**
 * @author dyoma
 */
public class HtmlTextIterator implements CharIterator {
  private final char[] myHtml;
  private int myPos = 0;
  private char[] myPreparsed = null;
  private int myPreparsedPos = 0;
  private int myPreparsedLength = 0;
  private static final char[] COMMENT_START = "<!--".toCharArray();
  private static final char[] COMMENT_END = "-->".toCharArray();
  private static final HashMap<String, char[]> SYMBOLS = Collections15.hashMap();
  private static final Set<String> INLINE_TAGS =
    Collections15.set("a", "b", "cite", "code", "em", "font", "i", "s", "small", "span", "strike", "strong", "u");

  public HtmlTextIterator(String html) {
    myHtml = html.toCharArray();
    skipToNext();
  }

  public char nextChar() {
    if (myPreparsed != null && myPreparsedPos < myPreparsedLength) {
      char result = myPreparsed[myPreparsedPos];
      myPreparsedPos++;
      if (myPreparsedPos == myPreparsedLength) {
        myPreparsedLength = 0;
        myPreparsedPos = 0;
        skipToNext();
      }
      return result;
    }
    assert false;
    return ' ';
  }

  private void skipToNext() {
    boolean spaceSkipped = false;
    while (hasMoreHtml()) {
      char c = myHtml[myPos];
      if (isSpace(c)) {
        if (!spaceSkipped)
          addPreparsed(' ');
        spaceSkipped = true;
        myPos++;
        continue;
      } else if (spaceSkipped) {
        break;
      }
      if (c == '&') {
        addEntityRef();
        break;
      }
      if (isNextChars(COMMENT_START)) {
        skipComment();
        continue;
      } else if (c == '<') {
        skipTag();
        continue;
      }
      addPreparsed(c);
      myPos++;
      break;
    }
  }

  private void skipTag() {
    assert myHtml[myPos] == '<';
    char quote = 0;
    StringBuffer tagName = new StringBuffer();
    while (hasMoreHtml(1)) {
      myPos++;
      char c = myHtml[myPos];
      if (quote != 0) {
        if (quote == c)
          quote = 0;
        continue;
      }
      if (c == '\'' || c == '"') {
        quote = c;
        continue;
      }
      if (tagName != null) {
        if (Character.isLetterOrDigit(c)) {
          tagName.append(c);
        } else {
          String tag = tagName.toString();
          tagName = null;
          if (tag.length() > 0 && !INLINE_TAGS.contains(Util.lower(tag))) {
            addPreparsed(' ');
          }
        }
      }
      if (c == '>')
        break;
    }
    myPos++;
  }

  private boolean isNextChars(char[] str) {
    if (!hasMoreHtml(str.length + 1))
      return false;
    for (int i = 0; i < str.length; i++)
      if (myHtml[myPos + i] != str[i])
        return false;
    return true;
  }

  private void skipComment() {
    assert isNextChars(COMMENT_START);
    myPos += COMMENT_START.length;
    while (hasMoreHtml()) {
      if (isNextChars(COMMENT_END)) {
        myPos += COMMENT_END.length;
        return;
      }
      myPos++;
    }
  }

  private boolean hasMoreHtml() {
    return hasMoreHtml(0);
  }

  private boolean hasMoreHtml(int additionalChars) {
    return myPos + additionalChars < myHtml.length;
  }

  public static boolean isSpace(char c) {
    return Character.isWhitespace(c) || Character.isSpaceChar(c);
  }

  private void addPreparsed(char c) {
    if (myPreparsed == null) {
      myPreparsed = new char[] {c};
      myPreparsedLength = 1;
      myPreparsedPos = 0;
      return;
    }
    ensurePreparsedCapasity(1);
    myPreparsed[myPreparsedLength] = c;
    myPreparsedLength++;
  }

  private void ensurePreparsedCapasity(int min) {
    if (myPreparsedLength + min > myPreparsed.length) {
      char[] chars = new char[Math.max(myPreparsedLength + min, myPreparsedLength * 2)];
      System.arraycopy(myPreparsed, 0, chars, 0, myPreparsed.length);
      myPreparsed = chars;
    }
  }

  private void addPreparsed(char[] chars) {
    if (myPreparsed == null) {
      myPreparsed = new char[chars.length];
      System.arraycopy(chars, 0, myPreparsed, 0, chars.length);
      myPreparsedLength = chars.length;
      myPreparsedPos = 0;
      return;
    }
    ensurePreparsedCapasity(chars.length);
    System.arraycopy(chars, 0, myPreparsed, myPreparsedLength, chars.length);
    myPreparsedLength += chars.length;
  }

  private void addEntityRef() {
    assert myHtml[myPos] == '&';
    int savedPos = myPos;
    myPos++;
    StringBuffer buffer = new StringBuffer();
    int pos;
    char c = 0;
    for (pos = myPos; pos < myHtml.length; pos++) {
      c = myHtml[pos];
      if (c == ';' || isSpace(c))
        break;
      buffer.append(c);
    }
    if (c == ';' && pos < myHtml.length)
      pos++;
    if (buffer.length() > 0) {
      if (buffer.charAt(0) == '#') {
        int code;
        try {
          code = Integer.parseInt(buffer.substring(1));
          char ch = (char) code;
          addPreparsed(isSpace(ch) ? ' ' : ch);
          myPos = pos;
          return;
        } catch (NumberFormatException e) {
          Log.warn("Bad unicode: " + buffer.substring(1));
        }
      } else {
        char[] sym = SYMBOLS.get(buffer.toString());
        if (sym != null) {
          addPreparsed(sym);
          myPos = pos;
          return;
        }
      }
    }
    // didn't find replacement
    addPreparsed('&');
  }

  public boolean hasNext() {
    return myPreparsedPos < myPreparsedLength || hasMoreHtml();
  }

  static {
    SYMBOLS.put("lt", "<".toCharArray());
    SYMBOLS.put("gt", ">".toCharArray());
    SYMBOLS.put("amp", "&".toCharArray());
    SYMBOLS.put("quot", "\"".toCharArray());
    SYMBOLS.put("apos", "'".toCharArray());
    SYMBOLS.put("nbsp", " ".toCharArray());
  }
}
