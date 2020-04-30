package com.almworks.util;

import org.almworks.util.Util;

/**
 * :todoc:
 *
 * @author sereda
 */
public class English {
  public static String are(int count) {
    return count == 1 ? "is" : "are";
  }

  // todo ...tum => ...ta (datum => data) -- need to check
  // todo ...s => ...ses (status => statuses) -- need to check
  // todo what if the word already is in plural form?
  public static String getPlural(String noun) {
    int length = noun.length();
    if (length == 0)
      return noun;
    char lastChar = noun.charAt(length - 1);
    if (length > 1 && (lastChar == 'y' || lastChar == 'Y'))
      return noun.substring(0, length - 1) + "ies";
    else
      return noun + 's';
  }

  /**
   * @param word The word.
   * @return a/an article plus the word, as appropriate:
   * a("bug") => "a bug"; a("issue") => "an issue".
   */
  public static String a(String word) {
    // todo: there are: "a user", "an honor", etc.
    final String article;
    if("aeiou".indexOf(Character.toLowerCase(word.charAt(0))) >= 0) {
      article = "an ";
    } else {
      article = "a ";
    }
    return article + word;
  }

  // todo #1211
  public static String getSingularOrPlural(String noun, int count) {
    return count == 1 ? noun : getPlural(noun);
  }

  public static String numberOf(int count, String noun) {
    return count + " " + getSingularOrPlural(noun, count);
  }

  public static String capitalize(String word) {
    if (word == null || word.length() < 2)
      return word;
    return Util.upper(word.substring(0, 1)) + Util.lower(word.substring(1));
  }

  public static String have(int count) {
    return count == 1 ? "has" : "have";
  }

  public static String verb(String verb, int count) {
    return count == 1 ? verb + "s" : verb;
  }

  public static String were(int count) {
    return count == 1 ? "was" : "were";
  }

  /**
   * This method takes typical enumerable name (ENUMERABLE_NAME) and makes it displayable (Enumerable Name).
   */
  public static String humanizeEnumerable(Enumerable enumerable) {
    if (enumerable == null)
      return "";
    String name = enumerable.getName();
    return humanizeEnumerable(name);
  }

  public static String humanizeEnumerable(String text) {
    return capitalizeWords(text, "[\\s_]+");
  }

  public static String capitalizeWords(String words) {
    return capitalizeWords(words, "\\s+");
  }

  private static String capitalizeWords(String words, String regex) {
    if (words == null)
      return words;
    String[] strings = words.split(regex);
    StringBuffer result = new StringBuffer();
    for (String s : strings) {
      if (result.length() > 0)
        result.append(' ');
      result.append(capitalize(s));
    }
    return result.toString();
  }
}
