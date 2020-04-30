package com.almworks.util.text.layout;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;

/**
 * @author : Dyoma
 */
public class ParagraphBuilderTests extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final ParagraphBuilder myBuilder = new ParagraphBuilder(10);

  public void testLeadingSpaceThenWord() {
    myBuilder.addWord("  ", 5);
    myBuilder.addWord("abc", 3);
    myBuilder.addWord(" ", 1);
    myBuilder.addWord("def", 3);
    checkLines(new String[]{"  abc ", "def"}, 8);
  }

  public void testLeadingSpaceThenLongWord() {
    myBuilder.addWord("  ", 5);
    myBuilder.addWord("abc", 6);
    myBuilder.addWord(" ", 1);
    myBuilder.addWord("d", 1);
    checkLines(new String[]{"  abc ", "d"}, 11);
  }

  public void testTrailingSpaces() {
    myBuilder.addWord("abc", 5);
    myBuilder.addWord(" ", 1);
    myBuilder.addWord("def", 3);
    myBuilder.addWord(" ", 1);
    myBuilder.addWord("\t", 2);
    myBuilder.addWord(" ", 3);
    checkLines(new String[]{"abc def \t "}, 9);
  }

  private void checkLines(String[] expected, int longestLine) {
    myBuilder.finishBuilding();
    CHECK.order(expected, myBuilder.getLines());
    assertEquals("Longest line", longestLine, myBuilder.getLongestLine());
  }
}
