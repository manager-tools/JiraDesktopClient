package com.almworks.util.text;

import com.almworks.util.Env;
import com.almworks.util.tests.BaseTestCase;

import java.awt.event.KeyEvent;
import java.math.BigDecimal;

/**
 * @author : Dyoma
 */
public class TextTests extends BaseTestCase {
  public void testLineTokenizer1() {
    LineTokenizer tokenizer = new LineTokenizer("\n1\r\n2\n\n\r3");
    tokenizer.setIncludeLineSeparators(true);
    assertEquals("\n", tokenizer.nextLine());
    assertEquals("1\r\n", tokenizer.nextLine());
    assertEquals("2\n", tokenizer.nextLine());
    assertEquals("\n", tokenizer.nextLine());
    assertEquals("\r", tokenizer.nextLine());
    assertEquals("3", tokenizer.nextLine());
    assertFalse(tokenizer.hasMoreLines());
  }

  public void testLineTokenizer2() {
    LineTokenizer tokenizer = new LineTokenizer("1\n");
    tokenizer.setIncludeLineSeparators(true);
    assertTrue(tokenizer.hasMoreLines());
    assertEquals("1\n", tokenizer.nextLine());
    assertFalse(tokenizer.hasMoreLines());
  }

  public void testLineTokenizer3() {
    LineTokenizer tokenizer = new LineTokenizer("1\r\n2\n3");
    assertEquals("1", tokenizer.nextLine());
    assertEquals("2", tokenizer.nextLine());
    assertEquals("3", tokenizer.nextLine());
  }

  public void testCountLines() {
    assertEquals(0, TextUtil.countLines(""));
    assertEquals(1, TextUtil.countLines("1"));
    assertEquals(1, TextUtil.countLines("1\n"));
    assertEquals(2, TextUtil.countLines("1\n2"));
  }

  public void testEscapeUnescapeChar() {
    assertEquals("abc", TextUtil.escapeChar("abc", ','));
    assertEquals("ab\\,c", TextUtil.escapeChar("ab,c", ','));
    assertEquals("a\\\\b\\,c", TextUtil.escapeChar("a\\b,c", ','));

    assertEquals("abc", TextUtil.unescapeChar("abc", ','));
    assertEquals("ab,c", TextUtil.unescapeChar("ab\\,c", ','));
    assertEquals("a\\b,c", TextUtil.unescapeChar("a\\\\b\\,c", ','));
  }

  public void testNameMnemonic() {
    NameMnemonic mnemonic = NameMnemonic.parseString("abc");
    assertEquals("abc", mnemonic.getText());
    assertEquals(-1, mnemonic.getMnemonicIndex());
    assertEquals(0, mnemonic.getMnemonicChar());

    mnemonic = NameMnemonic.parseString("&&a&b&&c");
    assertEquals("&ab&c", mnemonic.getText());
    assertEquals(Env.isMac() ? -1 : 2, mnemonic.getMnemonicIndex());
    if (!Env.isMac())
      assertEquals(KeyEvent.VK_B, mnemonic.getMnemonicChar());
  }

  public void testParseBigDecimal() {
    checkParse(" 1 ");
    checkParse(" 2 ");
    checkParse(" -2.3 ");
  }

  public void __testFuckingBigDecimal() {
    // this works on workstation and does not work on build server!!!!
    checkParse(" 12345678901234567890.123\t");
  }

  private void checkParse(String str) {
    BigDecimal n = TextUtil.parseBigDecimal(str);
    assertNotNull(str, n);
    String sample1 = str.trim();
    String sample2 = TextUtil.bigDecimalToString(n);
    assertEquals(n.toString() + ":" + sample1 + ":" + sample2, sample1, sample2);
  }
}
