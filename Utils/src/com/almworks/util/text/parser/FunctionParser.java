package com.almworks.util.text.parser;

/**
 * @author : Dyoma
 */
public interface FunctionParser<N> {
  N parse(ParserContext<N> context) throws ParseException;
}
