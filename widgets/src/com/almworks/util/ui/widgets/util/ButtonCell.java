package com.almworks.util.ui.widgets.util;

import com.almworks.util.components.AToolbarButton;
import com.almworks.util.ui.actions.EnableState;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.widgets.*;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

public abstract class ButtonCell<T> extends LeafRectCell<T> implements WidgetAttach {
  private static final int STATE_OUT = 0;
  private static final int STATE_HOVER = 1;
  private static final int STATE_PUSHED = 2;
  private static final TypedKey<Integer> MOUSE_STATE = TypedKey.create("mouseState");
  private static final TypedKey<Boolean> WAS_PRESSES = TypedKey.create("wasPressed");
  private final TypedKey<AbstractButton> BUTTON_KEY = TypedKey.create("button");
  private final List<WidgetHost> myHosts = Collections15.arrayList();

  private void processMouse(EventContext context, MouseEventData data, T value) {
    int state;
    int prevState = getMouseState(context);
    boolean actionPreformed = false;
    if (WidgetUtil.isMouseOver(context) && isEnabled(value)) {
      if (data.getEventId() == MouseEvent.MOUSE_PRESSED && data.getButton() == MouseEvent.BUTTON1) {
        state = STATE_PUSHED;
        context.putStateValue(WAS_PRESSES, true, true);
      } else if (data.getEventId() == MouseEvent.MOUSE_RELEASED && data.getButton() == MouseEvent.BUTTON1) {
        state = STATE_HOVER;
        actionPreformed = getWasPressed(context);
        context.putStateValue(WAS_PRESSES, false, true);
      } else state = prevState == STATE_OUT ? STATE_HOVER : prevState;
    } else {
      state = STATE_OUT;
      context.putStateValue(WAS_PRESSES, false, true);
    }
    if (state != prevState) {
      context.putStateValue(MOUSE_STATE, state, true);
      context.repaint();
    }
    if (actionPreformed && isEnabled(value)) actionPerformed(context, value);
  }

  @NotNull
  @Override
  protected Dimension getPrefSize(CellContext context, T value) {
    AbstractButton button = getTempButton(context.getHost(), true, true, value);
    Dimension size = button.getPreferredSize();
    int dimension = Math.max(size.width, size.height);
    size.width = dimension;
    size.height = dimension;
    return size;
  }

  protected void paintButton(GraphContext context, boolean selected, boolean enabled, T value) {
    AbstractButton button = getTempButton(context.getHost(), selected, enabled, value);
    ButtonModel model = button.getModel();
    int state = enabled ? getMouseState(context) : STATE_OUT;
    switch (state) {
    case STATE_OUT: model.setRollover(false); model.setArmed(false); break;
    case STATE_HOVER: model.setRollover(true); model.setArmed(false); break;
    case STATE_PUSHED: model.setRollover(true); model.setArmed(true); break;
    }
    int width = context.getWidth();
    int height = context.getHeight();
    int size = Math.min(width, height);
    int x = center(width, size);
    int y = center(height, size);
    AwtUtil.setSize(button, size, size);
    Graphics2D g = context.getGraphics();
    g.clipRect(x, y, size, size);
    g.translate(x, y);
    button.paint(g);
  }

  private int center(int total, int size) {
    return (total - size) / 2;
  }

  protected abstract void actionPerformed(EventContext context, T value);

  protected abstract void setupButton(AbstractButton button, boolean selected, T value);

  protected abstract String getTooltip(T value);

  protected abstract EnableState getEnableState(T flag);

  protected final boolean isEnabled(T value) {
    return getEnableState(value) == EnableState.ENABLED;
  }

  protected final boolean isVisible(T value) {
    EnableState state = getEnableState(value);
    return state == EnableState.ENABLED || state == EnableState.DISABLED;
  }

  @Override
  public WidgetAttach getAttach() {
    return this;
  }

  @Override
  public void attach(@NotNull WidgetHost host) {
    if (myHosts.contains(host)) return;
    myHosts.add(host);
  }

  @Override
  public void detach(@NotNull WidgetHost host) {
    host.putWidgetData(BUTTON_KEY, null);
    myHosts.remove(host);
  }

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable T value, TypedKey<?> reason) {
    if (reason == MouseEventData.KEY) processMouse(context, context.getData(MouseEventData.KEY), value);
    else if (reason == EventContext.CELL_RESHAPED) processReshape(context);
    else if (reason == TooltipRequest.KEY) {
      //noinspection ConstantConditions
      context.getData(TooltipRequest.KEY).setTooltip(getTooltip(value));
      context.consume();
    }
  }

  private void processReshape(EventContext context) {
    WidgetUtil.reshapeFullCellComponent(context);
  }

  private boolean getWasPressed(CellContext context) {
    Boolean was = context.getStateValue(WAS_PRESSES);
    return was != null && was;
  }

  private int getMouseState(CellContext context) {
    Integer state = context.getStateValue(MOUSE_STATE);
    return state != null ? state : STATE_OUT;
  }

  private AbstractButton getTempButton(WidgetHost host, boolean selected, boolean enabled, T value) {
    AbstractButton button = host.getWidgetData(BUTTON_KEY);
    if (button == null) {
      button = new AToolbarButton();
      host.putWidgetData(BUTTON_KEY, button);
    }
    button.getModel().setSelected(selected);
//    button.getModel().setPressed(selected);
    button.setEnabled(enabled);
    setupButton(button, selected, value);
    return button;
  }
}
