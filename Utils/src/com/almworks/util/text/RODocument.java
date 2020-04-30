package com.almworks.util.text;

import com.almworks.util.TODO;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.Collections15;

import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.*;
import java.util.Map;

/**
 * @author dyoma
 */
public abstract class RODocument implements Document {
  private final Map<Object, Object> myProperties = Collections15.hashMap();
  private final RODocumentRoot myRootElement;

  RODocument(RODocumentRoot root) {
    myRootElement = root;
    myRootElement.setDocument(this);
  }

  public void remove(int offs, int len) throws BadLocationException {
    throw TODO.shouldNotHappen("Read-only");
  }

  public void render(Runnable r) {
    r.run();
  }

  public void addDocumentListener(DocumentListener listener) {
  }

  public void removeDocumentListener(DocumentListener listener) {
  }

  public void addUndoableEditListener(UndoableEditListener listener) {
  }

  public void removeUndoableEditListener(UndoableEditListener listener) {
  }

  public Element getDefaultRootElement() {
    return myRootElement;
  }

  public Element[] getRootElements() {
    throw TODO.notImplementedYet("To be implemented");
  }

  public Position getEndPosition() {
    throw TODO.notImplementedYet("To be implemented");
  }

  public Position getStartPosition() {
    throw TODO.notImplementedYet("To be implemented");
  }

  public Position createPosition(int offs) throws BadLocationException {
    return new SimplePosition(offs);
  }

  public Object getProperty(Object key) {
    return myProperties.get(key);
  }

  public void putProperty(Object key, Object value) {
    if (value != null)
      myProperties.put(key, value);
    else
      myProperties.remove(key);
  }

  public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
    throw TODO.shouldNotHappen("Read-only");
  }

  private static final char[] EMPTY_CHARS = new char[]{'\n'};

  private static final Element EMPTY_LINE;

  static final Document EMPTY = new RODocument(new RODocumentRoot() {
    public int getElementCount() {
      return 1;
    }

    public int getEndOffset() {
      return EMPTY_LINE.getEndOffset();
    }

    public Element getElement(int index) {
      return EMPTY_LINE;
    }

    public int getElementIndex(int offset) {
      return 0;
    }
  }) {
    public int getLength() {
      return 0;
    }

    public String getText(int offset, int length) throws BadLocationException {
      if (offset > 0 || length > 0)
        throw new BadLocationException("Empty document", offset);
      return "";
    }

    public void getText(int offset, int length, Segment txt) throws BadLocationException {
      txt.array = EMPTY_CHARS;
      txt.offset = offset;
      txt.count = length;
    }
  };

  static {
    EMPTY_LINE = new ModifiableRODocument.Line(0, 1, EMPTY.getDefaultRootElement());
  }

  public static void setComponentText(JTextComponent component, String text) {
    if ("".equals(text)) {
      if (component.getDocument() != EMPTY)
        component.setDocument(EMPTY);
      return;
    }
    Document document = component.getDocument();
    if (document instanceof ModifiableRODocument) {
      ModifiableRODocument roDocument = (ModifiableRODocument) document;
      if (DocumentUtil.getDocumentText(roDocument).equals(text))
        return;
      component.setDocument(EMPTY);
      roDocument.setText(text);
      component.setDocument(document);
    } else {
      component.setDocument(new ModifiableRODocument(text));
    }
  }

  private static class SimplePosition implements Position {
    private final int myOffs;

    public SimplePosition(int offs) {
      myOffs = offs;
    }

    public int getOffset() {
      return myOffs;
    }
  }
}
