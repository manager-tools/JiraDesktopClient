package com.almworks.actions.console;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntList;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Function;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.Shortcuts;
import com.almworks.util.ui.swing.SwingTreeUtil;
import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.impl.HostComponentState;
import com.almworks.util.ui.widgets.impl.WidgetHostComponent;
import com.almworks.util.ui.widgets.impl.WidgetHostUtil;
import com.almworks.util.ui.widgets.util.WidgetUtil;
import com.almworks.util.ui.widgets.util.list.ColumnListWidget;
import com.almworks.util.ui.widgets.util.list.ConstHeight;
import com.almworks.util.ui.widgets.util.list.ListSelectionProcessor;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;

public class CompletionFieldController<T> {
  private static final ConstHeight TABLE_POLICY = new ConstHeight(0, 0, null);
  private static final IntList FORWARD_KEY_CODES = IntArray.create(KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_PAGE_UP, KeyEvent.VK_HOME, KeyEvent.VK_END);

  private final JTextComponent myTextComponent;
  private final Lifecycle myWorkingCycle = new Lifecycle(false);
  private final PopupController myPopup;
  private final WidgetController<T> myList = new WidgetController<T>();
  private Function<Lifespan, VariantModelController<T>> myModelFactory;
  private VariantModelController<T> myModel = null;
  private int myRowCount = 10;
  private boolean myConsumeEsc = true;

  public CompletionFieldController(JTextComponent textComponent) {
    myTextComponent = textComponent;
    myPopup = new PopupController(myList.createScrollPane());
    myPopup.setListenFocus(false);
    myTextComponent.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        if (!myWorkingCycle.isCycleStarted()) return;
        if (!myPopup.isShowing()) showDropDown();
      }

