package com.almworks.api.search.types;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.ContainsTextConstraint;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.search.FilterBasedSearchExecutor;
import com.almworks.api.search.TextSearchExecutor;
import com.almworks.api.search.TextSearchType;
import com.almworks.api.search.TextSearchUtils;
import com.almworks.api.syncreg.SyncCubeRegistry;
import com.almworks.api.syncreg.SyncRegistry;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.util.English;
import com.almworks.util.Terms;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.i18n.Local;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SearchWords implements TextSearchType {
  private final Engine myEngine;
  private final SyncCubeRegistry myCubeRegistry;

  public SearchWords(SyncRegistry registry, Engine engine) {
    myCubeRegistry = registry.getSyncCubeRegistry();
    myEngine = engine;
  }

  public final TextSearchExecutor parse(String searchString) {
    final String[] words = getWords(searchString);
    if(words == null || words.length == 0) {
      return null;
    }
    return new MyResult(words);
  }

  protected String[] getWords(String searchString) {
    return new Parser(searchString).parse();
  }

  public String getDisplayableShortName() {
    return "text";
  }

  public int getWeight() {
    return Weight.WORD_SEARCH;
  }

  public static BoolExpr<DP> createFilter(Connection connection, final String[] searchWords) {
    if(connection == null) {
      return null;
    }
    if(searchWords == null || searchWords.length == 0) {
      return BoolExpr.TRUE();
    }
    return new MyDP(connection, searchWords).term();
  }

  public static char[][] wordsToCharArrays(String[] searchWords) {
    final char[][] words = new char[searchWords.length][];
    for (int i = 0; i < searchWords.length; i++) {
      words[i] = searchWords[i].toCharArray();
    }
    return words;
  }

  private class MyResult extends FilterBasedSearchExecutor {
    private final String[] myWords;
    private final String myDescription;

    public MyResult(String[] words) {
      super(SearchWords.this.myEngine, SearchWords.this.myCubeRegistry);
      assert words.length > 0;
      myWords = words;
      myDescription = createDescription(words);
    }

    private String createDescription(String[] words) {
      StringBuffer buf = new StringBuffer();
      buf.append(Local.text(Terms.key_Artifacts))
        .append(" containing ")
        .append(English.getSingularOrPlural("word", words.length))
        .append(' ');
      return TextUtil.separate(words, ", ");
    }

    @Override
    public Constraint getConstraint(Connection connection) {
      return ContainsTextConstraint.Simple.create(myWords);
    }

    @Override
    public String getSearchDescription() {
      return myDescription;
    }

    @Override
    public BoolExpr<DP> getFilter(final Connection connection) {
      return createFilter(connection, myWords);
    }

    @Override
    public Collection<GenericNode> getRealScope(Collection<GenericNode> nodes) {
      return nodes;
    }

    @Override
    public TextSearchType getType() {
      return SearchWords.this;
    }
  }

  private static class MyDP extends DP {
    @NotNull private final Connection myConnection;
    @NotNull private final String[] myStringWords;
    @NotNull private final char[][] myCharWords;

    public MyDP(@NotNull Connection connection, @NotNull String[] stringWords) {
      myConnection = connection;
      myStringWords = stringWords;
      myCharWords = wordsToCharArrays(stringWords);
    }

    @Override
    public boolean accept(long item, DBReader reader) {
      return myConnection.matchAllWords(item, myCharWords, myStringWords, reader);
    }

    @Override
    protected boolean equalDP(DP other) {
      final MyDP that = (MyDP)other;
      return myConnection.getConnectionID().equals(that.myConnection.getConnectionID())
        && Arrays.equals(myStringWords, that.myStringWords);
    }

    @Override
    protected int hashCodeDP() {
      return 31 * (41 + myConnection.getConnectionID().hashCode()) + Arrays.hashCode(myStringWords);
    }

    @Override
    public String toString() {
      return "match(" + myConnection.getConnectionID() + ", " + Arrays.asList(myStringWords) + ")";
    }
  }

  public static class Parser {
    private final String myString;

    private final List<String> myWords = Collections15.arrayList();
    private final StringBuilder myBuffer = new StringBuilder();
    private char myLastChar = '\0';

    public Parser(String string) {
      myString = string;
    }

    public String[] parse() {
      if(myString == null) {
        return null;
      }
      
      final int len = myString.length();
      boolean inQuotes = false;

      for(int i = 0; i < len; i++) {
        final char c = myString.charAt(i);

        if(isEscape(c)) {
          appendEscaped(c);
        } else if(isDelimiter(c)) {
          if(inQuotes) {
            appendChar(c);
          } else {
            endOfWord();
          }
        } else if(isQuote(c)) {
          if(inQuotes) {
            endOfWord();
          }
          inQuotes = !inQuotes;
        } else {
          appendChar(c);
        }

        myLastChar = c;
      }

      endOfWord();

      return myWords.isEmpty() ? null : myWords.toArray(new String[myWords.size()]);
    }

    private boolean isEscape(char c) {
      if(myLastChar != '\\') {
        return false;
      }
      return c == '\"' || c == '\\';
    }

    private char unescape(char c) {
      return c;
    }

    private boolean isDelimiter(char c) {
      return TextSearchUtils.isDelimiter(c);
    }

    private boolean isQuote(char c) {
      return c == '\"';
    }

    private void appendChar(char c) {
      myBuffer.append(c);
    }

    private void appendEscaped(char c) {
      myBuffer.setLength(myBuffer.length() - 1);
      myBuffer.append(unescape(c));
    }

    private void endOfWord() {
      if(myBuffer.length() > 0) {
        myWords.add(myBuffer.toString());
        myBuffer.setLength(0);
      }
    }
  }
}
