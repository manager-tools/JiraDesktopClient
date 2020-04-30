package com.almworks.util.text;

import javax.swing.text.*;

/**
 * @author dyoma
 */
abstract class RODocumentRoot implements Element {
  private RODocument myDocument = null;

  public void setDocument(RODocument document) {
    assert myDocument == null;
    myDocument = document;
  }

  public int getStartOffset() {
    return 0;
  }

  public String getName() {
    return AbstractDocument.ParagraphElementName;
  }

  public AttributeSet getAttributes() {
    return SimpleAttributeSet.EMPTY;
  }

  public Document getDocument() {
    return myDocument;
  }

  public Element getParentElement() {
    return null;
  }

  public boolean isLeaf() {
    return false;
  }
}
