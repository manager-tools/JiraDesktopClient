package com.almworks.util.ui.swing;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.ui.DocumentAdapter;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class DocumentUtil {
  public static DocumentAdapter runOnChange(final Runnable runnable) {
    return new DocumentAdapter() {
      protected void documentChanged(DocumentEvent e) {
        runnable.run();
      }
    };
  }

  public static DocumentListener notifyListener(final ChangeListener listener) {
    return new DocumentAdapter() {
      protected void documentChanged(DocumentEvent e) {
        listener.onChange();
      }
    };
  }

  public static String getDocumentText(Document document) {
    String result;
    try {
      result = document.getText(0, document.getLength());
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  public static void setDocumentText(Document document, String string) {
    try {
      if (document instanceof AbstractDocument)
        ((AbstractDocument) document).replace(0, document.getLength(), string, null);
      else {
        document.remove(0, document.getLength());
        document.insertString(0, string, null);
      }
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean changeDocumentText(Document document, String newText) {
    if (Util.equals(newText, getDocumentText(document)))
      return false;
    setDocumentText(document, newText);
    return true;
  }

  public static void copyDocumentText(Document from, Document to) {
    String text = getDocumentText(from);
    if (!Util.equals(text, getDocumentText(to)))
      setDocumentText(to, text);
  }

  public static void addListener(Lifespan life, final Document document, final DocumentListener listener) {
    if (!life.isEnded()) {
      document.addDocumentListener(listener);
      if (life != Lifespan.FOREVER)
        life.add(new Detach() {
          protected void doDetach() {
            document.removeDocumentListener(listener);
          }
        });
    }
  }

  public static void addChangeListener(Lifespan life, Document document, final ChangeListener listener) {
    if (life.isEnded())
      return;
    addListener(life, document, notifyListener(listener));
  }

  public static void unsafeReplaceDocumentText(Document document, int offset, int length, String replacement) {
    try {
      if (document instanceof AbstractDocument) {
        ((AbstractDocument) document).replace(offset, length, replacement, null);
      } else {
        if (length > 0)
          document.remove(offset, length);
        if (replacement.length() > 0)
          document.insertString(offset, replacement, null);
      }
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }
}
