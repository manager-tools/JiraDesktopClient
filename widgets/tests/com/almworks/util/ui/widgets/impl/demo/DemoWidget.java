package com.almworks.util.ui.widgets.impl.demo;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.genutil.Log;
import com.almworks.util.ui.widgets.util.WidgetUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

class DemoWidget implements Widget<Node>, CellActivate {
  @SuppressWarnings({"InnerClassFieldHidesOuterClassField"})
  private static final TypedKey<int[]> SIZE = TypedKey.create("size");
  private static final TypedKey<Boolean> HOVER = TypedKey.create("hover");
  private static final TypedKey<Point> DRAG_START = TypedKey.create("drag");

  @Override
  public int getPreferedWidth(@NotNull CellContext context, @Nullable Node value) {
    return getPrefSize(context, value, 0);
  }

  @Override
  public int getPreferedHeight(@NotNull CellContext context, int width, @Nullable Node value) {
    return getPrefSize(context, value, 1);
  }

  private static int getPrefSize(CellContext context, Node value, int axis) {
    int[] size = context.getStateValue(SIZE);
    if (size == null) {
      size = new int[2];
      context.putStateValue(SIZE, size, false);
    }
    value.getSize(size);
    return size[axis];
  }

  @Override
  public void paint(@NotNull GraphContext context, @Nullable Node value) {
    if (value == null) return;
    boolean selected = value.isSelected();
    Color color;
    if (context.getStateValue(HOVER, false)) color = Color.BLUE;
    else if (selected) color = Color.GREEN;
    else color = Color.BLACK;
    context.setColor(color);
    int width = value.getWidth();
    int height = value.getHeight();
    context.drawRect(0, 0, width, height);
    FontMetrics metrics = context.getFontMetrics();
    context.drawString(0, metrics.getAscent(), value.getOrigin(true) + "@" + value.getOrigin(false) + ":" + value.getWidth() + "x" + value.getHeight());
    if (context.isFocused()) {
      context.drawRect(0, 0, width, height);
      context.drawRect(1, 1, width - 2, height - 2);
      context.drawRect(2, 2, width - 4, height - 4);
    }
  }

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable Node value, TypedKey<?> reason) {
    //noinspection IfStatementWithTooManyBranches
    if (reason == MouseEventData.KEY) processMouse(context, value, context.getData(MouseEventData.KEY));
    else if (reason == EventContext.KEY_EVENT) processKey(context, value, context.getData(EventContext.KEY_EVENT));
    else if (reason == EventContext.FOCUS_GAINED || reason == EventContext.FOCUS_LOST) processFocus(context, reason == EventContext.FOCUS_GAINED);
    else if (reason == EventContext.CELL_RESHAPED) processReshape(context);
    else if (reason == FocusTraverse.KEY) processFocusTraverse(context, context.getData(FocusTraverse.KEY), value);
  }

  private static void processFocusTraverse(EventContext context, FocusTraverse data, Node value) {
    if (data.isTraverse() && value.getChildCount() == 0) data.focusMe();
    else data.defaultTraverse(context, 0, value.getChildCount() - 1);
  }

  private static void processReshape(CellContext context) {
    HostCell cell = context.getActiveCell();
    if (cell == null) return;
    JComponent component = cell.getLiveComponent();
    if (!(component instanceof JTextField)) return;
    JTextField field = (JTextField) component;
    WidgetUtil.reshapeLifeTextField(field, context);
  }

  private static void processFocus(CellContext context, boolean gained) {
    context.repaint();
    if (!gained) context.removeLiveComponent();
  }

  private static void processKey(EventContext context, Node value, KeyEvent event) {
    if (event.getID() != KeyEvent.KEY_PRESSED) return;
    Node selected = value.getRoot().getSelected();
    if (selected == null) return;
    int dx = 0, dy = 0;
    switch (event.getKeyCode()) {
      case KeyEvent.VK_LEFT: dx = -1; break;
      case KeyEvent.VK_RIGHT: dx = 1; break;
      case KeyEvent.VK_UP: dy = -1; break;
      case KeyEvent.VK_DOWN: dy = 1; break;
      case KeyEvent.VK_ENTER: showTextField(context); break;
      default:return;
    }
    context.consume();
    if ((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK)  != 0)
      selected.resize(dx, dy);
    else
      selected.move(dx, dy);
  }

  private static void showTextField(EventContext context) {
    final HostCell cell = context.getActiveCell();
    if (cell == null) return;
    if (cell.getLiveComponent() != null) return;
    JTextField field = new JTextField();
    field.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) cell.setLiveComponent(null);
      }
    });
    cell.setLiveComponent(field);
    field.requestFocusInWindow();
  }

  private static void processMouse(EventContext context, Node value, MouseEventData event) {
    int eventId = event.getEventId();
    if (eventId != MouseEvent.MOUSE_DRAGGED && eventId != MouseEvent.MOUSE_MOVED) context.putStateValue(DRAG_START, null, true);
    int x = event.getX();
    int y = event.getY();
    boolean hover = context.getStateValue(HOVER, false);
    //noinspection SwitchStatementDensity
    switch (eventId) {
      case MouseEvent.MOUSE_PRESSED:
        int button = event.getButton();
        if (button == MouseEvent.BUTTON1) {
          if (!value.isSelected())
            value.getRoot().setSelected(value);
          context.putStateValue(DRAG_START, new Point(context.getHostX() + x, context.getHostY() + y), true);
        } else if (button == MouseEvent.BUTTON2) context.requestFocus();
        if (button == MouseEvent.BUTTON3) value.insertChild(x, y, context.getWidth(), context.getHeight());
        context.consume();
        break;
      case MouseEvent.MOUSE_MOVED:
        context.consume();
        if (!hover) {
          context.putStateValue(HOVER, true, true);
          context.repaint();
        }
      {
        Point drag = context.getStateValue(DRAG_START);
        if (drag != null) {
          drag.setLocation(context.getHostX() + x, context.getHostY() + y);
          context.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
      }
      break;
      case MouseEvent.MOUSE_EXITED:
        if (hover) {
          context.putStateValue(HOVER, false, true);
          context.repaint();
        }
        break;
      case MouseEvent.MOUSE_DRAGGED:
        Point drag = context.getStateValue(DRAG_START);
        int hostX = context.getHostX() + x;
        int hostY = context.getHostY() + y;
        if (drag == null) drag = new Point(hostX, hostY);
        int dx = hostX - drag.x;
        int dy = hostY - drag.y;
        value.move(dx, dy);
        drag.setLocation(hostX, hostY);
        context.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        break;
    }
  }

  @Nullable
  @Override
  public Object getChildValue(@NotNull CellContext context, int cellId, @Nullable Node value) {
    return value != null ? value.getChild(cellId) : null;
  }

  @Override
  public void layout(LayoutContext context, Node value, @Nullable ModifiableHostCell cell) {
    WidgetState.obtain(context, value);
    if (cell != null) layoutCell(context, value, cell);
    else if (!value.isEmpty()) {
      for (int i = 0; i < value.getChildCount(); i++) {
        Node subNode = value.getChild(i);
        layoutChild(context, i, this, subNode);
      }
    }
  }

  private static void layoutChild(LayoutContext context, int id, Widget<?> widget, Node subNode) {
    context.setChildBounds(id, widget, subNode.getOrigin(true), subNode.getOrigin(false), subNode.getWidth(), subNode.getHeight(), null);
  }

  private void layoutCell(LayoutContext context, Node value, ModifiableHostCell cell) {
    int id = cell.getId();
    Node child = value.getChild(id);
    layoutChild(context, id, this, child);
  }

  @Nullable
  @Override
  public WidgetAttach getAttach() {
    return null;
  }

  @Override
  public CellActivate getActivate() {
    return this;
  }

  @Override
  public void updateUI(HostCell cell) {}

  @Override
  public void activate(@NotNull HostCell cell) {
  }

  @Override
  public void deactivate(@NotNull HostCell cell, JComponent liveComponent) {
    WidgetState.deactivate(cell);
  }

  private static class WidgetState {
    @SuppressWarnings({"InnerClassFieldHidesOuterClassField"})
    private static final Log<WidgetState> log = Log.get(WidgetState.class);
    private static final TypedKey<WidgetState> KEY = TypedKey.create("state");

    private DetachComposite myLife = null;
    private final HostCell myCell;
    private Node myNode;

    private WidgetState(HostCell cell) {
      myCell = cell;
    }

    @Nullable
    public static WidgetState obtain(CellContext holder, Node value) {
      HostCell cell = holder.getActiveCell();
      if (cell == null) return null;
      WidgetState state = cell.getStateValue(KEY);
      if (state == null) {
        state = new WidgetState(cell);
        state.activate(value);
        cell.putStateValue(KEY, state, true);
      } else {
        if (value != state.myNode) {
          state.deactive();
          state.activate(value);
        }
      }
      return state;
    }

    private void deactive() {
      if (myLife != null) myLife.detach();
      myLife = null;
    }

    private void activate(Node node) {
      if (myLife != null) {
        log.error(this, "Active");
        myLife.detach();
        myLife = null;
      }
      myNode = node;
      myLife = new DetachComposite();
      node.getSelectedModifable().addAWTChangeListener(myLife, new ChangeListener() {
        @Override
        public void onChange() {
          myCell.repaint();
        }
      });
      node.getLayoutModifiable().addAWTChangeListener(myLife, new ChangeListener() {
        @Override
        public void onChange() {
          myCell.invalidate();
        }
      });
    }

    public static void deactivate(HostCell cell) {
      WidgetState state = cell.getStateValue(KEY);
      if (state != null) {
        state.deactive();
        cell.putStateValue(KEY, null, true);
      }
    }
  }
}
