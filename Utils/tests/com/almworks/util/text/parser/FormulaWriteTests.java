package com.almworks.util.text.parser;

import junit.framework.TestCase;

/**
 * @author : Dyoma
 */
public class FormulaWriteTests extends TestCase {
  public void testCreateChild() {
    FormulaWriter writer = FormulaWriter.create();
    writer.addRaw("-");
    FormulaWriter child = writer.createChild();
    child.addRaw("1+2");
    writer.addRaw("*");
    writer.addRaw("3");
    assertEquals("-(1+2)*3", writer.getWholeText());
  }

  public void testAddQuotes() throws ParseException {
    FormulaWriter writer = FormulaWriter.create();
    writer.addToken("a_b");
    writer.addToken("\"abc\"");
    writer.addToken("123");
    writer.addToken(null);
    assertEquals("\"a_b\"\"\\\"abc\\\"\"123\"\"", writer.getWholeText());
    Tokenize tokenize = new Tokenize(writer.getWholeText());
    tokenize.doTokenize();
    QueryTokenizerTests.checkTokens(new String[]{"a_b", "\"abc\"", "123", ""}, tokenize);
  }
}
