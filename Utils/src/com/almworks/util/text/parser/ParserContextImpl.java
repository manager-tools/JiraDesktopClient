package com.almworks.util.text.parser;

import com.almworks.util.Pair;
import org.almworks.util.Collections15;

import java.util.List;
import java.util.Map;

/**
 * @author : Dyoma
 */
class ParserContextImpl<N> extends ParserContext<N> {
  public ParserContextImpl(int[] tokenSequence, Map<String, Integer> tokenIds, TokenRegistry<N> tokenizer) {
    super(tokenSequence, tokenIds, tokenizer);
  }

  public ParserContextImpl(int[] sequence, ParserContext<N> parent, int offset) {
    super(sequence, parent, offset);
  }

  protected ParserContext<N> instantiate(int[] reducedSequence, int offset) {
    return new ParserContextImpl<N>(reducedSequence, this, offset);
  }

  public N parseNode() throws ParseException {
    ParserContext<N> parserContext = stripBraces();
    if (parserContext != this)
      return parserContext.parseNode();
    Pair<CommutativeParser<N>, List<ParserContext<N>>> commutative = splitByCommutative();
    if (commutative != null)
      return commutative.getFirst().parse(commutative.getSecond().iterator());
    Pair<InfixParser<N>, List<ParserContext<N>>> infix = splitByInfix();
    if (infix != null) {
      List<ParserContext<N>> arguements = infix.getSecond();
      return infix.getFirst().parse(arguements.get(0), arguements.get(1));
    }
    int id = getFirstId();
    FunctionParser<N> parser = getParser(id);
    if (parser == null)
      throw createException("Can't parse", 0, 0);
    ParserContext<N> context = sliceTail(1);
    if (Tokenize.OPEN_BRACE != context.getFirstId())
      throw context.createException("Expected '('", 0, 0);
    return parser.parse(context.stripBraces());
  }

  private Pair<InfixParser<N>, List<ParserContext<N>>> splitByInfix() throws ParseException {
    Pair<Integer, List<ParserContext<N>>> pair = splitByOneOf(getInfixConstrains());
    if (pair == null)
      return null;
    List<ParserContext<N>> contexts = pair.getSecond();
    int id = pair.getFirst().intValue();
    if (contexts.size() != 2)
      throw createException("Expected two parameters for '" + getToken(id) + "' but was " + contexts.size(), -1, -1);
    return Pair.create(getInfix(id), contexts);
  }


  private Pair<CommutativeParser<N>, List<ParserContext<N>>> splitByCommutative() throws ParseException {
    Pair<Integer, List<ParserContext<N>>> result = splitByOneOf(getCommutativeOperations());
    if (result == null)
      return null;
    int id = result.getFirst().intValue();
    List<ParserContext<N>> contexts = result.getSecond();
    return Pair.create(getCommutative(id), contexts);
  }

  private Pair<Integer, List<ParserContext<N>>> splitByOneOf(int[] ids)
    throws ParseException {
    Integer id = null;
    int index = -1;
    for (int i = 0; i < ids.length; i++) {
      index = find(ids[i], 0);
      if (index != -1) {
        id = ids[i];
        break;
      }
    }
    if (index == -1)
      return null;
    assert id != null;
    List<ParserContext<N>> contexts = Collections15.arrayList();
    int start = 0;
    do {
      contexts.add(slice(start, index - 1));
      start = index + 1;
      index = find(id.intValue(), start);
    } while (index != -1);
    contexts.add(sliceTail(start));
    return Pair.create(id, contexts);
  }
}
