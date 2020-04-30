package com.almworks.util.ui.widgets.impl.demo;

import com.almworks.util.commons.Procedure;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.util.ActiveCellCollector;
import com.almworks.util.ui.widgets.util.WidgetUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;

class EditTextWidget<T> implements Widget<T>, CellActivate {
  private static final TypedKey<Object> VALUE = TypedKey.create("value");
  private static final TypedKey<DetachComposite> LIFE = TypedKey.create("life");
  private final TextAccessor<? super T> myAccessor;
  private final ActiveCellCollector myCells = new ActiveCellCollector();
  private final Procedure<HostCell> myListener = new Procedure<HostCell>() {
    @Override
    public void invoke(HostCell arg) {
      onValueUpdated(arg);
    }
  };

  public EditTextWidget(TextAccessor<? super T> accessor) {
    myAccessor = accessor;
  }

  @Override
  public int getPreferedWidth(@NotNull CellContext context, @Nullable T value) {
    checkSubscription(context, value);
    if (value == null) return 0;
    String text = myAccessor.getText(value);
    if (text == null) return 0;
    FontMetrics metrics = context.getHost().getFontMetrics();
    return metrics.stringWidth(text);
  }

  @Override
  public int getPreferedHeight(@NotNull CellContext context, int width, @Nullable T value) {
    checkSubscription(context, value);
    return context.getHost().getFontMetrics().getHeight();
  }

  @Override
  public void paint(@NotNull GraphContext context, @Nullable T value) {
    checkSubscription(context, value);
    if (value == null) return;
    String text = myAccessor.getText(value);
    if (text == null) return;
    FontMetrics metrics = context.getFontMetrics();
    context.drawString(0, metrics.getAscent(), text);
  }

  @SuppressWarnings({"ConstantConditions"})
  @Override
  public void processEvent(@NotNull EventContext context, @Nullable T value, TypedKey<?> reason) {
    checkSubscription(context, value);
    //noinspection IfStatementWithTooManyBranches
    if (reason == EventContext.FOCUS_GAINED) showEditor(context, value);
    else if (reason == FocusTraverse.KEY) context.getData(FocusTraverse.KEY).focusMe();
    else if (reason == EventContext.CELL_RESHAPED) processReshape(context);
    else if (reason == MouseEventData.KEY) processMouse(context, value, context.getData(MouseEventData.KEY));
  }

  private void processMouse(EventContext context, T value, MouseEventData mouse) {
    if (mouse.getEventId() == MouseEvent.MOUSE_PRESSED && mouse.getButton() == MouseEvent.BUTTON1) showEditor(context, value);
  }

  private static void processReshape(CellContext context) {
    JComponent component = context.getLiveComponent();
    if (!(component instanceof JTextField)) return;
    WidgetUtil.reshapeLifeTextField((JTextField) component, context);
  }

  private void showEditor(EventContext context, T value) {
    checkSubscription(context, value);
    if (value == null) return;
    JComponent component = context.getLiveComponent();
    if (component != null) return;
    final HostCell cell = context.getActiveCell();
    if (cell == null) return;
    String text = myAccessor.getText(value);
    if (text == null) text = "";
    final JTextField field = new JTextField();
    field.setText(text);
    field.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) cell.removeLiveComponent();
      }
    });
    field.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void documentChanged(DocumentEvent e) {
        if (cell.getLiveComponent() != field) return;
        T val = (T) cell.getStateValue(VALUE);
        if (val == null) return;
        myAccessor.setText(val, field.getText());
      }
    });
    field.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {}

      @Override
      public void focusLost(FocusEvent e) {
        cell.removeLiveComponent();
      }
    });
    cell.setLiveComponent(field);
    field.requestFocusInWindow();
  }

  @Override
  public Object getChildValue(@NotNull CellContext context, int cellId, @Nullable T value) {
    return value;
  }

  @Override
  public void layout(LayoutContext context, T value, @Nullable ModifiableHostCell cell) {}

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
  public void updateUI(HostCell cell) {
    cell.invalidate();
  }

  @Override
  public void activate(@NotNull HostCell cell) {
    myCells.activate(cell);
    T value = cell.restoreValue(this);
    checkSubscription(cell, value);
    if (value != null) listenValue(cell, value);
  }

  @Override
  public void deactivate(@NotNull HostCell cell, JComponent liveComponent) {
    myCells.deactivate(cell, liveComponent);
    stopListenValue(cell);
  }

  private static void stopListenValue(CellContext cell) {
    DetachComposite life = cell.getStateValue(LIFE);
    if (life != null) life.detach();
    cell.putStateValue(LIFE, null, true);
  }

  private void checkSubscription(CellContext context, T value) {
    T subcsribedTo = (T) context.getStateValue(VALUE);
    if (subcsribedTo == value) return;
    stopListenValue(context);
    context.putStateValue(VALUE, value, true);
    HostCell cell = context.getActiveCell();
    if (cell == null) return;
    listenValue(cell, value);
  }

  private void listenValue(HostCell cell, T value) {
    if (value == null) return;
    DetachComposite detach = new DetachComposite();
    if (myAccessor.addListener(detach, myListener, cell, value)) cell.putStateValue(LIFE, detach, true);
    else detach.detach();
  }

  private void onValueUpdated(HostCell arg) {
    arg.invalidate();
    arg.repaint();
  }

  interface TextAccessor<T> {
    String getText(T value);

    void setText(T value, String text);

    <X> boolean addListener(Lifespan life, Procedure<X> listener, X callbackValue, T value);
  }
}
