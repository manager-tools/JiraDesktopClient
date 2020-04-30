package com.almworks.util.xml;

import org.almworks.util.Util;
import org.w3c.dom.CharacterData;
import org.w3c.dom.*;

public class DOMUtils {
  public static String dump(Element element) {
    StringBuffer buffer = new StringBuffer();
    dump(element, buffer, "");
    return buffer.toString();
  }

  private static void dump(Element element, StringBuffer buffer, String padding) {
    String ns = Util.NN(element.getNamespaceURI());
    String tag = element.getTagName();
    NamedNodeMap attributes = element.getAttributes();
    NodeList children = element.getChildNodes();

    buffer.append(padding);
    buffer.append('<');
    if (ns.length() > 0)
      buffer.append("ns0:");
    buffer.append(tag);
    for (int i = 0; i < attributes.getLength(); i++) {
      Attr attr = (Attr) attributes.item(i);
      String name = attr.getName();
      String value = attr.getValue();
      buffer.append(' ').append(name).append("=\"").append(value).append("\"");
    }
    if (ns.length() > 0)
      buffer.append(" xmlns:ns0=\"").append(ns).append("\"");

    if (children.getLength() > 0) {
      buffer.append(">\n");
      String childPadding = padding + "  ";
      for (int i = 0; i < children.getLength(); i++) {
        Node node = children.item(i);
        if (node instanceof Element) {
          dump((Element) node, buffer, childPadding);
        } else if (node instanceof CharacterData) {
          buffer.append(childPadding);
          buffer.append(((CharacterData) node).getData());
          buffer.append('\n');
        } else {
          // todo
          assert false : node.getClass().getName();
        }
      }

      buffer.append(padding);
      buffer.append("</").append(tag).append(">\n");
    } else {
      buffer.append("/>\n");
    }
  }
}
