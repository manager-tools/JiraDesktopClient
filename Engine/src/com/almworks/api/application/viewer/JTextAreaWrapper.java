package com.almworks.api.application.viewer;

import com.almworks.api.application.viewer.textdecorator.TextDecoration;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.util.components.CommunalFocusListener;
import com.almworks.util.text.RODocument;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import javax.swing.*;
import javax.swing.text.*;
import java.util.Collection;
import java.util.regex.Pattern;

import static com.almworks.util.components.Highlightable.HighlightUtil.addHighlighterByPattern;

/**
 * @author dyoma
*/
public class JTextAreaWrapper extends TextComponentWrapper {
  private final JTextArea myArea;

  private Highlighter myHighlighter = new DefaultHighlighter();
  
  private final TextDecoratorRegistry myDecorators;

  private JTextAreaWrapper(JTextArea area, boolean viewer, TextDecoratorRegistry decorators) {
    super(area, viewer);
    myArea = area;
    myDecorators = decorators;
    myArea.setLineWrap(true);
    myArea.setWrapStyleWord(true);
    myArea.setHighlighter(myHighlighter);
    if (!viewer)
      CommunalFocusListener.setupJTextArea(myArea);
  }

  private JTextAreaWrapper(boolean viewer, TextDecoratorRegistry decorators) {
    this(new JTextArea(), viewer, decorators);
  }


  protected JTextArea getTextComponent() {
    return myArea;
  }

  public TextAreaWrapper createEditorWrapper() {
    return new JTextAreaWrapper(false, null);
  }

  public Object setText(String text) {
    Collection<? extends TextDecoration> links =
      myDecorators != null ? myDecorators.processText(text) : Collections15.<TextDecoration>emptyCollection();
    if (links.isEmpty()) {
      RODocument.setComponentText(myArea, text);
      return null;
    }
    DefaultStyledDocument doc = new DefaultStyledDocument();
    int offset = 0;
    try {
      for (TextDecoration link : links) {
        int nextOffset = link.getOffset();
        if (nextOffset > offset)
          doc.insertString(offset, text.substring(offset, nextOffset), null);
        offset = nextOffset + link.getLength();
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.setResolveParent(COMMON_LINK_ATTR);
        attr.addAttribute(TextDecoration.ATTRIBUTE, link);
        doc.insertString(nextOffset, text.substring(nextOffset, offset), attr);
      }
      myArea.setDocument(doc);
    } catch (BadLocationException e) {
      Log.error(e);
      RODocument.setComponentText(myArea, text);
    }
    return null;
  }

  public static TextAreaWrapper viewer(TextDecoratorRegistry decorators) {
    return new JTextAreaWrapper(true, decorators);
  }

  public static TextAreaWrapper editor() {
    return new JTextAreaWrapper(false, null);
  }

  public void setCachedTextData(Object cachedData, String text) {
    assert false : cachedData;
    setText(text);
  }

  public void setHighlightPattern(Pattern pattern) {
    if (pattern != null) {
      myHighlighter.removeAllHighlights();      
      addHighlighterByPattern(myHighlighter, UIUtil.getDocumentText(myArea), pattern);
    }
  }
}
