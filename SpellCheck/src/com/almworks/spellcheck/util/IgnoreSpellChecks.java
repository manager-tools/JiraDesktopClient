package com.almworks.spellcheck.util;

import com.almworks.spellcheck.IgnoreSpellCheck;

public class IgnoreSpellChecks {
  public static final IgnoreSpellCheck ALL_CAPITALIZE = new IgnoreSpellCheck() {
    @Override
    public boolean shouldIgnore(String text, int offset, int length) {
      for (int i = 0; i < length; i++) {
        char c = text.charAt(offset + i);
        if (!Character.isUpperCase(c)) return false;
      }
      return true;
    }
  };

  public static final IgnoreSpellCheck HUMAN_NAME = new IgnoreSpellCheck() {
    @Override
    public boolean shouldIgnore(String text, int offset, int length) {
      if (!Character.isUpperCase(text.charAt(offset))) return false;
      int pos = offset - 1;
      boolean hasSpace = false;
      while (pos >= 0) {
        char c = text.charAt(pos);
        if (c == '.') return false;
        if (Character.isWhitespace(c)) hasSpace = true;
        else return hasSpace;
        pos--;
      }
      return false;
    }
  };
}
