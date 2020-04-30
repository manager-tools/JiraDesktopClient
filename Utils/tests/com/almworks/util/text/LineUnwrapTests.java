package com.almworks.util.text;

import com.almworks.util.tests.BaseTestCase;

/**
 * @author : Dyoma
 */
public class LineUnwrapTests extends BaseTestCase {
  public void testSingleLine() {
    String text = "asdf asdf asd f as f asdfasdf";
    assertEquals(text, TextUtil.unwrapLines(text));
  }

  public void testTwoLineSeparator() {
    String text =
      "abc def 1213 456\n" +
      "\n" +
      "def abc 456 1213";
    assertEquals(text, TextUtil.unwrapLines(text));
  }

  public void testSkipShortParagraphs() {
    String text =
      "a b c\n" +
      "1234\n" +
      "xx dd 1234 5";
    assertEquals(text, TextUtil.unwrapLines(text));
  }

  public void testUnwrapping() {
    String text =
      "abc def 123 456\n" +
      "xyz\n" +
      "\n" +
      "abc def 123 456";
    assertEquals(
      "abc def 123 456 xyz\n" +
        "\n" +
        "abc def 123 456", TextUtil.unwrapLines(text));
  }

  public void testKeepingParagraphBreaks() {
    String text =
      "abc def 123 456\n" +
      "xyz\n" +
      "abc def 123 456\n" +
      "qqq";
    assertEquals("abc def 123 456 xyz\n" +
                   "abc def 123 456 qqq", TextUtil.unwrapLines(text));
  }

  public void testUnwrapSeveralLines() {
    String text =
      "abc def 123 456\n" +
      "xyz 09876 543\n" +
      "qqqq\n" +
      "abc def 123 456\n" +
      "qqq";
    assertEquals("abc def 123 456 xyz 09876 543 qqqq\n" +
                   "abc def 123 456 qqq", TextUtil.unwrapLines(text));
  }

  public void testSkipStartingWithSpace() {
    String text =
      "abc def 1234 098\n" +
      " xxx yyy zz q\n" +
      "1\n" +
      "aaa";
    assertEquals(text, TextUtil.unwrapLines(text));
    text =
      "abc def 1234 098\n" +
      " xxx yyy zz q\n" +
      "11\n" +
      "aaa";
    assertEquals("abc def 1234 098\n" +
                   " xxx yyy zz q 11\n" +
                   "aaa", TextUtil.unwrapLines(text));
    text =
      "abc def 1234 098\n" +
      " xxx yyy zz q\n" +
      "1.\n" +
      "aaa";
    assertEquals(text, TextUtil.unwrapLines(text));
  }
}
