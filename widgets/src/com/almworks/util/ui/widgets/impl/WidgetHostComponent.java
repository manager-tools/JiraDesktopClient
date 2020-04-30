package com.almworks.util.ui.widgets.impl;

import com.almworks.integers.IntArray;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A widgets host - a bridge to insert widgets into Swing components tree.<br>
 * Typical usage:
 * <pre>
 *  JComponent parent; // parent swing component
 *  T value; // root cell value
 *  WidgetHostComponent host = new WidgetHostComponent();
 *  HostComponentState&lt;T&gt; state = host.{@link #createState createState}(); // create new state for this host
 *  host.{@link #setState(HostComponentState) setState}(state); // set the state as current
 *  state.{@link HostComponentState#setValue(Object) setValue}(value); // set root cell value
 *  parent.add(host); // insert into Swing tree
 * </pre>
 */
public class WidgetHostComponent extends JComponent implements IWidgetHostComponent {
  private static final ComponentUI UI = new HostUI();

  private TraversalPolicy myFocusPolicy;
  private final MyListener myListener;
  private final AtomicBoolean myListenWheel = new AtomicBoolean(true);
  private final IntArray myConsumeEventIds = new IntArray();
  private HostComponentState<?> myState = null;

  public WidgetHostComponent() {
    setUI(UI);
    myListener = new MyListener();
    addMouseListener(myListener);
    addMouseMotionListener(myListener);
    addMouseWheelListener(myListener);
    addKeyListener(myListener);
    setFocusable(true);
    setFocusTraversalKeysEnabled(false);
    //noinspection ThisEscapedInObjectConstruction
    setToolTipText(""); // Enable tooltip support
  }

  public void setListenMouseWheel(boolean listen) {
    if (listen == myListenWheel.get()) return;
    while (listen != myListenWheel.get()) {
      if (myListenWheel.compareAndSet(!listen, listen)) {
        if (listen) addMouseWheelListener(myListener);
        else removeMouseWheelListener(myListener);
      }
    }
  }

  public void addConsumeEventIds(int ... eventIds) {
    myConsumeEventIds.addAll(eventIds);
  }

  @Override
  public void setFocusable(boolean focusable) {
    super.setFocusable(focusable);
    if (isFocusable()) {
      if (myFocusPolicy == null) myFocusPolicy = TraversalPolicy.install(this);
    } else if (myFocusPolicy != null) {
      myFocusPolicy.uninstall();
      myFocusPolicy = null;
    }
  }

  public <T> HostComponentState<T> createState() {
    HostComponentState<T> state = new HostComponentState<T>(this);
    state.putWidgetData(SELECTION_BACKGROUND, UIManager.getColor("List.selectionBackground"));
    state.putWidgetData(SELECTION_FOREGROUND, UIManager.getColor("List.selectionForeground"));
    return state;
  }

  public void setState(@Nullable HostComponentState<?> state) {
    if (state == myState)
      return;
    if (myState != null)
      myState.deactivate();
    myState = state;
    if (myState != null && isDisplayable()) {
      myState.activate();
    }
    updateAll();
  }

  @Override
  public void reshape(int x, int y, int w, int h) {
    int oldWidth = getWidth();
    int oldHeight = getHeight();
    super.reshape(x, y, w, h);
    if (myState == null)
      return;
    if (oldWidth != w || oldHeight != h)
      myState.hostReshaped(oldWidth, oldHeight);
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (myState != null) {
      myState.activate();
    }
  }

  @Override
  public void removeNotify() {
    if (myState != null)
      myState.deactivate();
    super.removeNotify();
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    return myState.getToolTipText(event);
  }

  @Override
  public void setCursor(Cursor cursor) {
    if (cursor == null)
      cursor = Cursor.getDefaultCursor();
    Cursor oldCursor = getCursor();
    if (oldCursor != cursor)
      super.setCursor(cursor);
  }

  public void updateAll() {
    invalidate();
    revalidate();
    repaint();
  }

  public JComponent getHostComponent() {
    return this;
  }

  public void fullRefresh() {
    invalidate();
    revalidate();
    repaintAll();
  }

  public void repaintAll() {
    repaint();
  }

  @Override
  public void updateUI() {
    if (myState != null)
      myState.updateUI();
  }

  public void setRemovingComponent(Component component) {
    if (myFocusPolicy != null) myFocusPolicy.setCurrentRemove(component);
  }

  @Override
  public void widgetRequestsFocus() {
    requestFocusInWindow();
  }

  private void dispatchMouse(MouseEvent e) {
    if (myState != null)
      try {
        myState.dispatchMouse(e);
      } catch (Throwable e1) {
        Log.error(e1);
        e1.printStackTrace();
      }
  }

  private void dispatchKeyEvent(KeyEvent e) {
    if (myState != null)
      myState.dispatchKeyEvent(e);
  }

  public HostComponentState<?> getState() {
    return myState;
  }

  private static class HostUI extends ComponentUI {
    @Override
    public void installUI(JComponent c) {
      WidgetHostComponent host = (WidgetHostComponent) c;
      host.setOpaque(true);
      LookAndFeel.installColorsAndFont(c, "List.background", "List.foreground", "Label.font");
    }

    @Override
    public void paint(Graphics g, JComponent c) {
      HostComponentState<?> state = ((WidgetHostComponent) c).myState;
      if (state != null)
        state.paint((Graphics2D) g);
    }

    @Nullable
    @Override
    public Dimension getPreferredSize(JComponent c) {
      HostComponentState<?> state = ((WidgetHostComponent) c).myState;
      if (state == null)
        return null;
      int width = state.getPreferedWidth();
      if (width < 0)
        return null;
      int height = state.getPreferedHeight(width);
      if (height < 0)
        return null;
      return new Dimension(width, height);
    }

    @Nullable
    @Override
    public Dimension getMaximumSize(JComponent c) {
      return null;
    }

    @Nullable
    @Override
    public Dimension getMinimumSize(JComponent c) {
      return null;
    }

    @Override
    public void update(Graphics g, JComponent c) {
      g.setColor(c.getForeground());
      g.setFont(c.getFont());
      super.update(g, c);
    }
  }


  private class MyListener implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    public void keyTyped(KeyEvent e) {
      dispatchKeyEvent(e);
      eventProcessed(e);
    }

    public void keyPressed(KeyEvent e) {
      dispatchKeyEvent(e);
      eventProcessed(e);
    }

    public void keyReleased(KeyEvent e) {
      dispatchKeyEvent(e);
      eventProcessed(e);
    }

    public void mouseClicked(MouseEvent e) {
      dispatchMouse(e);
      eventProcessed(e);
    }

    public void mousePressed(MouseEvent e) {
      dispatchMouse(e);
      eventProcessed(e);
    }

    public void mouseReleased(MouseEvent e) {
      dispatchMouse(e);
      eventProcessed(e);
    }

    public void mouseEntered(MouseEvent e) {
      dispatchMouse(e);
      eventProcessed(e);
    }

    public void mouseExited(MouseEvent e) {
      dispatchMouse(e);
      eventProcessed(e);
    }

    public void mouseDragged(MouseEvent e) {
      dispatchMouse(e);
      eventProcessed(e);
    }

    public void mouseMoved(MouseEvent e) {
      dispatchMouse(e);
      eventProcessed(e);
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
      dispatchMouse(e);
      eventProcessed(e);
    }

    private void eventProcessed(InputEvent e) {
      if (!e.isConsumed() && myConsumeEventIds.contains(e.getID())) e.consume();
    }
  }
}
