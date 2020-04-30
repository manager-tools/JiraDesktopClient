package com.almworks.util.text;

import com.almworks.util.tests.GUITestCase;
import com.almworks.util.ui.swing.DocumentUtil;

import javax.swing.*;
import javax.swing.text.*;

/**
 * @author dyoma
 */
public class RODocumentTests extends GUITestCase {
  private PlainDocument myPlain;
  private JTextField myField;

  protected void setUp() throws Exception {
    super.setUp();
    myPlain = new PlainDocument();
    myField = new JTextField();
  }

  public void testSingleLine() throws BadLocationException {
    checkText("abc");
  }

  public void testLines() throws BadLocationException {
    checkText("a\nb\n\tc");
  }

  public void testEmptyText() throws BadLocationException {
    checkText("");
  }

  public void testEmptyDocument() throws BadLocationException {
    checkDocumentsEqual(new PlainDocument(), RODocument.EMPTY);
  }

  private void checkText(String text) throws BadLocationException {
    checkDocumentsEqual(myPlain, setDocumentText(text));
  }

  private void checkDocumentsEqual(Document expected, Document testing) throws BadLocationException {
    assertEquals(expected.getLength(), testing.getLength());
    // Not implemented yet
//    int rootElementsCount = expected.getRootElements().length;
//    assertEquals(rootElementsCount, testing.getRootElements().length);
//    for (int i = 0; i < rootElementsCount; i++)
//      checkEqualElements(expected.getRootElements()[i], testing.getRootElements()[i]);
    checkEqualElements("root", expected.getDefaultRootElement(), testing.getDefaultRootElement());
  }

  private Document setDocumentText(String text) {
    RODocument.setComponentText(myField, text);
    assertEquals(text, myField.getText());
    Document document = myField.getDocument();
    DocumentUtil.setDocumentText(myPlain, myField.getText());
    return document;
  }

  private void checkEqualElements(String message, Element plain, Element element) throws BadLocationException {
    assertNotNull(plain);
    assertNotNull(element);
    int startOffset = plain.getStartOffset();
    assertEquals(message, startOffset, element.getStartOffset());
    int endOffset = plain.getEndOffset();
    assertEquals(message, endOffset, element.getEndOffset());
    int length = endOffset - startOffset;
    Segment expectedSegment = new Segment();
    plain.getDocument().getText(startOffset, length, expectedSegment);
    Segment testingSegment = new Segment();
    element.getDocument().getText(startOffset, length, testingSegment);
//    assertEquals(message, expectedSegment.offset, testingSegment.offset);
    assertEquals(message, expectedSegment.count, testingSegment.count);
    assertTrue(message +" count:" + testingSegment.count + " length:" + testingSegment.array.length,
      testingSegment.count <= testingSegment.array.length);
    assertEquals(message, expectedSegment.toString(), testingSegment.toString());

    int elementCount = plain.getElementCount();
    assertEquals(message, elementCount, element.getElementCount());
    for (int i = 0; i < elementCount; i++) {
      checkEqualElements(message + "/" + i, plain.getElement(i), element.getElement(i));
    }
    for (int i = startOffset; i <= endOffset; i++)
      assertEquals(message + " offset:" + i, plain.getElementIndex(i), element.getElementIndex(i));
  }
}
