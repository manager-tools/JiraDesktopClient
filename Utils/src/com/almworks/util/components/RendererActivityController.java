package com.almworks.util.components;

import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * @author dyoma
 */
public class RendererActivityController {
  private static final ComponentProperty<Rectangle> LAYOUT_AREA = ComponentProperty.createProperty("layoutArea");
  private final FocusListener myFocusLostListener = new MyFocusListener();
  private final JComponent myComponent;

  private WeakReference<Component> myPrevFocusOwner = null;
  private JComponent myLiveComponent;
  private boolean myChangingComponent = false;
  private Cursor mySavedCursor;
  private final Rectangle myMouseArea = new Rectangle();
  private final Map<TypedKey<?>, JComponent> myAdditionalComponents = Collections15.hashMap();

  public RendererActivityController(JComponent component) {
    myComponent = component;
  }

  public void setLiveComponent(JComponent component, Rectangle rectangle) {
    assert !myChangingComponent;
    myChangingComponent = true;
    try {
      Component focusOwner = getPrevFocusOwner();
      removeLiveComponent(false);
      if (focusOwner == null)
        focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
      if (focusOwner != null)
        myPrevFocusOwner = new WeakReference<Component>(focusOwner);
      myLiveComponent = component;
      doAddComponent(rectangle, myLiveComponent);
      myLiveComponent.requestFocusInWindow();
      myLiveComponent.addFocusListener(myFocusLostListener);
    } finally {
      assert myChangingComponent;
      myChangingComponent = false;
    }
  }

  private void doAddComponent(Rectangle rectangle, JComponent component) {
    Insets insets = myComponent.getInsets();
    rectangle.x += insets.left;
    rectangle.y += insets.top;
    myComponent.add(component);
    LAYOUT_AREA.putClientValue(component, new Rectangle(rectangle));
    myComponent.invalidate();
    myComponent.revalidate();
    myComponent.repaint();
  }

  public void removeLiveComponent(boolean restoreFocus) {
    boolean alreadyChanging = myChangingComponent;
    myChangingComponent = true;
    try {
      if (myLiveComponent != null) {
        myLiveComponent.removeFocusListener(myFocusLostListener);
        myComponent.remove(myLiveComponent);
        myLiveComponent = null;
        myComponent.invalidate();
        myComponent.revalidate();
        myComponent.repaint();
        Component prevFocusOwner = getPrevFocusOwner();
        if (restoreFocus && prevFocusOwner != null && prevFocusOwner.isDisplayable())
          prevFocusOwner.requestFocusInWindow();
        myPrevFocusOwner = null;
      }
    } finally {
      myChangingComponent = alreadyChanging;
    }
  }

  public JComponent getLiveComponent() {
    return myLiveComponent;
  }

  public JComponent getWholeComponent() {
    return myComponent;
  }

  @Nullable
  private Component getPrevFocusOwner() {
    return myPrevFocusOwner != null ? myPrevFocusOwner.get() : null;
  }

  public void layout() {
    if (myLiveComponent != null)
      layoutComponent(myLiveComponent);
    for (JComponent component : myAdditionalComponents.values())
      layoutComponent(component);
  }

  private void layoutComponent(JComponent component) {
    Rectangle rectangle = LAYOUT_AREA.getClientValue(component);
    assert rectangle != null;
    component.setBounds(rectangle);
  }

  public boolean hasActivity(MouseEvent e) {
    if (myMouseArea == null)
      return false;
    if (myMouseArea.contains(e.getPoint()))
      return true;
    removeMouseActivity();
    return false;
  }

  public void setCursor(Cursor cursor, Rectangle rectangle) {
    removeMouseActivity();
    mySavedCursor = myComponent.getCursor();
    myComponent.setCursor(cursor);
    copyRectangle(myMouseArea, rectangle);
  }

  public void removeMouseActivity() {
    if (mySavedCursor != null) {
      myComponent.setCursor(mySavedCursor);
      mySavedCursor = null;
      myMouseArea.setBounds(UIUtil.RECT_0000);
    }
  }

  private void copyRectangle(Rectangle dest, Rectangle source) {
    AwtUtil.copyClientRectangle(myComponent, dest, source);
  }

  public boolean focusNextComponent(RendererContext context, com.almworks.util.components.renderer.Renderer renderer,
    boolean next)
  {
    if (myChangingComponent)
      return true;
    Rectangle area = new Rectangle();
    JComponent component = renderer.getNextLiveComponent(context, myLiveComponent, area, next);
    if (component != null) {
      setLiveComponent(component, area);
      return true;
    }
    return false;
  }

  public boolean hasComponent(TypedKey<? extends JComponent> key) {
    return myAdditionalComponents.containsKey(key);
  }

  public <T extends JComponent> void addComponent(TypedKey<? extends T> key, T component, Rectangle area) {
    if (myAdditionalComponents.containsKey(key)) {
      assert false;
      return;
    }
    myAdditionalComponents.put(key, component);
    doAddComponent(area, component);
  }

  public void removeComponent(TypedKey<AToolbarButton> key) {
    JComponent component = myAdditionalComponents.get(key);
    if (component == null)
      return;
    myComponent.remove(component);
    myAdditionalComponents.remove(key);
    myComponent.invalidate();
    myComponent.revalidate();
    myComponent.repaint();
  }

  public void repaint(Rectangle rectangle) {
    AwtUtil.repaintClientRectangle(myComponent, rectangle);
  }

  public Rectangle getLiveRectanle() {
    if (myLiveComponent == null)
      return null;
    Rectangle rect = LAYOUT_AREA.getClientValue(myLiveComponent);
    return rect == null ? null : new Rectangle(rect);
  }

  private class MyFocusListener implements FocusListener {
    public void focusGained(FocusEvent e) {

    }

    public void focusLost(FocusEvent e) {
      if (myLiveComponent != null && !e.isTemporary()) {
        myLiveComponent.removeFocusListener(this);
        removeLiveComponent(false);
      }
    }
  }
}
