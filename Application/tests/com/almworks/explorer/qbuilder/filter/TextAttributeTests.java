package com.almworks.explorer.qbuilder.filter;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;

public class TextAttributeTests extends BaseTestCase {
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  public void testSubstrings() {
    check("ab c", "ab", "c");
    check("ab-c 123", "ab-c", "123");
    check("\"ab c\" 123", "ab c", "123");
    check("a\\\"b 123", "a\"b", "123");
    check("aa  bb", "aa", "bb");
    check("ab", "ab");
    check("\"a\"", "a");
  }

  public void check(String query, String ... expected) {
    CHECK.order(expected, TextAttribute.parseTextFragments(query));
  }
}
