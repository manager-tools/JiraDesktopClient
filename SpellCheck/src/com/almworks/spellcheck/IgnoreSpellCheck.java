package com.almworks.spellcheck;

public interface IgnoreSpellCheck {
  /**
   * Check is the word should be ignored.
   * @param text the whole text being checked
   * @param offset offset of the first letter of the word
   * @param length length of the word
   * @return true if the word spelling should be ignored
   */
  boolean shouldIgnore(String text, int offset, int length);
}
