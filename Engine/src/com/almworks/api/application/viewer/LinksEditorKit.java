package com.almworks.api.application.viewer;

import com.almworks.api.application.viewer.textdecorator.TextDecoration;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.util.LogHelper;
import com.almworks.util.io.IOUtils;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;

public class LinksEditorKit extends StyledEditorKit {
  private JEditorPane myEditorPane;
  private final TextDecoratorRegistry myDecorators;
  private final LinkMouseListener myMouseListener = new LinkMouseListener();

  private LinksEditorKit(TextDecoratorRegistry decorators) {
    LogHelper.assertError(decorators != null);
    myDecorators = decorators;
  }

  public static StyledEditorKit create(TextDecoratorRegistry decorators, boolean supportsHtml) {
    return supportsHtml ? new LinkHtmlKit(decorators) : new LinksEditorKit(decorators);
  }

  @Override
  public Object clone() {
    return new LinksEditorKit(myDecorators);
  }

  @Override
  public void install(JEditorPane c) {
    assert myEditorPane == null;
    myEditorPane = c;
    myMouseListener.listen(c);
    super.install(c);
  }

  @Override
  public void deinstall(JEditorPane c) {
    assert myEditorPane == c;
    super.deinstall(c);
    myMouseListener.stopListen(c);
    myEditorPane = null;
  }

  @Override
  public Document createDefaultDocument() {
    return new MyStyledDocument();
  }

  @Override
  public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
    String text = IOUtils.readAll(in);
    super.read(new StringReader(text), doc, pos);
    readDocument(doc, pos, myDecorators, myEditorPane);
  }

  private static void readDocument(Document doc, int pos, TextDecoratorRegistry decorators, JEditorPane pane) throws IOException, BadLocationException {
    if (!(doc instanceof StyledDocument))
      return;
    StyledDocument document = (StyledDocument) doc;

    if(pane != null) {
      final SimpleAttributeSet paneSet = new SimpleAttributeSet();
      paneSet.addAttribute(StyleConstants.Foreground, pane.getForeground());
      document.setCharacterAttributes(0, document.getLength(), paneSet, false);
    }

    String text = document.getText(0, document.getLength());
    int offset = 0;
    Collection<? extends TextDecoration> links = decorators.processText(text);
    for (TextDecoration link : links) {
      int nextOffset = link.getOffset();
      Element startElement = document.getCharacterElement(nextOffset);
      Element endElement = document.getCharacterElement(link.getEndOffset() - 1);
      if (startElement != endElement)
        continue;
      AttributeSet existingAttrs = startElement.getAttributes();
      if (existingAttrs != null) {
        if (existingAttrs.getAttribute(HTML.Tag.A) != null)
          continue;
      }
      SimpleAttributeSet attr = new SimpleAttributeSet();
      attr.setResolveParent(TextComponentWrapper.COMMON_LINK_ATTR);
      attr.addAttributes(TextComponentWrapper.COMMON_LINK_ATTR);
      attr.addAttribute(TextDecoration.ATTRIBUTE, link);
      document.setCharacterAttributes(nextOffset, link.getLength(), attr, false);
    }
  }

//  public static DefaultStyledDocument clearDocument(JEditorPane component, boolean supportHtml) {
//    DefaultStyledDocument document;
//    Document d = component.getDocument();
//    boolean rightDocument =
//      supportHtml ? d instanceof HTMLDocument : d.getClass() == MyStyledDocument.class;
//    if (!rightDocument) {
//      document = (DefaultStyledDocument) component.getEditorKit().createDefaultDocument();
//      component.setDocument(document);
//    } else {
//      document = (DefaultStyledDocument) d;
//      try {
//        int len = document.getLength();
//        if (len > 0)
//          document.remove(0, len);
//      } catch (BadLocationException e) {
//        Log.error(e);
//      }
//    }
//
//    return document;
//  }

  private static class LinkHtmlKit extends HTMLEditorKit {
    private final LinkMouseListener myMouseListener = new LinkMouseListener();
    private final TextDecoratorRegistry myDecorators;

    private LinkHtmlKit(TextDecoratorRegistry decorators) {
      myDecorators = decorators;
    }

    @Override
    public void install(JEditorPane c) {
      myMouseListener.listen(c);
      super.install(c);
    }

    @Override
    public void deinstall(JEditorPane c) {
      super.deinstall(c);
      myMouseListener.stopListen(c);
    }

    @Override
    public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
      String text = IOUtils.readAll(in);
      super.read(new StringReader(text), doc, pos);
      readDocument(doc, pos, myDecorators, null);
    }

    @Override
    public Object clone() {
      return new LinkHtmlKit(myDecorators);
    }
  }

  private class MyStyledDocument extends DefaultStyledDocument {
    @Override
    public Font getFont(AttributeSet attr) {
      if (myEditorPane == null) {
        assert false;
        return super.getFont(attr);
      }
      return myEditorPane.getFont();
    }
  }

  private static class LinkMouseListener extends MouseAdapter implements Cloneable {
    private JTextComponent myComponent;

    public void listen(JTextComponent component) {
      assert myComponent == null;
      myComponent = component;
      component.addMouseListener(this);
      component.addMouseMotionListener(this);
    }

    public void stopListen(JTextComponent component) {
      assert myComponent == component;
      component.removeMouseListener(this);
      component.removeMouseMotionListener(this);
      myComponent = null;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
      return new LinkMouseListener();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      processMouse(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      processMouse(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
      processMouse(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      processMouse(e);
    }

    private void processMouse(MouseEvent e) {
      boolean processed = TextComponentWrapper.processMouse(e, myComponent);
      if (!processed)
        e.getComponent().setCursor(null);
    }
  }
}
