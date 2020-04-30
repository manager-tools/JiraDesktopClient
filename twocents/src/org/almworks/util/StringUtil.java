package org.almworks.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

public class StringUtil {
  /** The preferred line separator in the host machine's OS ({@code \r\n} in Windows) */
  public static final String LOCAL_LINE_SEPARATOR = String.format("%n");

  private StringUtil() {
  }

  /**
   * Returns string's substring that ends before the last occurence of the separator. If separator is not encountered,
   * returns the whole string.
   *
   * @param string    the original string to look in.
   * @param separator a string that is looked for
   * @return substring of a string that ends before last occurence of the separator
   */
  public static String substringBeforeLast(String string, String separator) {
    if (string == null || separator == null)
      return string;
    int k = string.lastIndexOf(separator);
    if (k < 0)
      return string;
    else
      return string.substring(0, k);
  }

  /**
   * Returns string's substring that ends before the first occurence of the separator. If separator is not encountered,
   * returns the whole string.
   *
   * @param string    the original string to look in.
   * @param separator a string that is looked for
   * @return substring of a string that ends before the first occurence of the separator
   */
  public static String substringBeforeFirst(String string, String separator) {
    if (string == null || separator == null)
      return string;
    int k = string.indexOf(separator);
    if (k < 0)
      return string;
    else
      return string.substring(0, k);
  }

  /**
   * Returns string's substring that starts after the last occurence of the separator. If separator is not encountered,
   * returns the whole string.
   *
   * @param string    the original string to look in.
   * @param separator a string that is looked for
   * @return substring of a string that starts after the last occurence of the separator
   */
  public static String substringAfterLast(String string, String separator) {
    if (string == null || separator == null)
      return string;
    int k = string.lastIndexOf(separator);
    if (k < 0)
      return string;
    else
      return string.substring(k + separator.length());
  }

  /**
   * Removes all character from the string that are whitespaces.
   *
   * @param name source string
   * @return source string without all whitespaces
   */
  public static String removeWhitespaces(String name) {
    if (name == null)
      return name;
    StringBuffer result = null;
    int len = name.length();
    for (int i = 0; i < len; i++) {
      char c = name.charAt(i);
      if (Character.isWhitespace(c)) {
        if (result == null) {
          result = new StringBuffer(len);
          result.append(name.substring(0, i));
        }
      } else {
        if (result != null) {
          result.append(c);
        }
      }
    }
    return result == null ? name : result.toString();
  }

  public static String implode(Iterable<String> strings, String delimiter) {
    StringBuffer r = new StringBuffer();
    for (String string : strings) {
      if (r.length() > 0)
        r.append(delimiter);
      r.append(string);
    }
    return r.toString();
  }

  @NotNull
  public static String limitString(String message, int maxChars) {
    if (message == null)
      return "";
    if (message.length() > maxChars) {
      message = message.substring(0, maxChars - 3).concat("...");
    }
    return message;
  }

  /**
   * Calculates heuristical similarity between strings. Very raw. The lower the result, the more affiliates the
   * strings are.
   */
  public static int calculateAffinity(String s1, String s2) {
    int[] arr1 = getLetterCounts(s1);
    int[] arr2 = getLetterCounts(s2);
    int diff = 0;
    int length = arr1.length;
    assert length == arr2.length;
    for (int i = 0; i < length; i++) {
      diff += Math.abs(arr1[i] - arr2[i]);
    }
    return diff;
  }

  private static int[] getLetterCounts(String s) {
    int[] result = new int[26];
    if (s != null) {
      int length = s.length();
      for (int i = 0; i < length; i++) {
        char c = s.charAt(i);
        int k;
        if (c >= 'A' && c <= 'Z') {
          k = c - 'A';
        } else if (c >= 'a' && c <= 'z') {
          k = c - 'a';
        } else {
          continue;
        }
        if (k >= 0 && k < result.length) {
          result[k]++;
        } else {
          assert false : k + ", " + String.valueOf(c);
        }
      }
    }
    return result;
  }

  public static int indexOfAny(String string, char c1, char c2) {
    int length = string.length();
    for (int i = 0; i < length; i++) {
      char c = string.charAt(i);
      if (c == c1 || c == c2)
        return i;
    }
    return -1;
  }

  public static String repeatCharacter(char c, int count) {
    char[] array = new char[count];
    Arrays.fill(array, c);
    return new String(array);
  }


  public static int indexOfIgnoreCase(String string, String subString) {
    return indexOfIgnoreCase(string.toCharArray(), 0, string.length(), subString.toCharArray(), 0, subString.length(), 0);
  }
  /**
   * Searches for target inclusion in source, char-by-char with Character.* methods.
   * Does not count in locale.
   * <p/>
   * Copied from String.
   */
  public static int indexOfIgnoreCase(char[] source, int sourceOffset, int sourceCount, char[] target, int targetOffset,
    int targetCount, int fromIndex)
  {
    if (fromIndex >= sourceCount) {
      return (targetCount == 0 ? sourceCount : -1);
    }
    if (fromIndex < 0) {
      fromIndex = 0;
    }
    if (targetCount == 0) {
      return fromIndex;
    }

    char first = target[targetOffset];
    char firstLower = Character.toLowerCase(first);

    int max = sourceOffset + (sourceCount - targetCount);

    for (int i = sourceOffset + fromIndex; i <= max; i++) {
      /* Look for first character. */
      if (Character.toLowerCase(source[i]) != firstLower) {
        while (++i <= max && Character.toLowerCase(source[i]) != firstLower)
          ;
      }

      /* Found first character, now look at the rest of v2 */
      if (i <= max) {
        int j = i + 1;
        int end = j + targetCount - 1;
        for (int k = targetOffset + 1; j < end; j++, k++) {
          char sch = source[j];
          char tch = target[k];
          if (sch != tch && Character.toLowerCase(sch) != Character.toLowerCase(tch)) {
            break;
          }
        }

        if (j == end) {
          /* Found whole string. */
          return i - sourceOffset;
        }
      }
    }
    return -1;
  }

