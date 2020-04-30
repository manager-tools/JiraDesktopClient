package com.almworks.util.components.renderer.table;

import com.almworks.util.commons.Function;
import com.almworks.util.components.renderer.FontStyle;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.ui.swing.AwtUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TextCell implements TableRendererCell {
  private final FontStyle myFontStyle;
  private final Function<RendererContext, String> myTextGetter;

  public TextCell(FontStyle fontStyle, Function<RendererContext, String> textGetter) {
    myFontStyle = fontStyle;
    myTextGetter = textGetter;
  }

  public int getHeight(RendererContext context) {
    return myFontStyle.getHeight(context);
  }

  public int getWidth(RendererContext context) {
    return myFontStyle.getStringWidth(context, getText(context));
  }

  public String getText(RendererContext context) {
    String text = myTextGetter.invoke(context);
    return text != null ? text : "";
  }

  public void paint(Graphics g, int x, int y, RendererContext context) {
    final String text = getText(context);
    if(x < 0) {
      // Simulating right label alignment on the Mac; negative x is a hint from TwoColumnLine.
      // todo: find a less kludgy solution.
      x = -x - myFontStyle.getStringWidth(context, text);
    }
    myFontStyle.paint(g, x, y, context, text);
  }

  public void invalidateLayout(RendererContext context) {
  }

  @Nullable
  public RendererActivity getActivityAt(int id, int x, int y, RendererContext context, Rectangle rectangle) {
    if (id != MouseEvent.MOUSE_CLICKED)
      return null;
    JTextField field = createLifeComponent(context, rectangle);
    return field != null ? LiveComponent.create(field, rectangle) : null;
  }

  @Nullable
  public JComponent getNextLifeComponent(@NotNull RendererContext context, @Nullable JComponent current,
    @NotNull Rectangle targetArea, boolean next)
  {
    JTextField result = current != null ? null : createLifeComponent(context, targetArea);
    if (result != null)
      LiveComponent.fixComponentArea(result, targetArea);
    return result;
  }

  @Nullable
  protected JTextField createLifeComponent(RendererContext context, Rectangle rectangle) {
    JTextField field = new JTextField(getText(context));
    field.setMargin(AwtUtil.EMPTY_INSETS);
    field.setEditable(false);
    rectangle.height = getHeight(context);
    return field;
  }

  public static TextCell label(String label) {
    return label(label, FontStyle.PLAIN, false);
  }

  public static TextCell label(final String label, FontStyle fontStyle, final boolean allowLife) {
    return new TextCell(fontStyle, Function.Const.create(label).<RendererContext>f()) {
      @Nullable
      protected JTextField createLifeComponent(RendererContext context, Rectangle rectangle) {
        return allowLife ? super.createLifeComponent(context, rectangle) : null;
      }
    };
  }
}
