package com.almworks.api.connector.http;

import org.apache.xerces.util.XMLChar;
import org.apache.xerces.util.XMLStringBuffer;
import org.apache.xerces.xni.*;
import org.cyberneko.html.filters.DefaultFilter;

/**
 * See org.cyberneko.html.filters.Purifier -- get more from there
 */
public class LightPurifier extends DefaultFilter {
  // temp vars
  /**
   * Qualified name.
   */
  private QName fQName = new QName();
  /**
   * String buffer.
   */
  private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();

  /**
   * XML declaration.
   */
  public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
    if (version == null || !version.equals("1.0")) {
      version = "1.0";
    }
    if (encoding != null && encoding.length() == 0) {
      encoding = null;
    }
    if (standalone != null) {
      if (!standalone.equalsIgnoreCase("true") && !standalone.equalsIgnoreCase("false")) {
        standalone = null;
      } else {
        standalone = standalone.toLowerCase();
      }
    }
    super.xmlDecl(version, encoding, standalone, augs);
  } // xmlDecl(String,String,String,Augmentations)

  public void startElement(QName element, XMLAttributes attrs, Augmentations augs) throws XNIException {
    handleStartElement(element, attrs);
    super.startElement(element, attrs, augs);
  } // startElement(QName,XMLAttributes,Augmentations)

  /**
   * Empty element.
   */
  public void emptyElement(QName element, XMLAttributes attrs, Augmentations augs) throws XNIException {
    handleStartElement(element, attrs);
    super.emptyElement(element, attrs, augs);
  } // emptyElement(QName,XMLAttributes,Augmentations)


  /**
   * End element.
   */
  public void endElement(QName element, Augmentations augs) throws XNIException {
    element = purifyQName(element);
    super.endElement(element, augs);
  } // endElement(QName,Augmentations)


  /**
   * Handle start element.
   */
  protected void handleStartElement(QName element, XMLAttributes attrs) {
    // handle element and attributes
    element = purifyQName(element);
    if(attrs != null) {
      int attrCount = attrs.getLength();
      for (int i = attrCount - 1; i >= 0; i--) {
        // purify attribute name
        attrs.getName(i, fQName);
        attrs.setName(i, purifyQName(fQName));
      }
    }
    // mark start element as seen
  } // handleStartElement(QName,XMLAttributes)


  /**
   * Purify qualified name.
   */
  protected QName purifyQName(QName qname) {
    qname.prefix = purifyName(qname.prefix, true);
    qname.localpart = purifyName(qname.localpart, true);
    qname.rawname = purifyName(qname.rawname, false);
    return qname;
  } // purifyQName(QName):QName

  /**
   * Purify name.
   */
  protected String purifyName(String name, boolean localpart) {
    if (name == null) {
      return name;
    }
    StringBuilder str = new StringBuilder();
    int length = name.length();
    boolean seenColon = localpart;
    for (int i = 0; i < length; i++) {
      char c = name.charAt(i);
      if (i == 0) {
        if (!XMLChar.isNameStart(c)) {
          str.append("_u" + toHexString(c, 4) + "_");
        } else {
          str.append(c);
        }
      } else {
        if ((false && c == ':' && seenColon) || !XMLChar.isName(c)) {
          str.append("_u" + toHexString(c, 4) + "_");
        } else {
          str.append(c);
        }
        seenColon = seenColon || c == ':';
      }
    }
    return str.toString();
  } // purifyName(String):String

  /**
   * Purify content.
   */
  protected XMLString purifyText(XMLString text) {
    fStringBuffer.length = 0;
    for (int i = 0; i < text.length; i++) {
      char c = text.ch[text.offset + i];
      if (XMLChar.isInvalid(c)) {
        fStringBuffer.append("\\u" + toHexString(c, 4));
      } else {
        fStringBuffer.append(c);
      }
    }
    return fStringBuffer;
  } // purifyText(XMLString):XMLString

  /**
   * Returns a padded hexadecimal string for the given value.
   */
  protected static String toHexString(int c, int padlen) {
    StringBuilder str = new StringBuilder(padlen);
    str.append(Integer.toHexString(c));
    int len = padlen - str.length();
    for (int i = 0; i < len; i++) {
      str.insert(0, '0');
    }
    return str.toString().toUpperCase();
  } // toHexString(int,int):String
} // class Purifier
