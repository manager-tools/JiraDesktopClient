package com.almworks.util.components.plaf.patches;

import com.almworks.util.Env;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EasternCharsetsOnWindowsPatch extends WindowsLAFPatch {
  private static final String ARIAL_UNICODE = "Arial Unicode MS";
  public static final String FORCE_SETTING = "arial.unicode";

  private final Set<String> myApplied = Collections15.hashSet();
  private static final String[] EASTERN_LANGUAGES = {"zh", "ace", "chi", "zho", "ja", "jpn", "ko", "kor", "ka", "kat", "geo"};
  private static final String[] EASTERN_COUNTRIES = {"CN", "TW", "JP", "KR", "GE"};

  public void install(LookAndFeel laf) {
    UIDefaults defaults = defaults();
    Set<String> fontKeys = Collections15.hashSet();
    Enumeration<Object> keys = defaults.keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      if (key instanceof String) {
        String keyName = ((String) key);
        if (defaults.getFont(key) != null) {
          fontKeys.add(keyName);
        }
      }
    }
    Map<Font, Font> transition = Collections15.hashMap();
    for (String key : fontKeys) {
      if (!key.startsWith(BACKUP)) {
        Font font = defaults.getFont(key);
        if (font != null) {
          Font substitute = transition.get(font);
          if (substitute == null) {
            substitute = getSubstitute(font);
            if (substitute != null)
              transition.put(font, substitute);
          }
          if (substitute != null) {
            Log.debug("changing " + key + " to " + substitute);
            save(key);
            myApplied.add(key);
            defaults.put(key, substitute);
          }
        }
      }
    }
    transition.clear();
  }

  private Font getSubstitute(Font font) {
    return new FontUIResource(ARIAL_UNICODE, font.getStyle(), font.getSize());
  }

  public void uninstall(LookAndFeel laf) {
    for (Object key : myApplied) {
      assert key instanceof String;
      if (key instanceof String)
        restore((String) key);
    }
    myApplied.clear();
  }

  public boolean isExtendingLookAndFeel(LookAndFeel laf) {
    boolean forced = false;
    String s = Env.getString(FORCE_SETTING);
    if (s != null) {
      forced = Env.getBoolean(FORCE_SETTING);
      if (!forced)
        return false;
    }
    Font f = Font.decode(ARIAL_UNICODE);
    if (f == null || !ARIAL_UNICODE.equalsIgnoreCase(f.getName())) {
      if (forced) {
        Log.warn("cannot install font " + ARIAL_UNICODE + ": " + f);
        return false;
      }
    }
    if (forced)
      return true;
    if (!super.isExtendingLookAndFeel(laf)) {
      // not windows
      return false;
    }
    Locale locale = Locale.getDefault();
    String country = locale.getCountry();
    String language = locale.getLanguage();
    if (language != null) {
      for (String lang : EASTERN_LANGUAGES) {
        if (language.equalsIgnoreCase(new Locale(lang, "", "").getLanguage()))
          return true;
      }
    }
    if (country != null) {
      for (String c : EASTERN_COUNTRIES) {
        if (country.equalsIgnoreCase(c))
          return true;
      }
    }
    return false;
  }
}
