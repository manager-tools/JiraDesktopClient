package com.almworks.spellcheck;

import com.almworks.spellcheck.util.CorrectMisspellListener;
import com.almworks.util.debug.DebugFrame;
import com.softcorporation.suggester.BasicSuggester;
import com.softcorporation.suggester.engine.core.Dictionary;
import com.softcorporation.suggester.util.SpellCheckConfiguration;
import com.softcorporation.suggester.util.SuggesterException;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * This application should be launched with working directory at the project root.
 */
public class SpellCheckSample {
  public static void main(String[] args) throws SuggesterException {
    final JTextComponent component = new JTextField();
    SpellCheckConfiguration config = new SpellCheckConfiguration("lib/suggester-basic/spellCheck.config");
    Dictionary dictionary = BasicDictionary.loadFromFile("lib/suggester-basic/dictionaries/english/english.jar", "english");
    BasicSuggester suggester = new BasicSuggester(config);
    suggester.attach(dictionary);
    TextSpellChecker.install(Lifespan.FOREVER, component, config, suggester);
    CorrectMisspellListener.install(component);
    DebugFrame.show(new JScrollPane(component), 200, 200);

  }
}
