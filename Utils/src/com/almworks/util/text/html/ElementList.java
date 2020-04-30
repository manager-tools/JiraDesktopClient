package com.almworks.util.text.html;

import org.almworks.util.Collections15;

import java.util.List;

/**
 * @author dyoma
 */
public class ElementList implements RawElement {
  private final List<RawElement> myChildren = Collections15.arrayList();

  private <T extends RawElement> T addChild(T element) {
    myChildren.add(element);
    return element;
  }

  public void addText(String text) {
    addChild(new TextElement(text));
  }

  public String getText() {
    StringBuffer buffer = new StringBuffer();
    appendTo(buffer);
    return buffer.toString();
  }

  public void appendTo(StringBuffer buffer) {
    for (RawElement child : myChildren)
      child.appendTo(buffer);
  }

  public HtmlElement addDiv(String cssClass) {
    return addElement("div", cssClass);
  }

  public HtmlElement addElement(String tagName, String cssClass) {
    HtmlElement div = new HtmlElement(tagName);
    div.setCssClass(cssClass);
    return addChild(div);
  }

  public HtmlElement addElement(String tagName) {
    return addChild(new HtmlElement(tagName));
  }

  public HtmlElement addTable(int border, int cellSpacing, int cellPadding) {
    HtmlElement table = new HtmlElement("table");
    table.setAttribute("border", border);
    table.setAttribute("cellspacing", cellSpacing);
    table.setAttribute("cellpadding", cellPadding);
    return addChild(table);
  }
}
