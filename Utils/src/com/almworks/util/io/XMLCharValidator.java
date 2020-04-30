package com.almworks.util.io;

/**
 * :todoc:
 *
 * @author sereda
 */
public final class XMLCharValidator extends CharValidator {
  public static final XMLCharValidator INSTANCE = new XMLCharValidator();

  public final boolean isValid(char c) {
    if (c >= 0x20) {
      if (c <= 0xD7FF)
        return true; // this is the most common and fastest case
      if (c < 0xE000)
        return false;
      if (c <= 0xFFFD)
        return true;
      if (c < 0x10000)
        return false;
      if (c <= 0x10FFFF)
        return true;
      return false;
    } else {
      if (c == 0x9 || c == 0xA || c == 0xD)
        return true;
      return false;
    }
  }
}
