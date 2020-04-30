package com.almworks.spellcheck.dictionary;

import com.softcorporation.suggester.Suggester;
import com.softcorporation.suggester.engine.core.Dictionary;
import com.softcorporation.suggester.util.BasicSuggesterConfiguration;
import com.softcorporation.suggester.util.Constants;
import com.softcorporation.suggester.util.SuggesterException;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class AlmSuggester extends Suggester {
  private final BasicSuggesterConfiguration myConfiguration;
  @Nullable
  private final Suggester myNext;
  private final List<AlmDictionary> myDictionaries;

  public AlmSuggester(BasicSuggesterConfiguration configuration, List<AlmDictionary> dictionaries, @Nullable Suggester next) {
    myConfiguration = configuration;
    myNext = next;
    myDictionaries = Collections15.unmodifiableListCopy(dictionaries);
  }

  @Nullable
  public static Suggester create(BasicSuggesterConfiguration configuration, @Nullable Suggester next, AlmDictionary ... dictionaries) {
    ArrayList<AlmDictionary> list = Collections15.arrayList();
    for (AlmDictionary dictionary : dictionaries) if (dictionary != null) list.add(dictionary);
    if (list.isEmpty()) return next;
    return new AlmSuggester(configuration, list, next);
  }

  @Override
  public boolean attach(Dictionary dictionary) throws SuggesterException {
    Log.error("Not implemented");
    return false;
  }

  @Override
  public boolean detach(Dictionary dictionary) {
    Log.error("Not implemented");
    return false;
  }

  @Override
  public ArrayList getSuggestions(String word, int limit, String langCode) throws SuggesterException {
    return myNext != null ? myNext.getSuggestions(word, limit, langCode) : Collections15.arrayList();
  }

  @Override
  public boolean hasExactWord(String word) throws SuggesterException {
    return priHasExactWord(word) || myNext != null && myNext.hasExactWord(word);
  }

  @Override
  public int hasWord(String word) throws SuggesterException {
    if (priHasExactWord(word)) return Constants.RESULT_ID_MATCH_EXACT;
    return myNext != null ? myNext.hasWord(word) : Constants.RESULT_ID_NO_MATCH;
  }

  private boolean priHasExactWord(String word) {
    word = Util.lower(word);
    for (AlmDictionary dictionary : myDictionaries) if (dictionary.hasWord(word)) return true;
    return false;
  }

  @Override
  protected TreeSet sortSuggestions(String word, ArrayList suggestions, String langCode) {
    return MultiDictionarySuggester.sortSuggestions(myConfiguration, word, suggestions, langCode);
  }

  public interface AlmDictionary {
    boolean hasWord(String lowCaseWord);
  }
}
