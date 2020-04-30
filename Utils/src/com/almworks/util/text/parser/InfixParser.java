package com.almworks.util.text.parser;

/**
 * @author : Dyoma
 */
public interface InfixParser<N> {
  N parse(ParserContext<N> left, ParserContext<N> right) throws ParseException;
}
