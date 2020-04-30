package com.almworks.util.i18n.text.statements;

import com.almworks.util.LogHelper;
import com.almworks.util.commons.Factory;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IntStatement {
  private static final Map<String, LangSupport> LANGUAGES;
  static {
    HashMap<String,LangSupport> map = Collections15.hashMap();
    map.put("en", new English());
    LANGUAGES = Collections.unmodifiableMap(map);
  }
  private final LocalizedAccessor myAccessor;

  public IntStatement(LocalizedAccessor accessor) {
    myAccessor = accessor;
  }

  public Factory<String> message(final int value) {
    return new Factory<String>() {
      @Override
      public String create() {
        String lang = myAccessor.getCurrentLocale().getLanguage();
        LangSupport support = LANGUAGES.get(lang);
        LogHelper.assertError(support != null, "Unknown language", lang);
        return support != null ? support.format(myAccessor, value) : "";
      }
    };
  }

  private interface LangSupport {
    void check(LocalizedAccessor accessor);

    String format(LocalizedAccessor accessor, int value);
  }

  private static class English implements LangSupport {
    @Override
    public void check(LocalizedAccessor accessor) {
      accessor.getFactory("single");
      accessor.getFactory("plural");
    }

    @Override
    public String format(LocalizedAccessor accessor, int value) {
      MessageFormat format;
      if (Math.abs(value) == 1) format = new MessageFormat(accessor.getString("single"));
      else format = new MessageFormat(accessor.getString("plural"));
      return format.format(value);
    }
  }
}
