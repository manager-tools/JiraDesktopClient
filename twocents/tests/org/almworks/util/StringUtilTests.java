package org.almworks.util;

import junit.framework.TestCase;

public class StringUtilTests extends TestCase {
  private static final String LADY = "i knew an old lady who swallowed a fly";

  public void testIndexOfIgnoreCase() {
    assertEquals(0, StringUtil.indexOfIgnoreCase("", ""));
    assertEquals(-1, StringUtil.indexOfIgnoreCase("", "xxx"));
    assertEquals(0, StringUtil.indexOfIgnoreCase("xxx", "xxx"));
    assertEquals(0, StringUtil.indexOfIgnoreCase("xxxx", "xxx"));
    assertEquals(-1, StringUtil.indexOfIgnoreCase("xx", "xxx"));
    assertEquals(-1, StringUtil.indexOfIgnoreCase("xx", "y"));

    assertEquals(0, StringUtil.indexOfIgnoreCase("xxX", "XX"));
    assertEquals(1, StringUtil.indexOfIgnoreCase("YxX", "XX"));

    assertEquals(LADY.length() - 3, StringUtil.indexOfIgnoreCase(LADY, "FLy"));
    assertEquals(2, StringUtil.indexOfIgnoreCase(LADY, "KNE"));
  }

  public void testFindAny() {
    checkFindAny(LADY, true, null, "knew", 2, "KNE", 2, "an ol", 7, "flya", -1, "E", 4);
    checkFindAny(LADY, false, null, "knew", 2, "KNE", -1, "an ol", 7, "flya", -1, "E", -1);

    int[] one = checkFindAny(LADY, false, null, "LADY", -1);
    int[] two = checkFindAny(LADY, true, one, "LADY", 14, "who", 19, "swall\u0999wed", -1, "#", -1);
    int[] three = checkFindAny(LADY, true, two, "i", 14, "know", 19, "an", 7, "old", 10, "", 0);
  }

  private int[] checkFindAny(String string, boolean ignoreCase, int[] initial, Object... data) {
    assert data.length % 2 == 0;
    int len = data.length / 2;
    char[][] substrings = new char[len][];
    int[] result = new int[len];
    int[] indices = new int[len];
    for (int i = 0; i < len; i++) {
      substrings[i] = ((String) data[i * 2]).toCharArray();
      result[i] = (Integer) data[i * 2 + 1];
      indices[i] = initial != null && i < initial.length ? initial[i] : -1;
    }
    StringUtil.findAny(string.toCharArray(), substrings, indices, ignoreCase, false);
    for (int i = 0; i < len; i++) {
      assertEquals("index " + i, result[i], indices[i]);
    }
    return indices;
  }
}
