package com.almworks.util.text.parser;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;

import java.util.Iterator;

/**
 * @author : Dyoma
 */
public class QueryTokenizerTests extends BaseTestCase {
  public static final Object DUMMY_NODE = new Object();
  public static final FunctionParser<Object> DUMMY_PARSER = new FunctionParser<Object>() {
    public Object parse(ParserContext<Object> context) {
      return DUMMY_NODE;
    }
  };

  public static final CommutativeParser<Object> DUMMY_COMMUTATIVE = new CommutativeParser<Object>() {
    public Object parse(Iterator<ParserContext<Object>> parameters) {
      return DUMMY_NODE;
    }
  };

  public static final InfixParser<Object> DUMMY_CONSTRAINT = new InfixParser<Object>() {
    public Object parse(ParserContext<Object> left, ParserContext<Object> right) {
      return DUMMY_NODE;
    }
  };

  public void testTokens() {
    TokenRegistry<Object> tokenizer = new TokenRegistry<Object>();
    tokenizer.registerFunction("f", DUMMY_PARSER);
    tokenizer.registerInfixOperation("|", DUMMY_COMMUTATIVE);
    tokenizer.registerInfixConstraint("=", DUMMY_CONSTRAINT);
    new CollectionsCompare().unordered(tokenizer.getPossibleTokens().keySet(), new String[]{"f", "|", "="});
  }

  public void testTokenize() throws ParseException {
    Tokenize tokenize = new Tokenize("a=b");
    tokenize.doTokenize();
    checkTokens(new String[]{"a", "=", "b"}, tokenize);
  }

  public void testTokenizeResult() throws ParseException {
    Tokenize tokenize = new Tokenize("ab |$ (45a-12+)=");
    tokenize.doTokenize();
    checkTokens(new String[]{"ab", "|$", "(", "45a", "-", "12", "+", ")", "="}, tokenize);
  }

  public void testTokenizeQuoted() throws ParseException {
    Tokenize tokenize = new Tokenize("a \"1\\\\2\\\"3(\" b)");
    tokenize.doTokenize();
    checkTokens(new String[]{"a", "1\\2\"3(", "b", ")"}, tokenize);
  }

  public void testTokenizeEmpty() {
    Tokenize tokenize = new Tokenize("");
    assertEquals(0, tokenize.getSequence().length);
  }


  public static void checkTokens(String[] strings, Tokenize tokenize) {
    for (int i = 0; i < tokenize.getSequence().length; i++) {
      int id = tokenize.getSequence()[i];
      String expectedToken = strings[i];
      Integer integer = tokenize.getTokenIds().get(expectedToken);
      assertNotNull(expectedToken, integer);
      assertEquals(integer.intValue(), id);
    }
  }

  public void testStripBraces() throws ParseException {
    TokenRegistry registry = new TokenRegistry();
    ParserContext context = registry.tokenize("(((())()))");
    assertEquals("( ( ( ( ) ) ( ) ) )", context.getTokensText());
    ParserContext stripped = context.stripBraces();
    assertEquals("( ( ) ) ( )", stripped.getTokensText());
    assertNotSame(context, stripped);
    assertSame(stripped, stripped.stripBraces());

    context = registry.tokenize("()");
    assertTrue(context.stripBraces().isEmpty());
  }
}
