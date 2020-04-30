package com.almworks.util.text.html;

import org.almworks.util.Collections15;

import java.util.Map;

/**
 * @author dyoma
 */
public class HtmlElement extends ElementList {
  private final String myTagName;
  private final Map<String, String> myAttributes = Collections15.hashMap();

  public HtmlElement(String tagName) {
    myTagName = tagName;
  }

  public HtmlElement setAttribute(String name, String value) {
    myAttributes.put(name, value);
    return this;
  }

  public HtmlElement setAttribute(String name, int value) {
    setAttribute(name, String.valueOf(value));
    return this;
  }

  public void setCssClass(String cssClass) {
    setAttribute("class", cssClass);
  }

  public void appendTo(StringBuffer buffer) {
    buffer.append('<');
    buffer.append(myTagName);
    for (Map.Entry<String, String> entry : myAttributes.entrySet()) {
      buffer.append(" ");
      buffer.append(entry.getKey());
      buffer.append("=\"");
      buffer.append(entry.getValue());
      buffer.append("\"");
    }
    buffer.append('>');
    super.appendTo(buffer);
    buffer.append("</");
    buffer.append(myTagName);
    buffer.append('>');
  }
}
