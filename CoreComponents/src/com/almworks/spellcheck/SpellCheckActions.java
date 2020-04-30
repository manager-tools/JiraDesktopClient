package com.almworks.spellcheck;

import com.almworks.api.gui.MainMenu;
import com.almworks.spellcheck.util.CorrectMisspellListener;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.PopupMenuListener;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;

class SpellCheckActions {
  private static final LocalizedAccessor.Value M_SAVE_TO_DICTIONARY_NAME = SpellCheckManager.I18N.getFactory("spellcheck.userDic.action.add.name");
  public static final SimpleAction NO_SUGGESTIONS = new SimpleAction(SpellCheckManager.I18N.getFactory("spellcheck.actions.noSuggestions.name"), null) {
    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(EnableState.DISABLED);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
    }
  };

  public static void install(Lifespan life, JTextComponent component) {
    new PopupMenuListener() {
      @Override
      protected void showPopup(JComponent component, int x, int y, InputEvent event) {
        openPopup(component, x, y, event);
      }
    }.attach(life, component);
  }

  private static void openPopup(JComponent c, int x, int y, InputEvent event) {
    JTextComponent component = Util.castNullable(JTextComponent.class, c);
    if (component == null) return;
    TextSpellChecker.Misspell misspell = CorrectMisspellListener.getMisspell(component, new Point(x, y));
    if (misspell == null) return;
    MenuBuilder builder = new MenuBuilder();
    List<String> suggestions = misspell.getSuggestions();
    if (suggestions.isEmpty()) builder.addAction(NO_SUGGESTIONS);
    else for (String suggestion : suggestions) builder.addAction(new AReplaceMisspellAction(misspell, component, suggestion));
    builder.addSeparator();
    builder.addAction(new AddToDictionaryAction(misspell.getMisspelt()));
    builder.addAction(MainMenu.Tools.SPELL_CHECKER_SETTINGS);
    builder.showPopupMenu(c, x, y, event);
  }

  private static class AReplaceMisspellAction extends SimpleAction {
    private final TextSpellChecker.Misspell myMisspell;
    private final JTextComponent myComponent;
    private final String mySuggestion;

    public AReplaceMisspellAction(TextSpellChecker.Misspell misspell, JTextComponent component, String suggestion) {
      super(suggestion);
      myMisspell = misspell;
      myComponent = component;
      mySuggestion = suggestion;
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      try {
        myMisspell.replace(myComponent.getDocument(), mySuggestion);
      } catch (BadLocationException e1) {
        Log.error(e1);
      }
    }
  }

  private static class AddToDictionaryAction extends SimpleAction {
    private final String myWord;

    private AddToDictionaryAction(String word) {
      super(M_SAVE_TO_DICTIONARY_NAME, null);
      myWord = word;
      watchRole(SpellCheckManager.ROLE);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      SpellCheckManager manager = context.getSourceObject(SpellCheckManager.ROLE);
      context.updateOnChange(manager.getModifiable());
      CantPerformException.ensureNotNull(manager.getUserDictionary());
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      SpellCheckManager manager = context.getSourceObject(SpellCheckManager.ROLE);
      UserDictionary dictionary = CantPerformException.ensureNotNull(manager.getUserDictionary());
      dictionary.addWord(myWord);
    }
  }
}
