package com.almworks.util.text.html;

import com.almworks.util.tests.BaseTestCase;

/**
 * @author dyoma
 */
public class HtmlTextIteratorTests extends BaseTestCase {
  private HtmlTextIterator myIt;

  public void testPlainText() {
    checkParseAll("", "");
    checkParseAll("abc", "abc");
    checkParseAll("a b\nc\td", "a b c d");
    checkParseAll("a  b\r\n\tc", "a b c");
    checkParseAll("a bc\r\n\t ", "a bc ");
  }

  public void testCharRef() {
    checkParseAll("a&#161;b&#160;c", "a\u00A1b c");
    checkParseAll("&lt;a&gt;&amp;&quot; c&nbsp;d", "<a>&\" c d");
  }

  public void testSkipComment() {
    checkParseAll("a <!--Comment -->b<!--Comment -->c<!-", "a bc");
    checkParseAll("abc<!-- Comment", "abc");
  }

  public void testSkipSimpleTag() {
    checkParseAll("a<b>b</b>c<b> x </b>d", "abc x d");
    checkParseAll("a<b", "a");
    checkParseAll("a<b ", "a");
    checkParseAll("a<TTTTTT> b", "a  b");
  }

  public void testTagWithAttributes() {
    checkParseAll("a<b n=x m='y' l=\"z\"> b", "a b");
    checkParseAll("a<b n=\">\" >b", "ab");
    checkParseAll("a<b n='>' >b", "ab");
    checkParseAll("a<b n='x\">' >b", "ab");
    checkParseAll("a<b n=\"x'>\" >b", "ab");
  }

  public void testLineBreaks() {
    checkParseAll("a<br>b", "a b");
    checkParseAll("a<br> b", "a  b");
    checkParseAll("a<p>b", "a b");
    checkParseAll("a<div>b", "a b");
    checkParseAll("a<dd>b", "a b");
    checkParseAll("a<dl>b", "a b");
    checkParseAll("a<form>b", "a b");
    checkParseAll("a<ol>b", "a b");
  }

  private void checkParseAll(String html, String plainText) {
    prepareTest(html);
    assertEquals(plainText, parseToEnd());
  }

  private void prepareTest(String html) {
    myIt = new HtmlTextIterator(html);
  }

  private String parseToEnd() {
    StringBuffer buffer = new StringBuffer();
    while (myIt.hasNext())
      buffer.append(myIt.nextChar());
    return buffer.toString();
  }
}
