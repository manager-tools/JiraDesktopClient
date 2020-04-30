package com.almworks.util;

import org.almworks.util.Util;

public class L {

  public static String actionName(String actionName) {
    assert checkTitleCaps(actionName);
    return actionName;
  }

  public static String borderTitle(String title) {
    assert checkTitleCaps(title);
    return title;
  }

  public static String checkbox(String checkbox) {
    assert checkSentenceCaps(checkbox);
    return checkbox;
  }
  public static String content(String text) {
    assert checkSentenceCaps(text);
    return text;
  }

  public static String dialog(String dialogTitle) {
    assert checkTitleCaps(dialogTitle);
    return dialogTitle;
  }

  public static String frame(String frameTitle) {
    assert checkTitleCaps(frameTitle);
    assert checkApplicationName(frameTitle);
    return frameTitle;
  }

  public static String html(String html) {
    return html;
  }

  public static String listItem(String listItem) {
    assert checkSentenceCaps(listItem);
    return listItem;
  }

  public static String progress(String progress) {
    //assert checkSentenceCaps(progress); -?
    return progress;
  }

  public static String special(String special) {
    return special;
  }

  public static String tabName(String tab) {
    assert checkTitleCaps(tab);
    return tab;
  }

  public static String tableColumn(String columnName) {
    assert checkTitleCaps(columnName);
    return columnName;
  }

  public static String textTitle(String text) {
    assert checkTitleCaps(text);
    return text;
  }

  public static String tooltip(String tooltip) {
    // todo word count define rules?
    assert checkSentenceCaps(tooltip);
    return tooltip;
  }

  public static String treeNode(String node) {
    assert checkTitleCaps(node);
    return node;
  }

  private static boolean checkApplicationName(String frameTitle) {
//    assert frameTitle.equals(Local.text(Terms.key_Deskzilla)) || frameTitle.endsWith(" - " +  Local.text(Terms.key_Deskzilla)) : "frame";
    return true;
  }

  private static boolean checkSentenceCaps(String text) {
    if (text == null)
      return true;
    if (text.length() == 0)
      return true;
    char c = text.charAt(0);
    if (!Character.isLetter(c))
      return true;
    assert Character.isUpperCase(c) : "not uppercase : " + text;
    return true;
  }

  private static boolean checkTitleCaps(String title) {
    if (title == null)
      return true;
    if (title.length() == 0)
      return true;
    String[] words = title.split("\\s");
    if (words.length > 0)
      checkCapWord(words[0], true, title);
    if (words.length > 1)
      checkCapWord(words[words.length - 1], true, title);
    for (int i = 1; i < words.length - 1; i++)
      checkCapWord(words[i], false, title);
    return true;
  }

  private static void checkCapWord(String word, boolean always, String phrase) {
    assert word != null;
    int length = word.length();
    assert length > 0;
    char c = word.charAt(0);
    if (!Character.isLetter(c)) {
      // ignore non-letters
      return;
    }
    if (length > 1 && Util.upper(word).equals(word)) {
      // ignore acronyms
      return;
    }

    boolean actual = Character.isUpperCase(c);
    boolean needed;
    if (always) {
      needed = true;
    } else {
      if (isArticle(word))
        needed = false;
      else if (isCoordinatingConjunction(word))
        needed = false;
      else if (isPrepositionWithThreeOrLessLetters(word))
        needed = false;
      else
        needed = true;
    }

    assert needed == actual : "cap in " + word + " from " + phrase;
  }

  private static boolean isPrepositionWithThreeOrLessLetters(String word) {
    return
      "IN".equalsIgnoreCase(word)
      || "FOR".equalsIgnoreCase(word)
      || "ON".equalsIgnoreCase(word)
      || "AS".equalsIgnoreCase(word)
      || "TO".equalsIgnoreCase(word)
      || "AT".equalsIgnoreCase(word)
      || "BY".equalsIgnoreCase(word)
      || "OF".equalsIgnoreCase(word)
      || "OF".equalsIgnoreCase(word)
      || "FROM".equalsIgnoreCase(word);
  }

  private static boolean isCoordinatingConjunction(String word) {
    return
      "AND".equalsIgnoreCase(word)
      || "OR".equalsIgnoreCase(word)
      || "BUT".equalsIgnoreCase(word)
      || "SO".equalsIgnoreCase(word)
      || "NOR".equalsIgnoreCase(word);
  }

  private static boolean isArticle(String word) {
    return "A".equalsIgnoreCase(word) || "THE".equalsIgnoreCase(word) || "AN".equalsIgnoreCase(word);
  }

  public static String combobox(String comboboxItem) {
    assert checkSentenceCaps(comboboxItem);
    return comboboxItem;
  }
}

/*
// to seek unnotices strings:
public class L {
  public static String content(String text) {
    return "-text-";
  }

  public static String title(String header) {
    return "-title-";
  }

  public static String html(String html) {
    return "<html><body>-html-</body></html>";
  }

  public static String shortdesc(String tooltip) {
    return "-shortdesc-";
  }

  public static String action(String actionName) {
    return "-action-";
  }

  public static String window(String windowTitle) {
    return "-window-";
  }

  public static String checkbox(String checkbox) {
    return "-checkbox-";
  }

  public static String progress(String progress) {
    return "-progress-";
  }

  public static String special(String special) {
    return "-special-";
  }
}
*/

