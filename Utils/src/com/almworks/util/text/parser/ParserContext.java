package com.almworks.util.text.parser;

import com.almworks.util.collections.IntArray;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author : Dyoma
 */
public abstract class ParserContext<N> {
  private final int[] myTokenSequence;
  private final String[] myTokens;
  private final Map<String, Integer> myTokenIds;
  private final TokenRegistry<N> myTokenizer;
  private ParserContext<N> myParent;
  private int myOffset;

  public ParserContext(int[] tokenSequence, Map<String, Integer> tokenIds, TokenRegistry<N> tokenizer) {
    this(tokenSequence, tokenIds, tokenizer, null, 0);
  }

  private ParserContext(int[] tokenSequence, Map<String, Integer> tokenIds, TokenRegistry<N> tokenizer, ParserContext<N> parent, int offset) {
    myTokenSequence = tokenSequence;
    myTokenIds = tokenIds;
    myTokenizer = tokenizer;
    myParent = parent != null ? parent.getRoot() : this;
    myOffset = (parent != null ? parent.myOffset : 0) + offset;
    myTokens = new String[tokenIds.size()];
    for (Map.Entry<String, Integer> entry : tokenIds.entrySet()) {
      myTokens[entry.getValue()] = entry.getKey();
    }
  }

  private ParserContext<N> getRoot() {
    return myParent != null ? myParent : this;
  }

  public ParserContext(int[] sequence, ParserContext<N> parent, int offset) {
    this(sequence, parent.myTokenIds, parent.myTokenizer, parent, 0);
  }

  protected Map<String, Integer> getTokenIds() {
    return myTokenIds;
  }

  protected int getFirstId() throws ParseException {
    if (myTokenSequence.length > 0)
      return myTokenSequence[0];
    else
      throw createException("no tokens", 0, 0);
  }

  public int[] getCommutativeOperations() {
    return getUsedSymbolIds(myTokenizer.getCommutativeOperations());
  }

  private int[] getUsedSymbolIds(Collection<String> operations) {
    IntArray result = new IntArray();
    for (String operation : operations) {
      Integer id = myTokenIds.get(operation);
      if (id == null)
        continue;
      result.add(id);
    }
    return result.toNativeArray();
  }

  public int[] getInfixConstrains() {
    return getUsedSymbolIds(myTokenizer.getInfixConstraints());
  }

  public ParserContext<N> stripBraces() throws ParseException {
    int braceCount = 0;
    if (myTokenSequence.length == 0)
      return this;
    while (myTokenSequence[braceCount] == Tokenize.OPEN_BRACE &&
      myTokenSequence[myTokenSequence.length - 1 - braceCount] == Tokenize.CLOSE_BRACE)
      braceCount++;
    if (braceCount == 0)
      return this;
    int openBraces = 0;
    for (int i = braceCount; i < myTokenSequence.length - 1 - braceCount; i++) {
      int token = myTokenSequence[i];
      switch (token) {
      case Tokenize.OPEN_BRACE: openBraces++; break;
      case Tokenize.CLOSE_BRACE:
        if (openBraces > 0)
          openBraces--;
        else
          braceCount--;
        break;
      }
    }
    if (braceCount <= 0)
      return this;
    int first = braceCount;
    int last = myTokenSequence.length - 1 - braceCount;
    if (first == last + 1)
      return instantiate(new int[0], first);
    return slice(first, last);
  }

  public int find(int id, int index) throws ParseException {
    for (int i = index; i < myTokenSequence.length; i++) {
      int currentId = myTokenSequence[i];
      if (currentId == id)
        return i;
      if (currentId == Tokenize.OPEN_BRACE)
        i = skipBraces(i);
    }
    return -1;
  }

  private int skipBraces(int index) throws ParseException {
    int braceCount = 1;
    for (int i = index + 1; i < myTokenSequence.length; i++) {
      switch (myTokenSequence[i]) {
      case Tokenize.OPEN_BRACE: braceCount++;break;
      case Tokenize.CLOSE_BRACE: braceCount--; break;
      default: continue;
      }
      if (braceCount == 0)
        return i;
    }
    throw createException("Brace not closed", index, index);
  }

  public ParseException createException(String message, int start, int last) {
    return new ParseException(message, start + myOffset, last + myOffset, getTokensText());
  }

  public CommutativeParser<N> getCommutative(int tokenId) throws ParseException {
    String token = myTokens[tokenId];
    CommutativeParser<N> parser = myTokenizer.getCommutative(token);
    return checkParserRegistered(parser, token);
  }

  public InfixParser<N> getInfix(int id) throws ParseException {
    String token = myTokens[id];
    return checkParserRegistered(myTokenizer.getInfix(token), token);
  }

  public FunctionParser<N> getParser(int id) throws ParseException {
    String token = myTokens[id];
    return checkParserRegistered(myTokenizer.getParser(token), token);
  }

  private <P> P checkParserRegistered(P parser, String token)
    throws ParseException {
    if (parser == null)
      throw createException("No parser registered for '" + token + "'", -1, -1);
    return parser;
  }

  public ParserContext<N> slice(int first, int last) throws ParseException {
    if (first == 0 && last == myTokenSequence.length - 1)
      return this;
    if (first > last)
      throw createException("Empty operand at " + first, first, last);
    int[] reducedSequence = new int[last - first + 1];
    System.arraycopy(myTokenSequence, first, reducedSequence, 0, reducedSequence.length);
    return instantiate(reducedSequence, first);
  }

  protected abstract ParserContext<N> instantiate(int[] reducedSequence, int offset);

  public String getToken(int id) {
    return myTokens[id];
  }

  public abstract N parseNode() throws ParseException;

  protected ParserContext<N> sliceTail(int firstIndex) throws ParseException {
    return slice(firstIndex, myTokenSequence.length - 1);
  }

  String getTokensText() {
    StringBuffer buffer = new StringBuffer();
    String separator = "";
    for (int i = 0; i < myTokenSequence.length; i++) {
      int id = myTokenSequence[i];
      buffer.append(separator);
      buffer.append(myTokens[id]);
      separator = " ";
    }
    return buffer.toString();
  }

  public String toString() {
    return getTokensText();
  }

  @Nullable
  public String getSingleOrNull() throws ParseException {
    if (myTokenSequence.length <= 1)
      return myTokenSequence.length == 1 ? getToken(myTokenSequence[0]) : null;
    if (myTokenSequence.length <= 3) {
      ParserContext<N> stripped = stripBraces();
      if (stripped.myTokenSequence.length != myTokenSequence.length)
        return stripped.getSingleOrNull();
    }
    throw createException("Expected one or none but was: <" + getTokensText() + ">", 0, myTokenSequence.length - 1);
  }

  @NotNull
  public String getSingle() throws ParseException {
    String result = getSingleOrNull();
    if (result != null)
      return result;
    throw createException("Expected not empty", 0, myTokenSequence.length - 1);
  }

  public List<String> getAllTokens() {
    List<String> result = Collections15.arrayList();
    for (int i = 0; i < myTokenSequence.length; i++) {
      int token = myTokenSequence[i];
      result.add(myTokens[token]);
    }
    return result;
  }

  public boolean isEmpty() {
    return myTokenSequence.length == 0;
  }

  public static <T> List<T> parseAll(Iterator<ParserContext<T>> iterator) throws ParseException {
    List<T> result = Collections15.arrayList();
    while (iterator.hasNext())
      result.add(iterator.next().parseNode());
    return result;
  }
}
