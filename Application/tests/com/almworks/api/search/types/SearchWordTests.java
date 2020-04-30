package com.almworks.api.search.types;

import com.almworks.util.tests.BaseTestCase;

import java.util.Arrays;

/**
 * @author Vasya
 */
public class SearchWordTests extends BaseTestCase {
  public void testParsing() {
    check(null);
    check("");
    check("\"abc", "abc");
    check("abc\"", "abc");
    check("\"\"");
    check("\"\"\"");
    check("\"abc\"", "abc");
    check("\"abc\"\"", "abc");
    check(
      "foo; \"bar; \\\"baz\\\"\"; , ; quux; \"",
      "foo", "bar; \"baz\"", "quux");
  }

  private void check(String input, String... output) {
    final String[] actual = new SearchWords.Parser(input).parse();
    if(output == null || output.length == 0) {
      assertTrue(actual == null);
    } else {
      assertTrue(Arrays.equals(actual, output));
    }
  }
}
