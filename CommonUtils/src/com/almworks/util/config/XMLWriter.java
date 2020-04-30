package com.almworks.util.config;

import com.sun.org.apache.xerces.internal.util.XMLChar;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;
import org.jdom.Verifier;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public final class XMLWriter {
  private static final String CHARSET_NAME = "UTF-8";
  private static final byte[] INDENT;
  private static final byte[] OPEN_BRACE;
  private static final byte[] CLOSE_BRACE;
  private static final byte[] CLOSE_MARK;
  private static final byte[] SPACE;
  private static final byte[] LT;
  private static final byte[] GT;
  private static final byte[] AMP;
  private static final byte[] xD;
  private static final byte[] xPREFIX;
  private static final byte[] SEMICOLON;

  static {
    try {
      INDENT = "  ".getBytes(CHARSET_NAME);
      OPEN_BRACE = "<".getBytes(CHARSET_NAME);
      CLOSE_BRACE = ">".getBytes(CHARSET_NAME);
      CLOSE_MARK = "/".getBytes(CHARSET_NAME);
      SPACE = " ".getBytes(CHARSET_NAME);
      LT = "&lt;".getBytes(CHARSET_NAME);
      GT = "&gt;".getBytes(CHARSET_NAME);
      AMP = "&amp;".getBytes(CHARSET_NAME);
      xD = "&#xD;".getBytes(CHARSET_NAME);
      xPREFIX = "&#x".getBytes(CHARSET_NAME);
      SEMICOLON = ";".getBytes(CHARSET_NAME);
    } catch (UnsupportedEncodingException e) {
      throw new Failure(e);
    }
  }

  private static final char[] NO_CHARS = new char[0];
  private final OutputStream myStream;
  private final Map<String, byte[]> myEncodedNames = Collections15.hashMap();
  private final List<byte[]> myTags = Collections15.arrayList();
  private boolean myLineStart = false;
  static final char PREFIX_CHAR = '_';
  private static final String PREFIX_CHAR_CODE = Integer.toHexString(PREFIX_CHAR);
  private static final char ENCODED_CHAR = '_';
  private static final char LONG_CODE_PREFIX = 'L';

  public XMLWriter(OutputStream stream) {
    myStream = stream;
  }

  public void openTag(String tagName) throws IOException {
    handleLineStart();
    myStream.write(OPEN_BRACE);
    byte[] encodedTagName = getEncodedTagName(tagName);
    myStream.write(encodedTagName);
    myStream.write(CLOSE_BRACE);
    myTags.add(encodedTagName);
  }

  public void closeTag() throws IOException {
    byte[] lastTag = myTags.remove(myTags.size() - 1);
    handleLineStart();
    myStream.write(OPEN_BRACE);
    myStream.write(CLOSE_MARK);
    myStream.write(lastTag);
    myStream.write(CLOSE_BRACE);
    newLine();
  }

  public void writeTag(String tagName, byte[] attributesBytes) throws IOException {
    handleLineStart();
    myStream.write(OPEN_BRACE);
    myStream.write(getEncodedTagName(tagName));
    myStream.write(SPACE);
    myStream.write(attributesBytes);
    myStream.write(CLOSE_MARK);
    myStream.write(CLOSE_BRACE);
    newLine();
  }

  public byte[] encodeString(String str) {
    try {
      return str.getBytes(CHARSET_NAME);
    } catch (UnsupportedEncodingException e) {
      throw new Failure(e);
    }
  }

  public void newLine() throws IOException {
    myStream.write('\n');
    myLineStart = true;
  }

  private void handleLineStart() throws IOException {
    if (!myLineStart)
      return;
    int offset = myTags.size();
    for (int i = 0; i < offset; i++)
      myStream.write(INDENT);
    myLineStart = false;
  }

  private char[] myTempArray = NO_CHARS;
//  private final StringBuffer myBuffer = new StringBuffer();
  public void appendText(String text) throws IOException {
    int length = text.length();
    if (length > myTempArray.length)
      myTempArray = new char[length];
    text.getChars(0, length, myTempArray, 0);
    for (int i = 0; i < length; i++) {
      char c = myTempArray[i];
      if (c < 127) {
        if (XMLChar.isInvalid(c)) c = '?'; // Replace invalid character - make the written config readable
        switch(c) {
        case '<' : myStream.write(LT); break;
        case '>' : myStream.write(GT); break;
        case '&' : myStream.write(AMP); break;
        case '\r': myStream.write(xD); break;
        default: myStream.write(c);
        }
      } else {
        myStream.write(xPREFIX);
        writeHex(c);
//        myStream.write(Integer.toHexString(c).getBytes(CHARSET_NAME));
        myStream.write(SEMICOLON);
      }
    }
  }

  private static final char HIGH_HEX_DIGIT = 0xF << 12;
  private static final byte[] HEX_DIGITS = new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  private void writeHex(char c) throws IOException {
    boolean notZeroWriten = false;
    for (int i = 0; i < 4; i++) {
      int digit = (c & HIGH_HEX_DIGIT) >> 12;
      if (notZeroWriten || digit != 0) {
        notZeroWriten = true;
        assert digit >= 0:digit;
        assert digit < 16:digit;
        myStream.write(HEX_DIGITS[digit]);
      }
      c = (char) (c << 4);
    }
    assert notZeroWriten;
  }

  /**
   * takes 25% of write time
   */
  private byte[] getEncodedTagName(String tagName) throws UnsupportedEncodingException {
    byte[] bytes = myEncodedNames.get(tagName);
    if (bytes == null) {
      bytes = encode(tagName).getBytes(CHARSET_NAME);
      myEncodedNames.put(tagName, bytes);
    }
    return bytes;
  }

  protected static String encode(String tagName) {
    StringBuffer result = new StringBuffer();
    char firstChar = tagName.charAt(0);
    if (Verifier.isXMLNameCharacter(firstChar)) {
      if (!Verifier.isXMLNameStartCharacter(firstChar)) {
        result.append(PREFIX_CHAR);
        result.append(firstChar);
      } else if (firstChar == PREFIX_CHAR) {
        result.append(PREFIX_CHAR);
        result.append(PREFIX_CHAR);
        result.append(PREFIX_CHAR_CODE);
      } else
        result.append(firstChar);
    } else {
      result.append(PREFIX_CHAR);
      appendEncoded(result, firstChar);
    }
    for (int i = 1; i < tagName.length(); i++) {
      char c = tagName.charAt(i);
      appendEncoded(result, c);
    }
    return result.toString();
  }

  static void appendEncoded(StringBuffer result, char aChar) {
    if (aChar == ENCODED_CHAR) {
      result.append(aChar);
      result.append(aChar);
    } else if (aChar != ':' && Verifier.isXMLNameCharacter(aChar)) {
      result.append(aChar);
    } else {
      result.append(ENCODED_CHAR);
      int charCode = (int) aChar;
      if (charCode < 256) {
        String encoded;
        encoded = Integer.toHexString(charCode);
        if (encoded.length() == 1)
          result.append('0');
        else
          assert encoded.length() == 2;
        result.append(encoded);
      } else {
        result.append(LONG_CODE_PREFIX);
        String encoded = Integer.toHexString(charCode);
        assert encoded.length() >= 3;
        if (encoded.length() == 3)
          result.append('0');
        else
          assert encoded.length() == 4;
        result.append(encoded);
      }
    }
  }

  public static String decode(String elementName) {
    if (elementName.charAt(0) == PREFIX_CHAR)
      elementName = elementName.substring(1, elementName.length());
    StringBuffer decoded = new StringBuffer();
    int i = 0;
    while (i < elementName.length() - 1) {
      char c = elementName.charAt(i);
      if (c != ENCODED_CHAR) {
        decoded.append(c);
        i++;
        continue;
      }
      if (elementName.charAt(i + 1) == ENCODED_CHAR) {
        decoded.append(c);
        i += 2;
        continue;
      }
      try {
        char firstDigit = elementName.charAt(i + 1);
        char encodedChar;
        if (firstDigit != LONG_CODE_PREFIX) {
          encodedChar = (char) Integer.parseInt(elementName.substring(i + 1, i + 3), 16);
          i += 3;
        } else {
          encodedChar = (char) Integer.parseInt(elementName.substring(i + 2, i + 6), 16);
          i += 6;
        }
        decoded.append(encodedChar);
      } catch (NumberFormatException e) {
        decoded.append(c);
        i++;
      }
    }
    if (i == elementName.length() - 1) {
      decoded.append(elementName.charAt(i));
    }
    return decoded.toString();
  }
}
