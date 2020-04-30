package com.almworks.spellcheck;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.components.ACheckboxList;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.FileSelectionField;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.ComponentEnabler;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;

class SpellCheckSettings {
  public static final LocalizedAccessor.Value M_TITLE = SpellCheckManager.I18N.getFactory("spellcheck.settings.window.title");

  private static final CanvasRenderer<SpellCheckerConfig.DicInfo> DICTIONARY_RENDERER = new CanvasRenderer<SpellCheckerConfig.DicInfo>() {
    @Override
    public void renderStateOn(CellState state, Canvas canvas, SpellCheckerConfig.DicInfo item) {
      canvas.appendText(item.getDisplayName());
    }
  };
  public static final LocalizedAccessor.Value M_CHOOSE_FILE = SpellCheckManager.I18N.getFactory("spellcheck.settings.userDictionary.file.choose.name");

  private JCheckBox myEnableCheck;
  private JPanel myWholePanel;
  private ACheckboxList<SpellCheckerConfig.DicInfo> myDictionaries;
  private JCheckBox myUseUser;
  private FileSelectionField myUserDictionaryFile;
  private JLabel myDictionariesLabel;

  public SpellCheckSettings() {
    ComponentEnabler.create(myEnableCheck, myDictionariesLabel, myDictionaries, myUseUser, myUserDictionaryFile);
    ComponentEnabler.create(myUseUser, myUserDictionaryFile);
    myDictionaries.setCanvasRenderer(DICTIONARY_RENDERER);
    myUserDictionaryFile.setActionName(M_CHOOSE_FILE.create());
  }

  public static void showDialog(DialogManager manager, final SpellCheckerConfig config) {
    DialogBuilder builder = manager.createBuilder("spellChecker.settings");
    builder.setTitle(M_TITLE.create());
    final SpellCheckSettings settings = new SpellCheckSettings();
    settings.loadSettings(config);
    builder.setContent(settings.myWholePanel);
    builder.setEmptyOkAction();
    builder.setEmptyCancelAction();
    builder.addOkListener(new AnActionListener() {
      @Override
      public void perform(ActionContext context) throws CantPerformException {
        settings.applyTo(config);
      }
    });
    builder.setModal(false);
    builder.showWindow();
  }

  private void applyTo(SpellCheckerConfig config) {
    config.updateSettings(myEnableCheck.isSelected(), myUseUser.isSelected(), myUserDictionaryFile.getField().getText(), myDictionaries.getCheckedAccessor().getSelectedItems());
  }

  private void loadSettings(SpellCheckerConfig config) {
    myEnableCheck.setSelected(config.isEnabled());
    myUseUser.setSelected(config.isUserDictionaryEnabled());
    myUserDictionaryFile.getField().setText(config.getUserDictionaryPath());
    ArrayList<SpellCheckerConfig.DicInfo> dictionaries = Collections15.arrayList(config.getAllDictionaries());
    Collections.sort(dictionaries, SpellCheckerConfig.DicInfo.ORDER);
    myDictionaries.setCollectionModel(FixedListModel.create(dictionaries));
    myDictionaries.getCheckedAccessor().setSelected(config.getEnabledDictionaries());
  }
}
