package com.almworks.spellcheck.util;

import com.almworks.spellcheck.IgnoreSpellCheck;
import com.almworks.spellcheck.TextSpellChecker;
import com.almworks.util.LogHelper;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.detach.Detach;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class IgnoreTypingWord implements IgnoreSpellCheck, CaretListener, DocumentListener {
  public static final int MAX_WORD_LENGTH = 50;
  private final TextSpellChecker myChecker;
  private int myPosition = -1;

  public IgnoreTypingWord(TextSpellChecker checker) {
    myChecker = checker;
  }

  public static void attach(final TextSpellChecker checker) {
    final IgnoreTypingWord ignore = new IgnoreTypingWord(checker);
    final JTextComponent component = checker.getComponent();
    component.addCaretListener(ignore);
    DocumentUtil.addListener(checker.getLife(), component.getDocument(), ignore);
    checker.getLife().add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        component.removeCaretListener(ignore);
      }
    });
    checker.addIgnoreSpellCheck(ignore);
  }

  @Override
  public boolean shouldIgnore(String text, int offset, int length) {
    return offset <= myPosition && (offset + length) >= myPosition;
  }

  private void clearWord() {
    if (myPosition >= 0) {
      myPosition = -1;
      myChecker.forceRecheck();
    }
  }

  @Override
  public void caretUpdate(CaretEvent e) {
    if (myPosition < 0) return;
    int dot = e.getDot();
    if (dot == myPosition) return;
    if (Math.abs(myPosition - dot) > MAX_WORD_LENGTH) {
      clearWord();
      return;
    }
    Document document = myChecker.getComponent().getDocument();
    int min = Math.min(dot, myPosition);
    int max = Math.max(dot, myPosition);
    String text = null;
    try {
      text = document.getText(min, max - min);
    } catch (BadLocationException e1) {
      LogHelper.warning(e1); // May be caused by setting new text to the editor
      clearWord();
      return;
    }
    for (int i = 0; i < text.length(); i++)
      if (!Character.isLetter(text.charAt(i))) {
        clearWord();
        return;
      }
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    if (e.getLength() == 1) myPosition = e.getOffset() + 1;
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    myPosition = e.getOffset();
  }

  @Override
  public void changedUpdate(DocumentEvent e) {

  }
}
