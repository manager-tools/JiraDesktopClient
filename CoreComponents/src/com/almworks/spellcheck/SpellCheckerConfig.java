package com.almworks.spellcheck;

import com.almworks.api.misc.WorkArea;
import com.almworks.spellcheck.dictionary.AlmSuggester;
import com.almworks.spellcheck.dictionary.FixedDictionary;
import com.almworks.spellcheck.dictionary.MultiDictionarySuggester;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.InterruptableRunnable;
import com.softcorporation.suggester.Suggester;
import com.softcorporation.suggester.engine.core.Dictionary;
import com.softcorporation.suggester.util.SpellCheckConfiguration;
import com.softcorporation.suggester.util.SuggesterException;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

class SpellCheckerConfig {
  private static final String ENABLED = "enabled";
  private static final String ENABLE_USER = "enableUser";
  private static final String USER_DICTIONARY_PATH = "userDictionaryPath";
  private static final String ENABLED_DICTIONARIES = "enabledDictionaries";
  private static final String S_NO_DICTIONARIES = "-none-";

  private static final String ETC_COLLECTION = "suggester";

  /**
   * Synthetic dictionary to point to bundled JC dictionary
   */
  private static final DicInfo JARGON_DICTIONARY = new DicInfo("-jargon-", null, "Jira Jargon", "");
  private static final DicInfo DEFAULT_DICTIONARY = new DicInfo("english.jar", "english", "English", "en");
  public static final List<DicInfo> DICTIONARIES = Collections15.unmodifiableListCopy(
    JARGON_DICTIONARY,
    DEFAULT_DICTIONARY,
    new DicInfo("russian.jar", "russian", "Русский", "ru"),
//    new DicInfo("cz.jar", "cz", "Čeština", "cs"),
    new DicInfo("de.jar", "de", "Deutsch", "de"),
    new DicInfo("es.jar", "es", "Español", "es"),
    new DicInfo("fr.jar", "fr", "Français", "fr")//,
//    new DicInfo("pl.jar", "pl", "Polski", "pl")
  );

  private final SpellCheckManager myManager;
  private final Configuration myConfig;
  private final WorkArea myWorkArea;
  private final SpellCheckConfiguration myCheckerConfig = new SpellCheckConfiguration();
  private final AtomicReference<State> myCurrentState = new AtomicReference<State>(State.DISABLED);
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  SpellCheckerConfig(SpellCheckManager manager, Configuration config, WorkArea workArea) {
    myManager = manager;
    myConfig = config;
    myWorkArea = workArea;
    myCheckerConfig.SEARCH_JOINED = false;
    myCheckerConfig.DELIMITERS = myCheckerConfig.DELIMITERS + myCheckerConfig.DELIMITERS_JOINED;
    myCheckerConfig.DELIMITERS_JOINED = "";
  }

  public boolean isEnabled() {
    return myConfig.getBooleanSetting(ENABLED, true);
  }

  public boolean isUserDictionaryEnabled() {
    return myConfig.getBooleanSetting(ENABLE_USER, true);
  }

  public String getUserDictionaryPath() {
    return myConfig.getSetting(USER_DICTIONARY_PATH, new File(myWorkArea.getRootDir(), SpellCheckManager.DICTIONARY_FILE).getAbsolutePath());
  }

  public Collection<DicInfo> getAllDictionaries() {
    return DICTIONARIES;
  }

  @NotNull
  public Collection<DicInfo> getEnabledDictionaries() {
    List<String> settings = myConfig.getAllSettings(ENABLED_DICTIONARIES);
    if (settings.size() == 1 && S_NO_DICTIONARIES.equals(settings.get(0))) return Collections.emptyList();
    Set<DicInfo> infos = Collections15.hashSet();
    for (String dicFile : settings) {
      DicInfo info = findByFile(dicFile);
      if (info != null) infos.add(info);
    }
    return infos.isEmpty() ? Arrays.asList(DEFAULT_DICTIONARY, JARGON_DICTIONARY) : infos;
  }

  public boolean isDictionaryEnabled(DicInfo info) {
    return getEnabledDictionaries().contains(info);
  }

  private DicInfo findByFile(String dicFile) {
    for (DicInfo info : DICTIONARIES) {
      if (info.getEtcCollectionFile().equals(dicFile)) return info;
    }
    return null;
  }

  public void updateSettings(boolean enable, boolean user, String userPath, Collection<DicInfo> dictionaries) {
    myConfig.setSetting(ENABLED, enable);
    myConfig.setSetting(ENABLE_USER, user);
    myConfig.setSetting(USER_DICTIONARY_PATH, userPath);
    List<String> files = Collections15.arrayList();
    for (DicInfo info : dictionaries) files.add(info.getEtcCollectionFile());
    myConfig.setSettings(ENABLED_DICTIONARIES, files.isEmpty() ? Collections.singletonList(S_NO_DICTIONARIES) : files);
    updateState();
  }

  void updateState() {
    if (myCurrentState.get().matchesConfig(this)) return;
    ThreadGate.executeLong(new InterruptableRunnable() {
      @Override
      public void run() throws InterruptedException {
        State.load(SpellCheckerConfig.this, myCurrentState.get());
      }
    });
  }

  public SpellCheckConfiguration getCheckerConfig() {
    return myCheckerConfig;
  }

  private void stateLoaded(State state) {
    if (!state.matchesConfig(this)) {
      LogHelper.warning("State does not match spell checker config");
      return;
    }
    myCurrentState.set(state);
    myManager.update(state.createSuggester(myCheckerConfig));
    myModifiable.fireChanged();
  }

  public UserDictionary getUserDictionary() {
    return myCurrentState.get().myUserDictionary;
  }

