package com.almworks.util.xml;

/**
 * This interface is used in {@link com.almworks.util.xml.JDOMUtils#getText(org.jdom.Element,boolean,XmlTextAdjuster)}
 * method to process individual tags and possible adjust text value of the whole element tree.
 */
public interface XmlTextAdjuster {
  /**
   * This method is called before tag's text value is added to the buffer.
   *
   * @param buffer  the collecting string buffer, filled with values of preceding tags, not including text value of
   *                the element
   * @param element the element that is about to be processed
   * @return true if the element should be processed; false if the element should be skipped and its value not added to
   *         the buffer.
   */
  boolean beforeElement(StringBuffer buffer, Object element, XmlTextAdjusterHelper helper);

  /**
   * This method is called after tag is processed. This method is called even if adjustBufferBeforeElement returned false.
   *
   * @param buffer  the collecting string buffer, which already has (or has not, if ...before... returned false) the
   *                text value of the element
   * @param element element which was processed
   */
  void afterElement(StringBuffer buffer, Object element, XmlTextAdjusterHelper helper);

  /**
   * This method is called to append text to the buffer. Non-processing method should add textValue to the buffer.
   *
   * @param textNode
   * @param textValue node's value, probably already with some adjustments
   */
  void appendText(StringBuffer buffer, Object textNode, String textValue, XmlTextAdjusterHelper helper);
}
