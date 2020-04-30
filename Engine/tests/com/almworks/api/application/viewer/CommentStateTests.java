package com.almworks.api.application.viewer;

import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import com.almworks.util.text.TextUtil;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class CommentStateTests extends GUITestCase {
  private final FontMetrics myMetrics = Toolkit.getDefaultToolkit().getFontMetrics(UIManager.getFont("Label.font"));
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final String a50 = generate('a', 50);
  private final String a30 = generate('a', 30);

  public void test() {
    CHECK.order(new String[] {a50, ""}, getPlainTextLines(40, 2, a50));
    CHECK.order(new String[] {a50, a30}, getPlainTextLines(40, 2, a50, a30));
    CHECK.order(new String[] {a50, a30 + " \u2026"}, getPlainTextLines(40, 2, a50, a30, a30));
    CHECK.order(new String[] {a30 + " " + a30, a50}, getPlainTextLines(100, 2, a30, a30, a50));
    CHECK.order(new String[] {a30 + " " + a30, a50 + " \u2026"}, getPlainTextLines(100, 2, a30, a30, a50, a50));

    CommentState<MockComment> state = createState(a30, a30, a50, a50);
    int width = myMetrics.stringWidth(a30 + " " + a30);
    CHECK.order(new String[] {a30 + " " + a30, a50 + " \u2026"}, TextUtil.getPlainTextLines(state.getText(), width + 1, 2, myMetrics, true,
      true));
    CHECK.order(new String[] {a30, a30 + " \u2026"}, TextUtil.getPlainTextLines(state.getText(), width - 1, 2, myMetrics, true,
      true));
    state = createState(a30, a30, a50);
    CHECK.order(new String[] {a30 + " " + a30, a50}, TextUtil.getPlainTextLines(state.getText(), width + 1, 2, myMetrics, true,
      true));
  }

  public void testShortText() {
    String[] lines = getPlainTextLines(100, 2, a30, a30);
    assertEquals(a30 + " " + a30, lines[0]);
    assertEquals("", lines[1]);
  }

  private String[] getPlainTextLines(int width, int lineCount, String... strings) {
    return TextUtil.getPlainTextLines(createState(strings).getText(), width, lineCount, myMetrics, true, true);
  }

  private CommentState<MockComment> createState(String ... strings) {
    return new CommentState<MockComment>(new MockComment(TextUtil.separate(strings, " ")));
  }

  public String generate(char c, int length) {
    StringBuffer buffer = new StringBuffer();
    while (myMetrics.stringWidth(buffer.toString()) < length)
      buffer.append(c);
    assert buffer.length() > 3 : buffer.toString();
    return buffer.toString();
  }
}