  public SimpleModifiable getModifiable() {
    return myModifiable;
  }

  public static class DicInfo {
    public static final Comparator<DicInfo> ORDER = new Comparator<DicInfo>() {
      @Override
      public int compare(DicInfo o1, DicInfo o2) {
        int langOrder = o1.myLangCode.compareTo(o2.myLangCode);
        if (langOrder != 0) return langOrder;
        return o1.myDisplayName.compareTo(o2.myDisplayName);
      }
    };

    private final String myEtcCollectionFile;
    private final String myDictionaryName;
    private final String myDisplayName;
    private final String myLangCode;

    public DicInfo(String etcCollectionFile, String dictionaryName, String displayName, String langCode) {
      myEtcCollectionFile = etcCollectionFile;
      myDictionaryName = dictionaryName;
      myDisplayName = displayName;
      myLangCode = langCode;
    }

    public String getDisplayName() {
      return myDisplayName;
    }

    public String getEtcCollectionFile() {
      return myEtcCollectionFile;
    }

    @Nullable
    public Dictionary load(WorkArea workArea) {
      File dicFile = workArea.getEtcCollectionFile(ETC_COLLECTION, myEtcCollectionFile);
      try {
        if (dicFile != null) return BasicDictionary.loadFromFile(dicFile.getAbsolutePath(), myDictionaryName);
        else LogHelper.warning("Dictionary not found:", myDictionaryName, myEtcCollectionFile);
      } catch (SuggesterException e) {
        LogHelper.error("Error loading", myEtcCollectionFile, e);
      }
      return null;
    }
  }

  private static class State {
    public static final State DISABLED = new State(null, null, null);
    private final Map<DicInfo, Dictionary> myDictionaries = Collections15.hashMap();
    @Nullable
    private final AlmSuggester.AlmDictionary myBundledDictionary;
    @Nullable
    private final String myUserDictionaryPath;
    @Nullable
    private final UserDictionary myUserDictionary;

    private State(@Nullable AlmSuggester.AlmDictionary bundledDictionary, @Nullable String userDictionaryPath, @Nullable UserDictionary userDictionary) {
      myBundledDictionary = bundledDictionary;
      myUserDictionaryPath = userDictionaryPath;
      myUserDictionary = userDictionary;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean matchesConfig(SpellCheckerConfig config) {
      Collection<DicInfo> dictionaries = getDictionaries(config);
      if (dictionaries.size() != myDictionaries.size()) return false;
      for (DicInfo dictionary : dictionaries) if (!myDictionaries.containsKey(dictionary)) return false;
      if ((myBundledDictionary == null) == isBundled(config)) return false;
      if ((myUserDictionary == null == isUser(config))) return false;
      return myUserDictionary == null || Util.equals(config.getUserDictionaryPath(), myUserDictionaryPath);
    }

    private static boolean isUser(SpellCheckerConfig config) {
      return config.isEnabled() && config.isUserDictionaryEnabled();
    }

    private static boolean isBundled(SpellCheckerConfig config) {
      return config.isEnabled() && config.isDictionaryEnabled(JARGON_DICTIONARY);
    }

    private static Collection<DicInfo> getDictionaries(SpellCheckerConfig config) {
      boolean enabled = config.isEnabled();
      if (!enabled) return Collections15.emptyList();
      ArrayList<DicInfo> dicInfos = Collections15.arrayList(config.getEnabledDictionaries());
      dicInfos.remove(JARGON_DICTIONARY);
      return dicInfos;
    }

    public static void load(SpellCheckerConfig config, State prevState) {
      boolean needBundled = isBundled(config);
      String userDicPath = isUser(config) ? config.getUserDictionaryPath() : null;
      Collection<DicInfo> infos = getDictionaries(config);
      WorkArea workArea = config.myWorkArea;
      State state = new State(needBundled ? loadBundled(workArea, prevState) : null, userDicPath, loadUser(config, userDicPath, prevState));
      for (DicInfo info : infos) {
        Dictionary dictionary = prevState.myDictionaries.get(info);
        if (dictionary == null) dictionary = info.load(workArea);
        state.myDictionaries.put(info, dictionary);
      }
      config.stateLoaded(state);
    }

    private static UserDictionary loadUser(SpellCheckerConfig config, @Nullable String userDicPath, State prevState) {
      if (userDicPath == null) return null;
      UserDictionary dictionary = prevState.myUserDictionary;
      if (dictionary != null && Util.equals(userDicPath, prevState.myUserDictionaryPath)) return dictionary;
      return UserDictionary.create(config.myWorkArea, config.myManager);
    }

    private static AlmSuggester.AlmDictionary loadBundled(WorkArea workArea, State state) {
      AlmSuggester.AlmDictionary dictionary = state.myBundledDictionary;
      if (dictionary != null) return dictionary;
      File file = workArea.getEtcCollectionFile(ETC_COLLECTION, SpellCheckManager.DICTIONARY_FILE);
      if (file == null) return null;
      try {
        return FixedDictionary.loadUTF8File(file);
      } catch (IOException e) {
        LogHelper.error("Failed to load bundled dictionary", file);
        return null;
      }
    }

    @Nullable
    public Suggester createSuggester(SpellCheckConfiguration config) {
      Suggester suggester;
      try {
        suggester = MultiDictionarySuggester.create(config, myDictionaries.values());
      } catch (SuggesterException e) {
        LogHelper.error("Failed to create suggester", e, myDictionaries.values());
        suggester = null;
      }
      AlmSuggester.AlmDictionary user = myUserDictionary != null ? myUserDictionary.getDictionary() : null;
      return AlmSuggester.create(config, suggester, user, myBundledDictionary);
    }
  }
}
