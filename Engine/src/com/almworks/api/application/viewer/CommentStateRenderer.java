package com.almworks.api.application.viewer;

import com.almworks.util.Env;
import com.almworks.util.components.CollectionRenderer;
import com.almworks.util.components.layout.WidthDrivenCollectionRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.TextGraphics;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.LafDependentColor;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.BaseRendererComponent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
class CommentStateRenderer<T extends Comment> extends BaseRendererComponent
  implements CollectionRenderer<CommentState<T>>, WidthDrivenCollectionRenderer<CommentState<T>>
{
  private static final int MAC_EXTRA_MARGIN = Env.isMac() ? 3 : 0;
  private static final Insets COMMENT_MARGIN = new Insets(0, 21 + MAC_EXTRA_MARGIN, 3, 5);
  private static final int HEADER_LEFT_MARGIN = 5 + MAC_EXTRA_MARGIN;
  private static final int HEADER_HORIZONTAL_GAP = 10;
  private static final int HEADER_BOTTOM_MARGIN = 0;
  private static final Font FONT = new JEditorPane().getFont();
  private static final Color myUnselectedBackground = UIManager.getColor("EditorPane.background");

  private static final LafDependentColor mySelectedBackground = 
    new LafDependentColor("Table.selectionBackground") {
      @Override
      public Color makeColor(Color lafColor) {
        return ColorUtil.between(myUnselectedBackground, lafColor, 0.2F);
      }
    };

  private static final int COLLAPSED_LINES = 2;

  private CommentState<T> myLastState;
  private final TextAreaWrapper myWrapper;
  private final boolean myHtmlContent;

  private Pattern myCurrentPattern;

  @Nullable
  private final CommentRenderingHelper<T> myHelper;

  public CommentStateRenderer(TextAreaWrapper wrapper, boolean htmlContent, @Nullable CommentRenderingHelper<T> helper)
  {
    myWrapper = wrapper;
    myHtmlContent = htmlContent;
    myHelper = helper;
    setOpaque(true);
    setFont(FONT); // TextGraphics will use this font.
  }

  public JComponent getRendererComponent(CellState state, CommentState<T> item) {
    myLastState = item;
    setBackground(state.isSelected() ? mySelectedBackground.getColor() : myUnselectedBackground);
    setForeground(getForegroundFor(item));
    myCurrentPattern = state.getHighlightPattern();
    setBorder(state.getBorder());
    return this;
  }

  Color getForegroundFor(CommentState<T> item) {
    Color result = null;
    if (myHelper != null) {
      result = myHelper.getForeground(item.getComment());
    }
    if (result == null) {
      result = UIUtil.getEditorForeground();
    }
    return result;
  }

  public int getPreferredHeight(CellState state, CommentState<T> item, int width) {
    myLastState = item;
    Border border = state.getBorder();
    int a = getHeaderHeight(state);
    int b = getContentHeight(state, getContentWidth(width, border));
    int c = AwtUtil.getInsetHeight(COMMENT_MARGIN);
    int d = border == null ? 0 : AwtUtil.getInsetHeight(border.getBorderInsets(this));
    return a + b + c + d;
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    g.setColor(getBackground());
    g.fillRect(0, 0, getWidth(), getHeight());
    TextGraphics textGraphics = TextGraphics.getInstance(g, this);
    textGraphics.setHighlightPattern(myCurrentPattern);

    int height = paintHeader(textGraphics);
    if (isCollapsed())
      paintCollapsedContent(height, textGraphics);
    else
      paintExpandedContent(g, height);

    textGraphics.dispose();
  }

  private void paintExpandedContent(Graphics g, int topOffset) {
    setupWrapperFullText(topOffset);
    myWrapper.setHighlightPattern(myCurrentPattern);
    myWrapper.paintAt(g, COMMENT_MARGIN.left, topOffset + COMMENT_MARGIN.top);
  }

  private void setupWrapperFullText(int topOffset) {
    setWrapperFullText();
    myWrapper.getComponent()
      .setSize(getContentWidth(), getHeight() - topOffset - AwtUtil.getInsetHeight(this));
  }

  private void paintCollapsedContent(int y, TextGraphics textGraphics) {
    textGraphics.setCorner(COMMENT_MARGIN.left, y + COMMENT_MARGIN.top);
    textGraphics.setDefaultStyle();
    textGraphics.setForeground(getForeground());

    String text = myLastState.getText();

    if (myCurrentPattern != null && myCurrentPattern.pattern() != "") {
      text = TextUtil.getStartWithMatch(text, myCurrentPattern);
    }

    String[] lines = TextUtil.getPlainTextLines(text, getContentWidth(), COLLAPSED_LINES,
      textGraphics.getCurrentFontMetrics(), myHtmlContent, true);
    for (String line : lines) {
      textGraphics.draw(line);
      textGraphics.newLine();
    }
  }

  private int getContentWidth() {
    return getContentWidth(this);
  }

  private int getContentWidth(JComponent component) {
    return getContentWidth(component.getWidth(), component.getBorder());
  }

  private int getContentWidth(int componentWidth, Border border) {
    int borderWidth = border != null ? AwtUtil.getInsetWidth(border.getBorderInsets(this)) : 0;
    return componentWidth - AwtUtil.getInsetWidth(COMMENT_MARGIN) - borderWidth;
  }

  private int paintHeader(TextGraphics textGraphics) {
    int height = textGraphics.getLineHeight() + HEADER_BOTTOM_MARGIN;
    textGraphics.setForeground(ColorUtil.between(getForeground(), getBackground(), 0.5f));
    textGraphics.setCorner(HEADER_LEFT_MARGIN, 0);
    textGraphics.setFontStyle(Font.PLAIN);

    String headerPrefix = getHeaderPrefix(myLastState);
    if (headerPrefix != null && !headerPrefix.isEmpty()) {
      textGraphics.draw(headerPrefix);
      // Calculate gap so that the rest of the header aligns with the comment body
      FontMetrics fontMetrics = textGraphics.getCurrentFontMetrics();
      int prefixWidth = fontMetrics.stringWidth(headerPrefix);
      int spaceWidth = fontMetrics.charWidth(' ');
      int alignedGap = COMMENT_MARGIN.left - prefixWidth - HEADER_LEFT_MARGIN;
      int gap = alignedGap >= spaceWidth ? alignedGap : HEADER_HORIZONTAL_GAP;
      textGraphics.skipPixels(gap);
    }
    textGraphics.draw(myLastState.getWho());
    textGraphics.skipPixels(HEADER_HORIZONTAL_GAP);
    textGraphics.draw(myLastState.getWhen());
    String additionalText = getHeaderSuffix(myLastState);
    if (additionalText != null && additionalText.length() > 0) {
      textGraphics.skipPixels(HEADER_HORIZONTAL_GAP);
      textGraphics.draw(additionalText);
    }
    return height;
  }

  String getHeaderPrefix(CommentState<T> state) {
    return myHelper == null ? null : myHelper.getHeaderPrefix(state.getComment());
  }

  String getHeaderSuffix(CommentState<T> state) {
    return myHelper == null ? null : myHelper.getHeaderSuffix(state.getComment());
  }

  private int getHeaderHeight(CellState state) {
    return state.getFontMetrics(Font.BOLD).getHeight() + HEADER_BOTTOM_MARGIN;
  }

  private int getContentHeight(CellState state, int width) {
    int minHeight = state.getFontMetrics().getHeight() * COLLAPSED_LINES;
    if (isCollapsed()) {
      return minHeight;
    } else {
      setWrapperFullText();
      return Math.max(minHeight, myWrapper.getPreferedHeight(state, width));
    }
  }

  private void setWrapperFullText() {
    myWrapper.setTextForeground(getForegroundFor(myLastState));
    myWrapper.setText(myLastState.getText());
  }

  private boolean isCollapsed() {
    return myLastState.isCollapsed();
  }

  @Nullable
  public String getTooltip(CellState cellState, CommentState<T> element, Point cellPoint, Rectangle cellRect) {
    int y = cellPoint.y - getHeaderHeight(cellState);
    if (y <= 0) {
      return element.getHeaderTooltipHtml();
    }
    myLastState = element;
    setWrapperFullText();
    return myWrapper.getTooltipAt(cellPoint.x, y);
  }

  public void layoutEditor(CellState state, CommentState<T> item, JComponent container, JLabel prefix, JTextField author,
    JTextField date, JLabel suffix, JComponent content)
  {
    final boolean mac = Env.isMac();

    Insets insets = container.getInsets();
    FontMetrics metrics = mac ? author.getFontMetrics(author.getFont()) : state.getFontMetrics();
    int headerTextHeight = metrics.getHeight();
    int x = HEADER_LEFT_MARGIN;
    int y = insets.top;
    int width;

    width = metrics.stringWidth(prefix.getText());
    if (width > 0)
      x += reshapeClientArea(prefix, prefix.getText(), metrics, headerTextHeight, x, y);
    x += reshapeClientArea(author, author.getText(), metrics, headerTextHeight, x, y);
    x += reshapeClientArea(date, date.getText(), metrics, headerTextHeight, x, y);
    x += reshapeClientArea(suffix, suffix.getText(), metrics, headerTextHeight, x, y);

    x = COMMENT_MARGIN.left;
    y = (mac ? (headerTextHeight + HEADER_BOTTOM_MARGIN) : getHeaderHeight(state)) + COMMENT_MARGIN.top;
    width = getContentWidth(container);
    int height =
      UIUtil.getClientAreaHeight(container) - y - AwtUtil.getInsetHeight(COMMENT_MARGIN) - container.getInsets().bottom;
    AwtUtil.reshapeClientArea(content, x, y, width, height);
  }

  private static int reshapeClientArea(JComponent component, String text, FontMetrics metrics, int headerTextHeight, int x, int y) {
    int width = metrics.stringWidth(text);
    AwtUtil.reshapeClientArea(component, x, y, width + 1, headerTextHeight);
    return width + HEADER_HORIZONTAL_GAP;
  }

  public boolean processMouse(CellState state, CommentState<T> comment, MouseEvent e, Rectangle rect) {
    if (comment.isCollapsed())
      return false;
    int topOffset = getHeaderHeight(state) + COMMENT_MARGIN.top;
    int left = COMMENT_MARGIN.left;
    if (topOffset >= e.getY() || left >= e.getX())
      return false;
    myLastState = comment;
    setupWrapperFullText(topOffset);
    e.translatePoint(-left, -topOffset);
    try {
      myWrapper.getComponent()
        .setBounds(0, 0, rect.width - AwtUtil.getInsetWidth(COMMENT_MARGIN),
          rect.height - topOffset - AwtUtil.getInsetHeight(COMMENT_MARGIN));
      return myWrapper.processMouse(e);
    } finally {
      e.translatePoint(left, topOffset);
    }
  }
}
