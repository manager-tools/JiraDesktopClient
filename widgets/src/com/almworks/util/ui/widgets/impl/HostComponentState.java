package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.genutil.Log;
import com.almworks.util.ui.widgets.util.WidgetUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Map;

public final class HostComponentState<T> implements WidgetHost {
  private static final Log<HostComponentState> log = Log.get(HostComponentState.class);
  private static final int ROOT_CELL_ID = 0;
  private final IWidgetHostComponent myComponent;
  private final FocusedWidgetManager myFocusManager;
  private final MouseDispatcher myMouseDispatcher;
  private final WidgetLayoutManager myLayoutManager;
  private final EventManager myEventManager;
  private final PaintWidgetManager myPainter;
  private final Map<TypedKey<?>, Object> myWidgetData = Collections15.hashMap();
  private HostCellImpl myRootCell = null;
  private boolean myActive = false;
  private Widget<? super T> myWidget = null;
  private T myValue = null;
  private boolean myFlushInProgress = false;
  private boolean myFlushRequested = false;
  private final Runnable myFlushRunnable = new Runnable() {
    @Override
    public void run() {
      myFlushRequested = false;
      flushPendingEvents();
    }
  };

  @SuppressWarnings({"ThisEscapedInObjectConstruction"})
  public HostComponentState(IWidgetHostComponent component) {
    myComponent = component;
    myMouseDispatcher = new MouseDispatcher(this);
    myFocusManager = new FocusedWidgetManager(this);
    myLayoutManager = new WidgetLayoutManager(this);
    myEventManager = new EventManager(this);
    myPainter = new PaintWidgetManager(this);
  }

  @Override
  public FontMetrics getFontMetrics() {
    JComponent component = myComponent.getHostComponent();
    return getFontMetrics(component.getFont().getStyle());
  }

  @Override
  public FontMetrics getFontMetrics(int style) {
    if (!isActive()) {
      log.error(this, "getFontMetrics");
      //noinspection ReturnOfNull
      return null;
    }
    JComponent component = myComponent.getHostComponent();
    Font font = component.getFont();
    if (font.getStyle() != style) font = font.deriveFont(style);
    return component.getFontMetrics(font);
  }

  @Override
  public JComponent getHostComponent() {
    return myComponent.getHostComponent();
  }

  @Override
  public <T> T getWidgetData(TypedKey<T> key) {
    return key.getFrom(myWidgetData);
  }

  @Override
  public <T> void putWidgetData(TypedKey<T> key, T data) {
    key.putTo(myWidgetData, data);
  }

  public void activate() {
    if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException(Thread.currentThread().toString());
    if (myActive) return;
    myActive = true;
    if (myWidget != null) createRootCell();
  }

  private void createRootCell() {
    WidgetUtil.attachWidget(myWidget, this);
    myRootCell = new HostCellImpl(null, myWidget, ROOT_CELL_ID, this);
    myRootCell.activate(HostCell.Purpose.ROOT);
    myRootCell.activate(HostCell.Purpose.VISIBLE);
    myRootCell.setBounds(0, 0, myComponent.getWidth(), myComponent.getHeight());
    myLayoutManager.invalidate(myRootCell);
    myLayoutManager.flushPending();
    myFocusManager.flushPending();
    flushPendingEvents();
    fullUpdateComponent();
  }

  void flushPendingEvents() {
    if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException(Thread.currentThread().toString());
    myFlushInProgress = true;
    try {
      if (!isActive() || myRootCell == null || !myRootCell.isActive()) return;
      //noinspection StatementWithEmptyBody
      while (myEventManager.flushPending() || myFocusManager.flushPending() || myLayoutManager.flushPending() || myMouseDispatcher.flushPending());
    } finally {
      myFlushInProgress = false;
    }
  }

  private void fullUpdateComponent() {
    myComponent.revalidate();
    myComponent.repaint(0, 0, myComponent.getWidth(), myComponent.getHeight());
  }

  public void deactivate() {
    if (!myActive) return;
    myActive = false;
    removeRootCell();
  }

