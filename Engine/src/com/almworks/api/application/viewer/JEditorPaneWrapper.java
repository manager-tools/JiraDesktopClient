package com.almworks.api.application.viewer;

import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.util.ErrorHunt;
import com.almworks.util.components.CommunalFocusListener;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.EditorKit;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.regex.Pattern;

import static com.almworks.util.components.Highlightable.HighlightUtil.addHighlighterByPattern;

/**
 * @author dyoma
*/
public class JEditorPaneWrapper extends TextComponentWrapper {
  private final JEditorPane myEditor;
  private Highlighter myHighlighter = new DefaultHighlighter();

  public JEditorPaneWrapper(JEditorPane component, boolean viewer, EditorKit kit) {
    super(component, viewer);
    myEditor = component;
    myEditor.setEditorKit(kit);
    UIUtil.addOpenLinkInBrowserListener(myEditor);
    if (!viewer)
      CommunalFocusListener.setupJEditorPane(myEditor);
    myEditor.setHighlighter(myHighlighter);
  }

  protected JTextComponent getTextComponent() {
    return myEditor;
  }

  private JEditorPaneWrapper(boolean viewer, TextDecoratorRegistry decorators, boolean supportHtml) {
    this(new JEditorPane(), viewer, LinksEditorKit.create(decorators, supportHtml));
  }

  public static JEditorPaneWrapper editor() {
    return new JEditorPaneWrapper(false, null, true);
  }

  public static JEditorPaneWrapper decoratedViewer(TextDecoratorRegistry decorators) {
    return new JEditorPaneWrapper(true, decorators, false);
  }

  public static JEditorPaneWrapper decoratedHtmlViewer(TextDecoratorRegistry decorators) {
    return new JEditorPaneWrapper(true, decorators, true);
  }

  public TextAreaWrapper createEditorWrapper() {
    return new JEditorPaneWrapper(new JEditorPane(), false, (EditorKit) myEditor.getEditorKit().clone());
  }

  public Object setText(String text) {
    ErrorHunt.setEditorPaneText(myEditor, text);
    return null;
  }

  public void setCachedTextData(Object cachedData, String text) {
    setText(text);
  }

  public void setHighlightPattern(Pattern pattern) {
    if (pattern != null) {
      myHighlighter.removeAllHighlights();
      addHighlighterByPattern(myHighlighter, UIUtil.getDocumentText(myEditor), pattern);
    }
  }

  public void paintViewAt(Graphics g, int x, int y) {
    myEditor.getUI().getRootView(myEditor).paint(g, new Rectangle(x, y, myEditor.getWidth(), myEditor.getHeight()));
  }

  public Dimension getPrefereredSize() {
    return myEditor.getPreferredSize();
  }
}
