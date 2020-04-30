package com.almworks.util.components.renderer.table;

import com.almworks.util.components.RendererActivityController;
import com.almworks.util.components.ScrollPaneBorder;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;

public class LiveComponent implements RendererActivity {
  private final JComponent myComponent;

  private LiveComponent(JComponent component) {
    myComponent = component;
  }

  public void apply(RendererActivityController controller, Rectangle rectangle) {
    controller.setLiveComponent(myComponent, rectangle);
  }

  public <T> void storeValue(ComponentProperty<T> key, T value) {
    key.putClientValue(myComponent, value);
  }

  public static LiveComponent create(JComponent component, Rectangle area) {
    fixComponentArea(component, area);
    return new LiveComponent(component);
  }

  public static void fixComponentArea(JComponent component, Rectangle area) {
    Insets componentInsets = getInsets(component);
    area.x -= componentInsets.left;
    area.y -= componentInsets.top;
    area.width += AwtUtil.getInsetWidth(componentInsets);
    area.height += AwtUtil.getInsetHeight(componentInsets);
  }

  private static Insets getInsets(JComponent component) {
    if (component instanceof ScrollPaneBorder) {
      ScrollPaneBorder border = (ScrollPaneBorder) component;
      Component view = border.getViewport().getView();
      assert view instanceof JComponent : view;
      return AwtUtil.uniteInsetsFromTo((JComponent) view, border);
    } else {
      return component.getInsets();
    }
  }
}
