package com.almworks.util.text.parser;

import com.almworks.util.collections.Containers;

import java.util.Iterator;
import java.util.List;

/**
 * @author : Dyoma
 */
public interface CommutativeParser<N> {
  /**
   * @see ParserContext#parseAll(java.util.Iterator<Object>) common implementation
   */
  N parse(Iterator<ParserContext<N>> parameters) throws ParseException;

  abstract class Greater<N> implements CommutativeParser<N> {
    private final String mySymbol;

    protected Greater(String symbol) {
      mySymbol = symbol;
    }

    public N parse(Iterator<ParserContext<N>> parameters) throws ParseException {
      List<ParserContext<N>> params = Containers.collectList(parameters);
      int paramsCount = params.size();
      if (paramsCount == 2) {
        String arg = params.get(0).getSingle();
        String upperBound = params.get(1).getSingleOrNull();
        return createOneBound(arg, upperBound);
      } else if (paramsCount == 3) {
        String arg = params.get(1).getSingle();
        String lowerBound = params.get(0).getSingleOrNull();
        String upperBound = params.get(2).getSingleOrNull();
        return createTwoBound(lowerBound, arg, upperBound);
      }
      throw ParseException.semanticError(
        "Wrong number of parameters for '" + mySymbol +"'. " +
          "Expected: 'attr " + mySymbol +" value' or " +
          "'lower " + mySymbol +" attr " + mySymbol +" upper'");
    }

    public void register(TokenRegistry<N> registry) {
      registry.registerInfixOperation(mySymbol, this);
    }

    protected abstract N createTwoBound(String lowerBound, String arg, String upperBound) throws ParseException ;

    protected abstract N createOneBound(String first, String second) throws ParseException ;
  }
}
