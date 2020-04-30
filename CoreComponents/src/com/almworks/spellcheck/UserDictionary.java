package com.almworks.spellcheck;

import com.almworks.api.misc.WorkArea;
import com.almworks.spellcheck.dictionary.AlmSuggester;
import com.almworks.spellcheck.dictionary.WriteThroughDictionary;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.DialogsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

class UserDictionary implements Procedure<IOException> {
  private static final LocalizedAccessor.Value M_FAILURE_TITLE = SpellCheckManager.I18N.getFactory("spellcheck.userDic.saveFailure.title");
  private static final LocalizedAccessor.Value M_FAILURE_MESSAGE = SpellCheckManager.I18N.getFactory("spellcheck.userDic.saveFailure.message");
  @NotNull
  private final WriteThroughDictionary myDictionary;
  private final SpellCheckManager myManager;

  private UserDictionary(File file, SpellCheckManager manager) throws IOException {
    myManager = manager;
    myDictionary = WriteThroughDictionary.create(file, this);
  }

  @Nullable
  public static UserDictionary create(WorkArea workArea, SpellCheckManager manager) {
    File file = new File(workArea.getRootDir(), SpellCheckManager.DICTIONARY_FILE);
    try {
      return new UserDictionary(file, manager);
    } catch (IOException e) {
      LogHelper.error("Failed to create user dictionary", file);
      return null;
    }
  }

  @NotNull
  public AlmSuggester.AlmDictionary getDictionary() {
    return myDictionary;
  }

  @Override
  public void invoke(IOException arg) {
    DialogsUtil.showException(M_FAILURE_TITLE.create(), M_FAILURE_MESSAGE.create(), arg);
  }

  public void addWord(String word) {
    if (myDictionary.addWord(word)) myManager.onDictionaryChanged();
  }
}