  private void removeRootCell() {
    if (myRootCell == null) return;
    myRootCell.deleteAll();
    WidgetUtil.detachWidget(myWidget, this);
    myRootCell = null;
    myFocusManager.clear();
    myLayoutManager.clear();
    myMouseDispatcher.clear();
    myEventManager.clear();
  }

  public boolean isActive() {
    return myActive;
  }

  public void hostReshaped(int oldWidth, int oldHeight) {
    if (myRootCell == null) return;
    flushPendingEvents();
    myLayoutManager.hostReshaped(oldWidth, oldHeight);
    flushPendingEvents();
  }

  public void setWidget(Widget<? super T> widget) {
    if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException(Thread.currentThread().toString());
    if (myWidget == widget) return;
    removeRootCell();
    myWidget = widget;
    if (!myActive) return;
    if (myWidget != null) createRootCell();
  }

  public Widget<? super T> getWidget() {
    return myWidget;
  }

  public void setValue(T value) {
    if (Util.equals(value, myValue)) return;
    myValue = value;
    if (!myActive) return;
    HostCellImpl rootCell = getRootCell();
    if (rootCell != null) {
      myEventManager.postEventToAncestors(rootCell, EventContext.VALUE_CHANGED, null);
      myEventManager.flushPending();
    }
    fullUpdateComponent();
  }

  public FocusedWidgetManager getFocusManager() {
    return myFocusManager;
  }

  public WidgetLayoutManager getLayoutManager() {
    return myLayoutManager;
  }

  public MouseDispatcher getMouseDispatcher() {
    return myMouseDispatcher;
  }

  public HostCellImpl getRootCell() {
    return myRootCell;
  }

  public void dispatchMouse(MouseEvent event) {
    if (!myActive) {
      log.error(this, "Not active");
      return;
    }
    flushPendingEvents();
    myMouseDispatcher.dispatch(event);
    flushPendingEvents();
  }

  public void dispatchKeyEvent(KeyEvent event) {
    if (!myActive) {
      log.error(this, "Not active");
      return;
    }
    flushPendingEvents();
    myFocusManager.dispatch(event);
    flushPendingEvents();
  }

  public String getToolTipText(MouseEvent event) {
    HostCellImpl cell = getRootCell();
    if (cell == null) return null;
    int x = event.getX();
    int y = event.getY();
    HostCellImpl child = cell;
    while (child != null) {
      cell = child;
      child = cell.findChild(x, y);
    }
    TooltipRequest request = new TooltipRequest();
    myEventManager.sendEvent(cell, TooltipRequest.KEY, request);
    return request.getTooltip(); 
  }

  public void repaint(int x, int y, int width, int hight) {
    if (myActive)
      myComponent.repaint(x, y, width, hight);
  }

  public T getValue() {
    return myValue;
  }

  public IWidgetHostComponent getHost() {
    return myComponent;
  }

  public int getPreferedWidth() {
    if (myWidget == null || myRootCell == null) return 0;
    return myWidget.getPreferedWidth(myRootCell, getValue());
  }

  public int getPreferedHeight(int width) {
    if (myWidget == null || myRootCell == null) return 0;
    return myWidget.getPreferedHeight(myRootCell, width, getValue());
  }

  public void paint(Graphics2D g) {
    myPainter.paint(g);
  }

  void requestFlush() {
    if (myFlushInProgress || myFlushRequested || !isActive() || myRootCell == null) return;
    myFlushRequested = true;
    SwingUtilities.invokeLater(myFlushRunnable);
  }

  public EventManager getEventManager() {
    return myEventManager;
  }

  public void updateUI() {
    if (isActive() && myRootCell != null && myRootCell.isActive() && myWidget != null)
      updateUI(myRootCell);
  }

  private static void updateUI(HostCellImpl cell) {
    if (!cell.isActive()) return;
    Widget<?> widget = cell.getWidget();
    widget.updateUI(cell);
    for (HostCellImpl child : cell.getChildren())
      updateUI(child);
  }
}
