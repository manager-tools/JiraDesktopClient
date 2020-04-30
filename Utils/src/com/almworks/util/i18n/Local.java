package com.almworks.util.i18n;

import com.almworks.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Locale;

public class Local {
  private static final Locale DEFAULT_LOCALE = Locale.getDefault();
  private static final LocalBook DEFAULT_BOOK = new DefaultLocalBook();

  private static LocalBook myBook = DEFAULT_BOOK;
  private static Locale myLocale = DEFAULT_LOCALE;

  private static final String MNEMONIC_PREFIX = "mnemonic=";
  private static final String NOT_EXISTENT = "$$not$$.$$existent$$";

  public static Locale setLocale(Locale locale) {
    Locale oldLocale = myLocale;
    myLocale = locale == null ? DEFAULT_LOCALE : locale;
    return oldLocale;
  }

  public static LocalBook setBook(LocalBook book) {
    LocalBook oldBook = myBook;
    myBook = book == null ? DEFAULT_BOOK : book;
    return oldBook;
  }

  public static LocalBook getBook() {
    return myBook;
  }

  @NotNull
  public static String text(@NotNull String key) {
    return text(key, key, myLocale);
  }

  @NotNull
  public static String text(@NotNull String key, @NotNull String defaultText) {
    return text(key, defaultText, myLocale);
  }

  @NotNull
  public static String text(@NotNull String key, @NotNull String defaultText, @NotNull Locale locale) {
    String text = myBook.get(key, locale);
    String raw = text == null ? defaultText : text;
    String result = process(key, raw);
    return result;
  }

  @Nullable
  public static String textOrNull(@NotNull String key) {
    return textOrNull(key, myLocale);
  }

  @Nullable
  public static String textOrNull(@NotNull String key, @NotNull Locale locale) {
    String text = myBook.get(key, locale);
    if (text == null)
      return null;
    return process(key, text);
  }

  public static void updateComponents(Component component, ComponentLocalizer localizer) {
    UIUtil.updateComponents(component, new ComponentLocalizerVisitor(localizer));
  }

  @NotNull
  public static String parse(@NotNull String raw) {
    return process(NOT_EXISTENT, raw);
  }

  /**
   * Replaces all references in raw with values.
   */
  static String process(String key, String raw) {
    int dollar = raw.indexOf('$');
    if (dollar < 0)
      return raw;
    StringBuffer processed = new StringBuffer();
    StringBuffer name = new StringBuffer();
    int mpLength = MNEMONIC_PREFIX.length();

    boolean sName = false;
    boolean sDollar = false;
    char defaultMnemonic = 0;
    int length = raw.length();
    for (int i = 0; i < length; i++) {
      char c = raw.charAt(i);
      if (!sName) {
        if (c == '$') {
          sDollar = !sDollar;
          if (!sDollar)
            processed.append('$');
        } else if (c == '(' && sDollar) {
          sDollar = false;
          sName = true;
          name.setLength(0);
          defaultMnemonic = 0;
        } else {
          if (sDollar)
            processed.append('$');
          sDollar = false;
          processed.append(c);
        }
      } else {
        if (c == ')') {
          String insert = name.toString();
          if (isValidPropertyName(insert)) {
            String replacement = text(insert);
            boolean hasMnemonic = replacement.indexOf('&') >= 0;
            if (!hasMnemonic && defaultMnemonic != 0) {
              int idx = replacement.indexOf(Character.toUpperCase(defaultMnemonic));
              if (idx < 0)
                idx = replacement.indexOf(Character.toLowerCase(defaultMnemonic));
              if (idx >= 0) {
                replacement = replacement.substring(0, idx) + '&' + replacement.substring(idx);
              }
            }
            processed.append(replacement);
          } else {
            processed.append("$(").append(insert).append(')');
          }
          sName = false;
        } else if (c == '!' && i + 1 + mpLength <= length &&
          MNEMONIC_PREFIX.equalsIgnoreCase(raw.substring(i + 1, i + 1 + mpLength)))
        {
          i += 1 + mpLength;
          c = raw.charAt(i);
          defaultMnemonic = c;
        } else {
          name.append(c);
        }
      }
    }
    if (sName) {
      processed.append("$(").append(name);
    }
    if (sDollar) {
      processed.append('$');
    }
    return processed.toString();
  }

  private static boolean isValidPropertyName(String name) {
    int length = name.length();
    for (int i = 0; i < length; i++) {
      char c = name.charAt(i);
      if (c != '.' && !Character.isLetterOrDigit(c) && c != '_' && c != '$')
        return false;
    }
    return true;
  }
}
