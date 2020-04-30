package com.almworks.util.xml;

import org.almworks.util.Collections15;
import org.jdom.Content;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * This is a document-order deep iterator that returns Elements as they appear in textual
 * representation of the XML.
 */
public class JDOMElementIterator {
  private final List<Integer> myIndexStack = Collections15.arrayList();
  private Element myCurrentParent;
  private int myNextIndex;

  public JDOMElementIterator(Element root) {
    myCurrentParent = root;
    myNextIndex = 0;
  }

  private JDOMElementIterator(Element currentParent, int nextIndex, List<Integer> indexStack) {
    myCurrentParent = currentParent;
    myNextIndex = nextIndex;
    myIndexStack.addAll(indexStack);
  }

  /**
   * Retrieves the next element.
   *
   * @return next element or null if iteration is finished
   */
  @Nullable
  public Element next() {
    while (true) {
      Element e = findElement();
      if (e == null) {
        boolean proceed = pop();
        if (!proceed)
          return null;
      } else {
        push(e);
        return e;
      }
    }
  }

  /**
   * Skips iteration inside the last returned element.
   */
  public void skipLastElementContent() {
    pop();
  }

  private boolean pop() {
    int stackSize = myIndexStack.size();
    if (stackSize == 0)
      return false;
    myNextIndex = myIndexStack.remove(stackSize - 1);
    Element parent = myCurrentParent.getParentElement();
    assert parent.getContent(myNextIndex - 1) == myCurrentParent;
    myCurrentParent = parent;
    return true;
  }

  private void push(Element e) {
    assert e.getParent() == myCurrentParent : myCurrentParent + " " + e;
    myIndexStack.add(myNextIndex);
    myCurrentParent = e;
    myNextIndex = 0;
  }

  private Element findElement() {
    int size = myCurrentParent.getContentSize();
    while(myNextIndex < size) {
      Content content = myCurrentParent.getContent(myNextIndex);
      myNextIndex++;
      if (content instanceof Element) {
        return (Element) content;
      }
    }
    return null;
  }

  /**
   * Finds next element with given name, case-insensitive
   *
   * @return next element with tagName or null
   */
  @Nullable
  public Element next(String tagName) {
    Element e;
    while ((e = next()) != null) {
      if (e.getName().equalsIgnoreCase(tagName)) {
        return e;
      }
    }
    return null;
  }

  /**
   * Creates duplicate iterator in the same state
   */
  public JDOMElementIterator duplicate() {
    return new JDOMElementIterator(myCurrentParent, myNextIndex, myIndexStack);
  }
}
