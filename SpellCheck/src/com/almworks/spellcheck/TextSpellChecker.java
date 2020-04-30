package com.almworks.spellcheck;

import com.almworks.spellcheck.util.DefaultSpellHighlighter;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.swing.DocumentUtil;
import com.softcorporation.suggester.Suggester;
import com.softcorporation.suggester.Suggestion;
import com.softcorporation.suggester.tools.SpellCheck;
import com.softcorporation.suggester.util.SpellCheckConfiguration;
import com.softcorporation.suggester.util.SuggesterException;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TextSpellChecker {
  private static final Object COMPONENT_PROPERTY = new Object();

  private final Suggester mySuggester;
  private final SpellCheckConfiguration myConfig;
  private final JTextComponent myComponent;
  private final List<Object> myHighlightTags = Collections15.arrayList();
  private final List<Misspell> myMisspells = Collections15.arrayList();
  private final MyDocumentListener myRecheck;
  private final List<IgnoreSpellCheck> myIgnores = Collections15.arrayList();
  private Highlighter.HighlightPainter myHighlighter = DefaultSpellHighlighter.DEFAULT;

  private TextSpellChecker(Lifespan life, SpellCheckConfiguration config, JTextComponent component, Suggester suggester) {
    myRecheck = new MyDocumentListener(life, this);
    myConfig = config;
    myComponent = component;
    mySuggester = suggester;
  }

  public void setHighlighter(Highlighter.HighlightPainter highlighter) {
    myHighlighter = highlighter;
    try {
      recheck();
    } catch (SuggesterException e) {
      Log.error(e);
    }
  }

  public void addIgnoreSpellCheck(IgnoreSpellCheck ignore) {
    myIgnores.add(ignore);
  }

  @Nullable
  public Misspell misspellAt(int offset) {
    for (Misspell misspell : myMisspells) if (misspell.containsOffset(offset)) return misspell;
    return null;
  }

  public JTextComponent getComponent() {
    return myComponent;
  }

  public Lifespan getLife() {
    return myRecheck.myLife;
  }

  public static TextSpellChecker install(Lifespan life, JTextComponent component, SpellCheckConfiguration config, Suggester suggester) throws SuggesterException {
    final TextSpellChecker checker = new TextSpellChecker(life, config, component, suggester);
    final Document document = component.getDocument();
    if (!life.isEnded()) {
      DocumentUtil.addListener(life, document, checker.myRecheck);
      checker.forceRecheck();
    }
    component.putClientProperty(COMPONENT_PROPERTY, checker);
    life.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
          @Override
          public void run() {
            checker.myPrevText.set("");
            checker.clearHighlights();
          }
        });
      }
    });
    return checker;
  }

  public void forceRecheck() {
    myPrevText.set("");
    myRecheck.requestCheck();
  }

  private void recheck() throws SuggesterException {
    Document document = myComponent.getDocument();
    try {
      recheck(document.getText(0, document.getLength()));
    } catch (BadLocationException e) {
      Log.error(e);
    }
  }

  private final AtomicReference<String> myPrevText = new AtomicReference<String>("");
  private void recheck(String text) throws SuggesterException {
    if (text.equals(myPrevText.get())) return;
    myPrevText.set(text);
    clearHighlights();
    myMisspells.clear();
    doCheck(text, myComponent.getHighlighter());
  }

  private void clearHighlights() {
    Highlighter highlighter = myComponent.getHighlighter();
    for (Object tag : myHighlightTags) highlighter.removeHighlight(tag);
    myHighlightTags.clear();
  }

  private void doCheck(String text, Highlighter highlighter) throws SuggesterException {
    SpellCheck spellCheck = new SpellCheck(myConfig);
    spellCheck.setSuggester(mySuggester);
    spellCheck.setSuggestionLimit(5);
    spellCheck.setText(text);
    spellCheck.check();
    while (spellCheck.hasMisspelt()) {
      processMisspell(text, highlighter, spellCheck);
      spellCheck.checkNext();
    }
  }

  private void processMisspell(String text, Highlighter highlighter, SpellCheck spellCheck) {
    int offset = spellCheck.getMisspeltOffset();
    int length = spellCheck.getMisspeltLength();
    if (isIgnored(text, offset, length)) return;
    @SuppressWarnings("unchecked")
    ArrayList<Suggestion> suggestions = spellCheck.getSuggestions();
    if (ignoreSuggestions(spellCheck.getMisspelt(), suggestions)) return;
    try {
      Object tag = highlighter.addHighlight(offset, offset + length, myHighlighter);
      myHighlightTags.add(tag);
    } catch (BadLocationException e) {
      Log.error(e);
      return;
    }
    String misspelt = spellCheck.getMisspelt();
    if (misspelt != null) {
      myMisspells.add(Misspell.create(offset, length, suggestions, misspelt));
    }
  }

  private boolean ignoreSuggestions(String word, ArrayList<Suggestion> suggestions) {
    word = Util.lower(word);
    for (Suggestion suggestion : suggestions) {
      String suggested = suggestion.getWord();
      if (word.length() != suggested.length()) continue;
      String lower = Util.lower(suggested);
      if (word.equals(lower)) return true;
    }
    return false;
  }

  private boolean isIgnored(String text, int offset, int length) {
    for (IgnoreSpellCheck ignore : myIgnores) if (ignore.shouldIgnore(text, offset, length)) return true;
    return false;
  }

  public static TextSpellChecker getInstance(JTextComponent component) {
    return Util.castNullable(TextSpellChecker.class, component.getClientProperty(COMPONENT_PROPERTY));
  }

  public static class Misspell {
    private final String[] mySuggestions;
    private final int myOffset;
    private final int myLength;
    private final String myMisspelt;

    public Misspell(String[] suggestions, int offset, int length, String misspelt) {
      mySuggestions = suggestions;
      myOffset = offset;
      myLength = length;
      myMisspelt = misspelt;
    }

    public static Misspell create(int offset, int length, List<Suggestion> suggestions, String misspelt) {
      String[] variants = new String[suggestions.size()];
      for (int i = 0; i < suggestions.size(); i++) {
        Suggestion suggestion = suggestions.get(i);
        variants[i] = suggestion.getWord();
      }
      return new Misspell(variants, offset, length, misspelt);
    }

    public boolean containsOffset(int offset) {
      return myOffset <= offset && (myOffset + myLength) > offset;
    }

    public List<String> getSuggestions() {
      return Collections15.unmodifiableListCopy(mySuggestions);
    }

    public void replace(Document document, String suggestion) throws BadLocationException {
      document.remove(myOffset, myLength);
      document.insertString(myOffset, suggestion, null);
    }

    public String getMisspelt() {
      return myMisspelt;
    }
  }

  private static class MyDocumentListener implements DocumentListener, Runnable {
    private final Lifespan myLife;
    private final TextSpellChecker myChecker;
    private final AtomicBoolean myUpdateRequested = new AtomicBoolean(false);

    public MyDocumentListener(Lifespan life, TextSpellChecker checker) {
      myLife = life;
      myChecker = checker;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      requestCheck();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      requestCheck();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      requestCheck();
    }

    public void requestCheck() {
      if (!ensureRunning()) return;
      if (!myUpdateRequested.compareAndSet(false, true)) return;
      SwingUtilities.invokeLater(this);
    }

    @Override
    public void run() {
      try {
        if (!ensureRunning()) return;
        myChecker.recheck();
      } catch (SuggesterException e) {
        Log.error(e);
      } finally {
        myUpdateRequested.set(false);
      }
    }

    private boolean ensureRunning() {
      return !myLife.isEnded();
    }
  }
}
