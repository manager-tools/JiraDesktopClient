package com.almworks.spellcheck.dictionary;

import com.softcorporation.suggester.BasicSuggester;
import com.softcorporation.suggester.Suggester;
import com.softcorporation.suggester.Suggestion;
import com.softcorporation.suggester.engine.Searcher;
import com.softcorporation.suggester.engine.core.Dictionary;
import com.softcorporation.suggester.language.fuzzy.Matcher;
import com.softcorporation.suggester.language.fuzzy.Phoneme;
import com.softcorporation.suggester.language.sound.SoundEncoder;
import com.softcorporation.suggester.util.BasicSuggesterConfiguration;
import com.softcorporation.suggester.util.Constants;
import com.softcorporation.suggester.util.SuggesterException;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MultiDictionarySuggester extends Suggester {
  private final BasicSuggester[] mySuggesters;

  private MultiDictionarySuggester(BasicSuggester[] suggesters) {
    mySuggesters = suggesters;
  }

  @Nullable
  public static Suggester create(BasicSuggesterConfiguration config, Collection<? extends Dictionary> dictionaries) throws SuggesterException {
    if (dictionaries.isEmpty()) return null;
    dictionaries = Collections15.arrayList(dictionaries);
    for (Iterator<? extends Dictionary> iterator = dictionaries.iterator(); iterator.hasNext(); ) if (iterator.next() == null) iterator.remove();
    if (dictionaries.size() == 1) {
      BasicSuggester suggester = new BasicSuggester(config);
      suggester.attach(dictionaries.iterator().next());
      return suggester;
    }
    BasicSuggester[] suggesters = new BasicSuggester[dictionaries.size()];
    int i = 0;
    for (Dictionary dictionary : dictionaries) {
      BasicSuggester suggester = new BasicSuggester(config);
      suggester.attach(dictionary);
      suggesters[i] = suggester;
      i++;
    }
    return new MultiDictionarySuggester(suggesters);
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
    ArrayList<Suggestion> best = null;
    for (Suggester suggester : mySuggesters) {
      @SuppressWarnings("unchecked")
      ArrayList<Suggestion> suggestions = suggester.getSuggestions(word, limit, langCode);
      best = chooseBetter(best, suggestions);
    }
    return best != null ? best : Collections15.arrayList();
  }

  private ArrayList<Suggestion> chooseBetter(ArrayList<Suggestion> s1, ArrayList<Suggestion> s2) {
    if (s1 == null || s1.isEmpty()) return s2;
    if (s2 == null || s2.isEmpty()) return s1;
    Suggestion min1 = Collections.min(s1);
    Suggestion min2 = Collections.min(s2);
    return min1.compareTo(min2) < 0 ? s1 : s2;
  }

  @Override
  public boolean hasExactWord(String word) throws SuggesterException {
    for (Suggester suggester : mySuggesters) if (suggester.hasExactWord(word)) return true;
    return false;
  }

  @Override
  public int hasWord(String word) throws SuggesterException {
    int max = Constants.RESULT_ID_NO_MATCH;
    for (Suggester suggester : mySuggesters) {
      int result = suggester.hasWord(word);
      if (result == Constants.RESULT_ID_MATCH_EXACT) return result;
      max = Math.max(max, result);
    }
    return max;
  }

  @Override
  protected TreeSet sortSuggestions(String word, ArrayList suggestions, String langCode) {
    return sortSuggestions((BasicSuggesterConfiguration) mySuggesters[0].getConfiguration(), word, suggestions, langCode);
  }

  public static TreeSet sortSuggestions(BasicSuggesterConfiguration configuration, String word, ArrayList suggestions, String langCode) {
    // Copy-paste from BasicSuggester
    if (suggestions == null || suggestions.isEmpty()) return new TreeSet();
    Suggestion joinedSuggestion = null;
    TreeSet sortedSuggestions = new TreeSet();

    String sCode = null;
    SoundEncoder soundEncoder = SoundEncoder.getEncoder(langCode);
    if (soundEncoder != null)
    {
      sCode = soundEncoder.getSoundCode(word);
    }

    Phoneme[][] phonemes = null;
    Matcher matcher = Matcher.getMatcher(langCode);
    if (matcher != null)
    {
      phonemes = matcher.getPhonemes(word.toLowerCase());
    }

    String word1 = word.toLowerCase();
    int len1 = word1.length();

    TreeMap acceptedResults = new TreeMap();
    Iterator iter = suggestions.iterator();
    while (iter.hasNext())
    {
      Suggestion suggestion = (Suggestion) iter.next();
      if (soundEncoder != null)
      {
        String sCode1 = soundEncoder.getSoundCode(suggestion.word);
        suggestion.scoreSD = Searcher.getED(sCode, sCode1, Constants.ED_SD_MAX);
      }

      // calculate first character score
      char c = word.charAt(0);
      char c1 = suggestion.word.charAt(0);
      if (c != c1)
      {
        char cL = Character.toLowerCase(c);
        char cU = Character.toUpperCase(c);
        char c1L = Character.toLowerCase(c1);
        char c1U = Character.toUpperCase(c1);
        if (cL == c1L)
        {
          if (cL != c1)
          {
            suggestion.scoreFC = configuration.WEIGHT_FIRST_CHAR_UPPER;
          }
          else
          {
            suggestion.scoreFC = configuration.WEIGHT_FIRST_CHAR_LOWER;
          }
        }
        else
        {
          suggestion.scoreFC = configuration.WEIGHT_FIRST_CHAR;
          if (c1U == c1 && cU != c)
          {
            suggestion.scoreFC += configuration.WEIGHT_FIRST_CHAR_UPPER;
          }
          else if (c1L == c1 && cL != c)
          {
            suggestion.scoreFC += configuration.WEIGHT_FIRST_CHAR_LOWER;
          }
        }
      }

      String word2 = suggestion.word.toLowerCase();
      int len2 = suggestion.word.length();

      suggestion.scoreLN = Math.abs(len1 - len2);

      // calculate append remove score
      if (configuration.WEIGHT_ADD_REM_CHAR > 0)
      {
        int len;
        if (len2 < len1)
        {
          len = len1;
        }
        else
        {
          len = len2;
        }
        for (int i = 0; i < len; i++)
        {
          if (i < len1)
          {
            if (word2.indexOf(word1.charAt(i)) < 0)
            {
              suggestion.scoreAR++;
            }
          }
          if (i < len2)
          {
            if (word1.indexOf(word2.charAt(i)) < 0)
            {
              suggestion.scoreAR++;
            }
          }
        }
        suggestion.scoreAR = suggestion.scoreAR;
      }

      if (word.charAt(len1 - 1) != suggestion.word.charAt(len2 - 1))
      {
        suggestion.scoreLC = configuration.WEIGHT_LAST_CHAR;
      }

      if (matcher != null)
      {
        suggestion.scoreFP = (100 - matcher.getWeight(word, word2, phonemes));
      }

      // calculate total score
      suggestion.score = suggestion.scoreED
        * configuration.WEIGHT_EDIT_DISTANCE + suggestion.scoreSD
        * configuration.WEIGHT_SOUNDEX + suggestion.scoreLN
        * configuration.WEIGHT_LENGTH + suggestion.scoreFC
        + suggestion.scoreLC + suggestion.scoreAR
        * configuration.WEIGHT_ADD_REM_CHAR + suggestion.scoreJW
        + suggestion.scoreFP * configuration.WEIGHT_FUZZY;

      // remove variations of joined words
      if (suggestion.scoreJW > 0 && configuration.REMOVE_JOINED_VARIATIONS)
      {
        if (joinedSuggestion == null)
        {
          joinedSuggestion = suggestion;
        }
        else
        {
          if (suggestion.score < joinedSuggestion.score)
          {
            sortedSuggestions.remove(joinedSuggestion);
            joinedSuggestion = suggestion;
          }
          else
          {
            continue;
          }
        }
      }

      // remove duplicates (EN: Tab / tab)
      if (configuration.REMOVE_CASE_DUPLICATES)
      {
        Suggestion suggestion0 = (Suggestion) acceptedResults.get(word2);
        if (suggestion0 == null)
        {
          sortedSuggestions.add(suggestion);
          acceptedResults.put(word2, suggestion);
        }
        else if (suggestion.score < suggestion0.score)
        {
          sortedSuggestions.remove(suggestion0);
          sortedSuggestions.add(suggestion);
          acceptedResults.put(word2, suggestion);
        }
      }
      else
      {
        sortedSuggestions.add(suggestion);
      }
    }
    return sortedSuggestions;
  }
}
