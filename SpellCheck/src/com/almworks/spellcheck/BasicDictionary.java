package com.almworks.spellcheck;

import com.softcorporation.suggester.engine.core.Dictionary;
import com.softcorporation.suggester.util.SuggesterException;

public class BasicDictionary extends Dictionary {
  @Override
  public String getDictionaryType() {
    return "basic";
  }

  public static Dictionary loadFromFile(String jarPath, String dictionary) throws SuggesterException {
    BasicDictionary result = new BasicDictionary();
    result.load("file://" + jarPath, dictionary);
    return result;
  }
}
