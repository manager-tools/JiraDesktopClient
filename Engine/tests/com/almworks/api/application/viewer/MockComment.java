package com.almworks.api.application.viewer;

import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

class MockComment implements Comment {
  private final String myText;

  public MockComment(String text) {
    myText = text;
  }

  public String getText() {
    return myText;
  }

  public String getWhenText() {
    assert false;
    return null;
  }

  public Date getWhen() {
    assert false;
    return null;
  }

  public String getWhoText() {
    assert false;
    return null;
  }

  @Override
  public String getHeaderTooltipHtml() {
    return null;
  }

  @Override
  public long getItem() {
    assert false;
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof MockComment)) return false;
    return ((MockComment) obj).myText.equals(myText);
  }

  @Override
  public int hashCode() {
    return myText.hashCode();
  }

  public static List<CommentState<MockComment>> createStateList(String ... texts) {
    List<CommentState<MockComment>> result = Collections15.arrayList(texts.length);
    for (String text : texts) result.add(createState(text));
    return result;
  }

  public static CommentState<MockComment> createState(String text) {
    return new CommentState<MockComment>(new MockComment(text));
  }

  public static String[] collectTexts(Collection<? extends CommentState<? extends Comment>> states) {
    String[] result = new String[states.size()];
    int i = 0;
    for (Iterator<? extends CommentState<? extends Comment>> iterator = states.iterator(); iterator.hasNext(); i++) {
      CommentState<? extends Comment> state = iterator.next();
      result[i] = state.getText();
    }
    return result;
  }
}
