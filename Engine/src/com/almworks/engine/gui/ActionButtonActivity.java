package com.almworks.engine.gui;

import com.almworks.util.components.AToolbarButton;
import com.almworks.util.components.RendererActivityController;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.TypedKey;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class ActionButtonActivity implements RendererActivity {
  private static final int HALF_GAP = ActionCellDecorator.ICON_GAP / 2;

  private final TypedKey<AToolbarButton> myButtonKey;
  private final Icon myIcon;
  private final PerformActionActivity myPerform;

  public ActionButtonActivity(TypedKey<AToolbarButton> buttonKey, AnActionListener listener, String name, Icon icon) {
    myButtonKey = buttonKey;
    myIcon = icon;
    myPerform = new PerformActionActivity(listener, name);
  }

  public void apply(RendererActivityController controller, Rectangle rectangle) {
    if (controller.hasComponent(myButtonKey))
      return;
    PropertyMap presentation = new PropertyMap();
    presentation.put(PresentationKey.ENABLE, EnableState.ENABLED);
    presentation.put(PresentationKey.NAME, myPerform.getTooltip());
    presentation.put(PresentationKey.SMALL_ICON, myIcon);
    AToolbarButton button = new AToolbarButton(ActionUtil.createAction(presentation, myPerform.getAction()));
    button.setBorder(new EmptyBorder(HALF_GAP, HALF_GAP, HALF_GAP, HALF_GAP));
    Insets insets = button.getInsets();
    rectangle.x -= insets.left;
    rectangle.y -= insets.top;
    rectangle.width += AwtUtil.getInsetWidth(insets);
    rectangle.height += AwtUtil.getInsetHeight(insets);
    button.overridePresentation(PresentationMapping.VISIBLE_NONAME);
    controller.addComponent(myButtonKey, button, rectangle);
    JComponent wholeComponent = controller.getWholeComponent();
    wholeComponent.addMouseListener(new MyMouseListener(controller, myButtonKey, rectangle));
  }

  public <T> void storeValue(ComponentProperty<T> key, T value) {

  }

  public boolean hasComponent(RendererContext context) {
    return context.getController().hasComponent(myButtonKey);
  }

  public void removeComponent(RendererContext context) {
    context.getController().removeComponent(myButtonKey);
  }

  public int getIconWidth() {
    return myIcon.getIconWidth() + HALF_GAP * 6;
  }

  public int getIconHeight() {
    return myIcon.getIconHeight();
  }

  public void paintIcon(JComponent component, Graphics g, int x, int y) {
    myIcon.paintIcon(component, g, x + HALF_GAP * 3, y);
  }

  public RendererActivity getPerform() {
    return myPerform;
  }

  private static class MyMouseListener extends MouseAdapter implements MouseMotionListener {
    private final RendererActivityController myController;
    private final TypedKey<AToolbarButton> myKey;
    private final Rectangle myRectangle;

    public MyMouseListener(RendererActivityController controller, TypedKey<AToolbarButton> key, Rectangle rectangle) {
      myController = controller;
      myKey = key;
      myRectangle = rectangle;
    }

    public void mouseDragged(MouseEvent e) {
      processMousePosition(e.getPoint());
    }

    public void mouseMoved(MouseEvent e) {
      processMousePosition(e.getPoint());
    }

    private void processMousePosition(Point p) {
      int x = p.x;
      int y = p.y;
      Insets insets = myController.getWholeComponent().getInsets();
      x += insets.left;
      y += insets.top;
      if (!myRectangle.contains(x, y))
        removeComponent();
    }

    private void removeComponent() {
      myController.removeComponent(myKey);
      JComponent component = myController.getWholeComponent();
      component.removeMouseListener(this);
      component.removeMouseMotionListener(this);
    }
  }
}