      @Override
      public void focusLost(FocusEvent e) {
      }
    });
    myTextComponent.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && (e.getModifiersEx() & Shortcuts.KEYBOARD_MODIFIERS) == 0) {
          if (myPopup.isShowing()) {
            myPopup.hide();
            if (myConsumeEsc) e.consume();
            return;
          }
        }
        maybeRedispatch(e);
      }

      @Override
      public void keyReleased(KeyEvent e) {
        maybeRedispatch(e);
      }

      private void maybeRedispatch(KeyEvent e) {
        if (!myWorkingCycle.isCycleStarted()) return;
        int keyCode = e.getKeyCode();
        if (!isForwardCode(keyCode)) return;
        if (!myPopup.isShowing()) {
          if (keyCode == KeyEvent.VK_DOWN) showDropDown();
          e.consume();
        }
        myList.dispatchEvent(e);
      }

      private boolean isForwardCode(int keyCode) {
        return FORWARD_KEY_CODES.contains(keyCode);
      }
    });
  }

  public void onAddNotify() {
    LogHelper.assertError(myModel == null);
    if (myModelFactory == null) return;
    myWorkingCycle.cycle();
    myModel = myModelFactory.invoke(myWorkingCycle.lifespan());
    myModel.setText(myTextComponent.getText());
    UIUtil.addTextListener(myWorkingCycle.lifespan(), myTextComponent, new ChangeListener() {
      private String myPrev = myTextComponent.getText();

      @Override
      public void onChange() {
        String text = myTextComponent.getText();
        if (myPrev.equals(text)) return;
        myPrev = text;
        myModel.setText(text);
      }
    });
    AListModel<T> variants = myModel.getVariants();
    myList.setModel(variants);
    myWorkingCycle.lifespan().add(variants.addListener(new AListModel.Adapter<T>() {
      @Override
      public void onInsert(int index, int length) {
        updatePopupLocation();
      }

      @Override
      public void onRemove(int index, int length, AListModel.RemovedEvent<T> event) {
        updatePopupLocation();
      }
    }));
  }

  public ColumnListWidget<T> getListWidget() {
    return myList.getWidget();
  }

  public void setRowCount(int rowCount) {
    myRowCount = rowCount;
  }

  public ListSelectionProcessor<T> getListSelection() {
    return myList.getListSelection();
  }

  /**
   * This method defines ESC behaviour when drop-down is visible. If drop-down is not visible ESC event is ignored and other components always get it.<br>
   * If set to true closes popup and consume event, preventing it farther dispatch.<br>
   * If set to false closes popup and allows other components to process ESC key event.
   */
  public void setConsumeEsc(boolean consumeEsc) {
    myConsumeEsc = consumeEsc;
  }

  public PopupController getPopup() {
    return myPopup;
  }

  public void onRemoveNotify() {
    myWorkingCycle.cycleEnd();
    myPopup.hide();
    myList.setModel(AListModel.EMPTY);
    myModel = null;
  }

  public void setModelFactory(Function<Lifespan, VariantModelController<T>> modelFactory) {
    LogHelper.assertError(myModel == null, "On the fly model change not supported", myModel);
    myModelFactory = modelFactory;
  }

  public void showDropDown() {
    if (!myWorkingCycle.isCycleStarted()) {
      LogHelper.error("Not started");
      return;
    }
    Window parentWindow = SwingTreeUtil.findAncestorOfType(myTextComponent, Window.class);
    if (parentWindow == null) {
      LogHelper.error("No parent window");
      return;
    }
    JWindow window = myPopup.showAt(parentWindow, getPopupLocation());
    FocusTracker tracker = new FocusTracker(window);
    tracker.disposeRootWhenFocusLost();
    tracker.addComponents(myTextComponent);
    tracker.start();
    parentWindow.addWindowStateListener(new WindowStateListener() {
      @Override
      public void windowStateChanged(WindowEvent e) {
        if (e.getNewState() == WindowEvent.WINDOW_ICONIFIED) myPopup.hide();
        else updatePopupLocation();
      }
    });
    ComponentAdapter locationTracker = new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        updatePopupLocation();
      }

      @Override
      public void componentMoved(ComponentEvent e) {
        updatePopupLocation();
      }
    };
    parentWindow.addComponentListener(locationTracker);
    myTextComponent.addComponentListener(locationTracker);
  }

  private void updatePopupLocation() {
    JWindow window = myPopup.getShowingPopup();
    if (window == null) return;
    Rectangle current = window.getBounds();
    Rectangle location = getPopupLocation();
    if (location != null && !location.equals(current)) {
      window.setBounds(location);
      if (!current.getSize().equals(location.getSize())) window.getRootPane().revalidate();
    }
  }

  @Nullable
  private Rectangle getPopupLocation() {
    AListModel<? extends T> variants = myModel.getVariants();
    if (variants == null) return null;
    Point location = myTextComponent.getLocationOnScreen();
    Dimension textSize = myTextComponent.getSize();
    location.y += textSize.height;
    int rowCount = Math.min(myRowCount, Math.max(1, variants.getSize()));
    int listHeight = myList.getRowHeight() * rowCount;
    Insets insets = AwtUtil.uniteInsetsFromTo(myList.getComponent(), null);
    listHeight += AwtUtil.getInsetHeight(insets);
    Dimension size = new Dimension(textSize.width, listHeight);
    return new Rectangle(location, size);
  }

  public int getMaxListHeight() {
    int listHeight = myList.getRowHeight() * myRowCount;
    Insets insets = AwtUtil.uniteInsetsFromTo(myList.getComponent(), null);
    return listHeight + AwtUtil.getInsetHeight(insets);
  }

  /**
   * @return current selection in drop-down or single variant (iff there is only one variant for typed in text)
   */
  @Nullable
  public T getSelected() {
    return myList.getSelected();
  }

  private static class WidgetController<T> {
    private final WidgetHostUtil<AListModel<T>> myListHost;
    private final ColumnListWidget<T> myListWidget = new ColumnListWidget<T>(TABLE_POLICY, TABLE_POLICY);
    private final ListSelectionProcessor<T> myListSelection = new ListSelectionProcessor<T>();

    private WidgetController() {
      myListSelection.install(myListWidget);
      myListHost = WidgetHostUtil.create(myListWidget);
      myListHost.getComponent().setFocusable(false);
      myListHost.getComponent().addConsumeEventIds(MouseEvent.MOUSE_CLICKED, MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED);
    }

    public ListSelectionProcessor<T> getListSelection() {
      return myListSelection;
    }

    public ColumnListWidget<T> getWidget() {
      return myListWidget;
    }

    public JComponent createScrollPane() {
      return myListHost.wrapWithScrollPane();
    }

    public void dispatchEvent(KeyEvent e) {
      JComponent target = myListHost.getComponent();
      e.setSource(target);
      target.dispatchEvent(e);
    }

    public void setModel(AListModel<T> variants) {
      myListHost.setValue(variants);
    }

    public int getRowHeight() {
      AListModel<T> model = myListHost.getState().getValue();
      int cellHeight;
      if (model == null) {
        cellHeight = 0;
      } else {
        cellHeight = calculateRowHeight(model);
      }
      if (cellHeight <= 0) {
        WidgetHostComponent component = myListHost.getComponent();
        cellHeight = component.getFontMetrics(component.getFont()).getHeight();
      }
      return cellHeight;
    }

    private int calculateRowHeight(AListModel<T> model) {
      HostComponentState<AListModel<T>> state = myListHost.getState();
      HostCell rootCell = state.getRootCell();
      HostComponentState<AListModel<T>> tempState = null;
      if (rootCell == null) {
        tempState = myListHost.getComponent().createState();
        tempState.setWidget(state.getWidget());
        tempState.setValue(state.getValue());
        tempState.activate();
        rootCell = tempState.getRootCell();
      }
      try {
        HostCell listCell = findListCell(rootCell);
        int[] width = ColumnListWidget.copyCurrentColumnLayout(listCell);
        T sampleValue = model.getSize() > 0 ? model.getAt(0) : null;
        return width != null ? TABLE_POLICY.getPreferedCellHeight(listCell, myListWidget, 0, sampleValue, width, model.getSize()) : 0;
      } finally {
        if (tempState != null) tempState.deactivate();
      }
    }

    public JComponent getComponent() {
      return myListHost.getComponent();
    }

    private HostCell findListCell(HostCell rootCell) {
      if (rootCell == null) return null;
      if (rootCell.getWidget() == myListWidget) return rootCell;
      return WidgetUtil.findDescendant(rootCell, myListWidget);
    }

    public T getSelected() {
      AListModel<? extends T> variants = myListHost.getState().getValue();
      if (variants == null) return null;
      HostCell listCell = findListCell(myListHost.getState().getRootCell());
      if (listCell != null) {
        int row = myListSelection.getSelection(listCell);
        if (row >= 0 && row < variants.getSize()) return variants.getAt(row);
      }
      T candidate = null;
      for (T variant : variants) {
        if (myListSelection.canSelect(variant)) {
          if (candidate == null) candidate = variant;
          else return null; // not unique variant
        }
      }
      return candidate;
    }
  }
}
