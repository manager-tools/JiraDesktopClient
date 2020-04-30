package com.almworks.spellcheck.util;

import com.almworks.spellcheck.TextSpellChecker;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.util.List;

public class ReplaceMisspellAction extends AbstractAction {
  private final TextSpellChecker.Misspell myMisspell;
  private final JTextComponent myComponent;
  private final String mySuggestion;

  public ReplaceMisspellAction(TextSpellChecker.Misspell misspell, JTextComponent component, String suggestion) {
    super(suggestion);
    myMisspell = misspell;
    myComponent = component;
    mySuggestion = suggestion;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      myMisspell.replace(myComponent.getDocument(), mySuggestion);
    } catch (BadLocationException e1) {
      Log.error(e1);
    }

  }

  @Nullable
  public static JPopupMenu createPopupMenu(JTextComponent component, TextSpellChecker.Misspell misspell) {
    JPopupMenu menu = new JPopupMenu();
    List<String> suggestions = misspell.getSuggestions();
    if (suggestions.isEmpty()) return null;
    for (String suggestion : suggestions) {
      JMenuItem item = new JMenuItem();
      item.setAction(new ReplaceMisspellAction(misspell, component, suggestion));
      menu.add(item);
    }
    return menu;
  }
}
