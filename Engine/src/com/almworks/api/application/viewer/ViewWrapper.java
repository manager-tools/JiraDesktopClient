package com.almworks.api.application.viewer;

import com.almworks.util.components.renderer.CellState;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.SwingHTMLHack;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.html.HTML;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

/**
 * @author dyoma
*/
class ViewWrapper extends JComponent implements TextAreaWrapper {
  private static final Rectangle PAINT_PLACE = new Rectangle();
  private static final Insets TEMP_INSETS = new Insets(0, 0, 0, 0);
  private View myView;
  private int myLastWidth = -1;
  private int myLastHeight = -1;
  private static final Dimension TEMP_SIZE = new Dimension();
  private final boolean myHtmlContent;

  public ViewWrapper(boolean htmlContent) {
    myHtmlContent = htmlContent;
    setBorder(TextComponentWrapper.getEmptyScrollPaneBorder());
  }

  public JComponent getComponent() {
    return this;
  }

  public TextAreaWrapper createEditorWrapper() {
    return myHtmlContent ? JEditorPaneWrapper.editor() : JTextAreaWrapper.editor();
  }

  public void setCachedTextData(Object cachedData, String text) {
    assert cachedData instanceof View : cachedData;
    myView = SwingHTMLHack.createHTMLRenderer(this, (View) cachedData);
    myLastWidth = -1;
    myLastHeight = -1;
  }

  public Object setText(String text) {
//      myView = BasicHTML.createHTMLView(this, TextUtil.preprocessHtml(text));
    View cachableView =
      SwingHTMLHack.createCachableView(TextUtil.preprocessHtml(text), null);
    myView = SwingHTMLHack.createHTMLRenderer(this, cachableView);
    myLastWidth = -1;
    myLastHeight = -1;
    return cachableView;
  }

  public void selectAll() {}

  public void scrollToBeginning() {}

  public int getPreferedHeight(CellState state, int width) {
    if (myView == null)
      return 0;
    if (width < 0) {
      assert false;
      return 0;
    }
    setViewWidth(width);
    if (myLastHeight < 0)
      myLastHeight = (int) myView.getPreferredSpan(View.Y_AXIS);
    return myLastHeight + AwtUtil.getInsetHeight(this) + 1;
  }

  @SuppressWarnings({"Deprecation"})
  @Deprecated
  public void reshape(int x, int y, int w, int h) {
    super.reshape(x, y, w, h);
    setViewWidth(w);
  }

  private void setViewWidth(int width) {
    if (myView == null)
      return;
    width -= AwtUtil.getInsetWidth(this);
    if (myLastWidth != width) {
      myLastWidth = width;
      myLastHeight = -1;
      myView.setSize(width, Short.MAX_VALUE);
    }
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    super.paintComponent(g);
    if (myView == null)
      return;
    paintAt(g, 0, 0);
  }

  public void paintAt(Graphics g, int x, int y) {
    preparePaintPlace();
    PAINT_PLACE.x = x;
    PAINT_PLACE.y = y;
    myView.setSize(PAINT_PLACE.width, PAINT_PLACE.height);
    myView.paint(g, PAINT_PLACE);
  }

  public void setTextForeground(Color foreground) {
    setForeground(foreground);
  }

  public void setHighlightPattern(Pattern pattern) {    
  }

  private void preparePaintPlace() {
    getInsets(TEMP_INSETS);
    getSize(TEMP_SIZE);
    PAINT_PLACE.x = TEMP_INSETS.left;
    PAINT_PLACE.y = TEMP_INSETS.top;
    PAINT_PLACE.width = TEMP_SIZE.width - AwtUtil.getInsetWidth(TEMP_INSETS);
    PAINT_PLACE.height = TEMP_SIZE.height - AwtUtil.getInsetHeight(TEMP_INSETS);
  }

  @Nullable
  public String getTooltipAt(int x, int y) {
    AttributeSet a = findA(x, y);
    return a != null ? Util.castNullable(String.class, a.getAttribute(HTML.Attribute.TITLE)) : null;
  }

  @Nullable
  private AttributeSet findA(int x, int y) {
    setViewWidth(getWidth());
    preparePaintPlace();
    //noinspection Deprecation
    int pos = myView.viewToModel(x, y, PAINT_PLACE);
    Element element = findElementAtPosition(myView, pos);
    Object a = element.getAttributes().getAttribute(HTML.Tag.A);
    return !(a instanceof AttributeSet) ? null : (AttributeSet) a;
  }

  public static Element findElementAtPosition(View view, int pos) {
    Element element = view.getElement();
    return findElementAtPosition(pos, element);
  }

  public static Element findElementAtPosition(int pos, Element element) {
  findDeepestChild:
    while (true) {
      int count = element.getElementCount();
      if (count == 0)
        break;
      for (int i = 0; i < count; i++) {
        Element child = element.getElement(i);
        if ((child.getEndOffset() >= pos) && (child.getStartOffset() <= pos)) {
          element = child;
          continue findDeepestChild;
        }
      }
      break;
    }
    return element;
  }

  public boolean processMouse(MouseEvent e) {
    return false;
  }
}
