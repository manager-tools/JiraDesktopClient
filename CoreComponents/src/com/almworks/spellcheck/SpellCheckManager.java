package com.almworks.spellcheck;

import com.almworks.api.misc.WorkArea;
import com.almworks.spellcheck.util.IgnoreSpellChecks;
import com.almworks.spellcheck.util.IgnoreTypingWord;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.plaf.linux.LinuxPatches;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.properties.Role;
import com.softcorporation.suggester.Suggester;
import com.softcorporation.suggester.util.SpellCheckConfiguration;
import com.softcorporation.suggester.util.SuggesterException;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import javax.swing.text.JTextComponent;
import java.util.List;

/**
 * Distribution files:<br>
 * <b>Folder "suggester"</b><br>
 * "dictionary.txt" - dictionary of additional general-purpose words and abbreviations. JC doesn't add words here. User may override this file in workspace folder<br>
 * "*.jar" - bundled suggester's dictionaries<br><br>
 * <b>Folder "workspace":</b><br>
 * "dictionary.txt" - user's dictionary. Contains words added by user
 */
public class SpellCheckManager implements Startable {
  public static final Role<SpellCheckManager> ROLE = Role.role("spellchecker", SpellCheckManager.class);

  static final String DICTIONARY_FILE = "dictionary.txt";
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(SpellCheckManager.class.getClassLoader(), "com/almworks/spellcheck/message");

  private final List<RunningChecker> myCheckers = Collections15.arrayList();
  private final SpellCheckerConfig myConfig;
  private Suggester mySuggester;

  public SpellCheckManager(WorkArea workArea, Configuration config) {
    myConfig = new SpellCheckerConfig(this, config, workArea);
  }

  public void attachSpellChecker(final Lifespan life, final JTextComponent component) {
    if (life.isEnded()) return;
    final SpellCheckConfiguration config = myConfig.getCheckerConfig();
    Suggester dictionary;
    RunningChecker checker = new RunningChecker(life, component, this);
    checker.init();
    synchronized (myCheckers) {
      myCheckers.add(checker);
      dictionary = mySuggester;
    }
    life.add(checker);
    if (config != null && dictionary != null) checker.start(config, dictionary);
  }

  @Override
  public void start() {
    myConfig.updateState();
  }

  SpellCheckerConfig getConfig() {
    return myConfig;
  }

  void update(@Nullable Suggester suggester) {
    RunningChecker[] checkers;
    synchronized (myCheckers) {
      mySuggester = suggester;
      checkers = myCheckers.toArray(new RunningChecker[myCheckers.size()]);
    }
    for (RunningChecker checker : checkers) checker.stop();
    SpellCheckConfiguration config = myConfig.getCheckerConfig();
    if (config != null && suggester != null)
      for (RunningChecker checker : checkers) checker.start(config, suggester);
  }

  @Override
  public void stop() {}

  private void removeChecker(RunningChecker checker) {
    synchronized (myCheckers) {
      myCheckers.remove(checker);
    }
  }

  public static void attach(Lifespan life, JTextComponent component) {
    SpellCheckManager manager = Context.get(ROLE);
    if (manager != null) manager.attachSpellChecker(life, component);
  }

  @Nullable
  public UserDictionary getUserDictionary() {
    return myConfig.getUserDictionary();
  }

  public SimpleModifiable getModifiable() {
    return myConfig.getModifiable();
  }

  public void onDictionaryChanged() {
    RunningChecker[] checkers;
    synchronized (myCheckers) {
      checkers = myCheckers.toArray(new RunningChecker[myCheckers.size()]);
    }
    for (RunningChecker checker : checkers) checker.requestRecheck();
  }

  private static class RunningChecker extends Detach {
    private final Lifespan myLife;
    private final JTextComponent myComponent;
    private final SpellCheckManager myManager;
    private final Lifecycle myLifecycle = new Lifecycle(false);
    private volatile TextSpellChecker myChecker = null;

    private RunningChecker(Lifespan life, JTextComponent component, SpellCheckManager manager) {
      myLife = life;
      myComponent = component;
      myManager = manager;
      myLife.add(myLifecycle.getDisposeDetach());
    }

    @Override
    protected void doDetach() throws Exception {
      myManager.removeChecker(this);
    }

    public void start(SpellCheckConfiguration config, Suggester suggester) {
      if (myLife.isEnded()) return;
      if (!myLifecycle.cycleStart()) return;
      try {
        myChecker = TextSpellChecker.install(myLifecycle.lifespan(), myComponent, config, suggester);
        myChecker.addIgnoreSpellCheck(IgnoreSpellChecks.ALL_CAPITALIZE);
        myChecker.addIgnoreSpellCheck(IgnoreSpellChecks.HUMAN_NAME);
        IgnoreTypingWord.attach(myChecker);
      } catch (SuggesterException e) {
        LogHelper.error(e);
      }
    }

    public void stop() {
      myChecker = null;
      myLifecycle.cycleEnd();
    }

    public void init() {
      LinuxPatches.removeTextMenus(myComponent);
      SpellCheckActions.install(myLife, myComponent);
    }

    public void requestRecheck() {
      TextSpellChecker checker = myChecker;
      if (checker != null && !myLife.isEnded()) checker.forceRecheck();
    }
  }
}
