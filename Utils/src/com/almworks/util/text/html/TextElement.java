package com.almworks.util.text.html;

/**
 * @author dyoma
 */
class TextElement implements RawElement {
  private final String myText;

  public TextElement(String text) {
    myText = text;
  }

  public void appendTo(StringBuffer buffer) {
    buffer.append(myText);
  }
}
