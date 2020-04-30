package com.almworks.spellcheck;

import com.softcorporation.suggester.BasicSuggester;
import com.softcorporation.suggester.engine.core.Dictionary;
import com.softcorporation.suggester.tools.SpellCheck;
import com.softcorporation.suggester.util.SpellCheckConfiguration;
import com.softcorporation.suggester.util.SuggesterException;

public class SpellCheckDebug {
  public static void main(String[] args) throws SuggesterException {
    SpellCheckConfiguration config = new SpellCheckConfiguration("lib/suggester-basic/spellCheck.config");
    BasicSuggester suggester = new BasicSuggester(config);
    Dictionary dictionary = BasicDictionary.loadFromFile("lib/suggester-basic/dictionaries/english/english.jar", "english");
    suggester.attach(dictionary);
    SpellCheck spellCheck = new SpellCheck(config);
    spellCheck.setSuggester(suggester);
    spellCheck.setSuggestionLimit(5);
    spellCheck.setText("pay for me");
    config.CAPITALIZE_FIRST_LETTER = false;
    spellCheck.check();
    while (spellCheck.hasMisspelt()) {
      System.out.println(spellCheck.getMisspelt());
      spellCheck.checkNext();
    }
  }
}
