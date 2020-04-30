package com.almworks.util.io;

/**
 * :todoc:
 *
 * @author sereda
 */
public abstract class CharValidator {
  public abstract boolean isValid(char c);

  public static final CharValidator ALL_VALID = new CharValidator() {
    public boolean isValid(char c) {
      return true;
    }
  };
}
