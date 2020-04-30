package com.almworks.util.ui.widgets.util;

import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.ui.widgets.CellContext;
import com.almworks.util.ui.widgets.GraphContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * A raw text cell. Converts cell value to text and paints it using specified font and optionally additional graph setup procedure.
 */
public class TextLeafCell<T> extends LeafRectCell<T> {
  private final String myPrototypeValue;
  private final boolean myLeftAlligned;
  private final int myFontStyle;
  @Nullable
  private final Procedure2<GraphContext, ? super T> myGraphSetup;
  private final Convertor<? super T, String> myGetText;

  /**
   * @param prototype used to calculate default preferred size (when no actual cell value available)
   * @param leftAlligned true align text to the left, otherwise to the right
   * @param fontStyle font style used to calculate preferred size and for painting
   * @param graphSetup additional paint setup procedure (optional)
   * @param getText converts cell value to displayable text
   */
  public TextLeafCell(String prototype, boolean leftAlligned, int fontStyle, @Nullable Procedure2<GraphContext, ? super T> graphSetup,
    @NotNull Convertor<? super T, String> getText) {
    myPrototypeValue = prototype;
    myLeftAlligned = leftAlligned;
    myFontStyle = fontStyle;
    myGraphSetup = graphSetup;
    myGetText = getText;
  }

  @NotNull
  @Override
  protected Dimension getPrefSize(CellContext context, T value) {
    FontMetrics metrics = context.getHost().getFontMetrics(myFontStyle);
    String text = value != null ? getText(value) : myPrototypeValue;
    if (text == null) text = myPrototypeValue;
    return new Dimension(metrics.stringWidth(text), metrics.getHeight());
  }

  private String getText(T value) {
    return myGetText.convert(value);
  }

  @Override
  public void paint(@NotNull GraphContext context, @Nullable T flag) {
    if (flag == null) return;
    String text = getText(flag);
    if (text == null || text.length() == 0) return;
    context.setFontStyle(myFontStyle);
    if (myGraphSetup != null) myGraphSetup.invoke(context, flag);
    int y = WidgetUtil.centerYText(context);
    int x = 0;
    if (!myLeftAlligned) {
      FontMetrics metrics = context.getFontMetrics();
      int width = metrics.stringWidth(text);
      x = context.getWidth() - width;
    }
    context.drawTrancatableString(x, y, text);
  }
}