  /**
   * Looks for any inclusion of words into source. Fills found with offsets of found elements.
   * If found[i] contains value that is different from -1, does not search for this substring.
   * Returns true if found has been changed.
   * <p>
   * NB: does not change found[i] if substrings[i] is not found!
   * <p>
   * NB: if found[i] != -1, will not search for substrings[i]!
   *
   * @param string source string
   * @param substrings substrings to be looked for
   * @param found array with equal size to subtrings that will be used to store offsets of found substrings
   * @param oneEnough return if at least one substring is found
   * @return the number of newly found substrings
   */
  public static int findAny(char[] string, char[][] substrings, @Nullable int[] found, boolean ignoreCase, boolean oneEnough) {
    assert found == null || substrings.length == found.length : substrings + " " + found;

    FindAnyMethod method;
    if (found == null)
      method = new FindAnyMethod(substrings, ignoreCase);
    else
      method = new FindAnyMethod(substrings, found, ignoreCase);
    return method.perform(string, oneEnough);


  }

  public static String normalizeWhitespace(String message) {
    final List<String> result = Collections15.arrayList();
    for(final String line : message.split("\\r?\\n")) {
      final String trimmed = line.trim();
      if(!trimmed.isEmpty()) {
        result.add(trimmed.replaceAll("\\s+", " "));
      }
    }
    return implode(result, LOCAL_LINE_SEPARATOR);
  }

  public static Set<String> caseInsensitiveSet(String... strings) {
    return Collections15.treeSet(String.CASE_INSENSITIVE_ORDER, strings);
  }

  public static String urlDecode(String urlEncoded) {
    try {
      return URLDecoder.decode(urlEncoded, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      Log.warn(e);
      return URLDecoder.decode(urlEncoded);
    }
  }

  public static class FindAnyMethod {
    private final BitSet myFirstChars = new BitSet();
    private final int mySubCount;
    private int mySubFound = 0;
    private final int myInitiallyFound;
    private final int[] myFound;
    private final char[][] mySubstrings;

    private final boolean myIgnoreCase;

    public FindAnyMethod(char[][] substrings, boolean ignoreCase) {
      this(substrings, createFound(substrings.length), ignoreCase);
    }

    private static int[] createFound(int count) {
      int[] found = new int[count];
      Arrays.fill(found, -1);
      return found;
    }

    public FindAnyMethod(char[][] substrings, int[] found, boolean ignoreCase) {
      myIgnoreCase = ignoreCase;
      mySubstrings = substrings;
      myFound = found;
      mySubCount = mySubstrings.length;
      int initiallyFound = 0;
      for (int ssi = 0; ssi < mySubCount; ssi++) {
        if (myFound[ssi] != -1) {
          initiallyFound++;
          continue;
        }
        char[] ss = mySubstrings[ssi];
        if (ss.length == 0) {
          myFound[ssi] = 0;
          mySubFound++;
          continue;
        }
        char sch = ss[0];
        myFirstChars.set((int) sch);
        if (myIgnoreCase) {
          myFirstChars.set((int) Character.toLowerCase(sch));
          myFirstChars.set((int) Character.toUpperCase(sch));
          myFirstChars.set((int) Character.toTitleCase(sch));
        }
      }
      myInitiallyFound = initiallyFound;
    }

    public int perform(char[] string, boolean oneEnough) {
      int slen = string.length;
      int toFind = mySubCount - myInitiallyFound;
      for (int i = 0; i < slen && mySubFound < toFind; i++) {
        if (myFirstChars.get((int)string[i])) {
          // have first character
          for (int ssi = 0; ssi < mySubCount; ssi++) {
            if (myFound[ssi] != -1)
              continue;
            char[] ss = mySubstrings[ssi];
            int sslen = ss.length;
            if (i + sslen > slen)
              continue;
            int j;
            for (j = 0; j < sslen; j++) {
              char a = string[i + j];
              char b = ss[j];
              if (a != b && (!myIgnoreCase || Character.toLowerCase(a) != Character.toLowerCase(b))) {
                break;
              }
            }
            if (j == sslen) {
              myFound[ssi] = i;
              mySubFound++;
              if (oneEnough) {
                return mySubFound;
              }
            }
          }
        }
      }

      return mySubFound;
    }
    public boolean areAllFound() {
      for (int found : myFound) {
        if (found < 0) {
          return false;
        }
      }
      return true;
    }

  }
}
