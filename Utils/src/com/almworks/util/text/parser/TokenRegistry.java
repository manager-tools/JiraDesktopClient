package com.almworks.util.text.parser;

import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class TokenRegistry<N> {
  private final Map<String, FunctionParser<N>> myFunctions = Collections15.linkedHashMap();
  private final Map<String, CommutativeParser<N>> myInfix = Collections15.linkedHashMap();
  private final Map<String, InfixParser<N>> myConstrains = Collections15.linkedHashMap();

  public void registerFunction(String symbol, FunctionParser<N> parser) {
    registerNewSymbol(symbol, myFunctions, parser);
  }

  public void registerInfixOperation(String symbol, CommutativeParser<N> parser) {
    registerNewSymbol(symbol, myInfix, parser);
  }

  public void registerInfixConstraint(String symbol, InfixParser<N> parser) {
    registerNewSymbol(symbol, myConstrains, parser);
  }

  public ParserContext<N> tokenize(String text) throws ParseException {
    Tokenize tokenize = new Tokenize(text);
    tokenize.doTokenize();
    return new ParserContextImpl<N>(tokenize.getSequence(), tokenize.getTokenIds(), this);
  }

  Map<String, Object> getPossibleTokens() {
    Map<String, Object> result = Collections15.hashMap();
    result.putAll(myFunctions);
    result.putAll(myInfix);
    result.putAll(myConstrains);
    return result;
  }

  private <P> void registerNewSymbol(String symbol, Map<String, P> registery, P parser) {
    if (parser == null)
      throw new NullPointerException();
    assert checkNewToken(symbol);
    checkNewToken(symbol);
    registery.put(symbol, parser);
  }

  private boolean checkNewToken(String symbol) {
    Map<String, Object> possibleTokens = getPossibleTokens();
    assert !possibleTokens.containsKey(symbol) : "Symbol: " + symbol + " Object: " + possibleTokens.get(symbol);
    return true;
  }

  public Collection<String> getCommutativeOperations() {
    return myInfix.keySet();
  }

  public CommutativeParser<N> getCommutative(String token) {
    return myInfix.get(token);
  }

  public InfixParser<N> getInfix(String token) {
    return myConstrains.get(token);
  }

  public Collection<String> getInfixConstraints() {
    return myConstrains.keySet();
  }

  public FunctionParser<N> getParser(String token) {
    return myFunctions.get(token);
  }
}
