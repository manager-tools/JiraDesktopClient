package com.almworks.util.text;

import org.almworks.util.Collections15;

import javax.swing.text.*;
import java.util.List;

/**
 * @author dyoma
 */
class ModifiableRODocument extends RODocument {
  private char[] myChars = null;
  private final List<Element> myElements = Collections15.arrayList();

  ModifiableRODocument(String text) {
    super(new MyDocumentRoot());
    setText(text);
  }

  public MyDocumentRoot getDefaultRootElement() {
    return (MyDocumentRoot) super.getDefaultRootElement();
  }

  public void setText(String text) {
    myElements.clear();
    text = text != null ? text : "";
    myChars = new char[text.length() + 1];
    text.getChars(0, text.length(), myChars, 0);
    myChars[myChars.length - 1] = '\n';
    LineTokenizer tokenizer = new LineTokenizer(text);
    tokenizer.setIncludeLineSeparators(true);
    int offset = 0;
    MyDocumentRoot root = getDefaultRootElement();
    root.resetElements();
    while (tokenizer.hasMoreLines()) {
      String line = tokenizer.nextLine();
      int end = offset + line.length();
      if (!tokenizer.hasMoreLines())
        end++;
      root.addElement(new Line(offset, end, root));
      offset = end;
    }
  }

  public String getText(int offset, int length) throws BadLocationException {
    return new String(myChars, offset, length);
  }

  public int getLength() {
    return myChars.length - 1;
  }

  public void getText(int offset, int length, Segment txt) throws BadLocationException {
    txt.array = myChars;
    txt.offset = offset;
    txt.count = length;
  }

  static class Line implements Element {
    private final int myStart;
    private final int myEnd;
    private final Element myParent;

    public Line(int start, int end, Element parent) {
      myStart = start;
      myEnd = end;
      myParent = parent;
    }

    public int getElementCount() {
      return 0;
    }

    public int getEndOffset() {
      return myEnd;
    }

    public int getStartOffset() {
      return myStart;
    }

    public boolean isLeaf() {
      return true;
    }

    public int getElementIndex(int offset) {
      return -1;
    }

    public String getName() {
      return AbstractDocument.SectionElementName;
    }

    public AttributeSet getAttributes() {
      return SimpleAttributeSet.EMPTY;
    }

    public Document getDocument() {
      return myParent.getDocument();
    }

    public Element getParentElement() {
      return myParent;
    }

    public Element getElement(int index) {
      return null;
    }
  }

  private static class MyDocumentRoot extends RODocumentRoot {
    private final List<Element> myChildren = Collections15.arrayList();

    public int getElementCount() {
      return myChildren.size();
    }

    public int getEndOffset() {
      return myChildren.size() == 0 ? 0 : myChildren.get(myChildren.size() - 1).getEndOffset();
    }

    public Element getElement(int index) {
      return index >= 0 ? myChildren.get(index) : null;
    }

    public void resetElements() {
      myChildren.clear();
    }

    public void addElement(Element element) {
      myChildren.add(element);
    }

    public int getElementIndex(int offset) {
      if (myChildren.isEmpty())
        return 0;
      if (offset >= getEndOffset())
        return myChildren.size() - 1;
      for (int i = 0; i < myChildren.size(); i++) {
        Element element = myChildren.get(i);
        if (element.getStartOffset() <= offset && offset < element.getEndOffset())
          return i;
      }
      return -1;
    }
  }
}
