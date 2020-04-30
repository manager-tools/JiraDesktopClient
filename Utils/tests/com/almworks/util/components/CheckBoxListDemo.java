package com.almworks.util.components;

import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class CheckBoxListDemo {
  public static void main(String[] args) {
    LAFUtil.initializeLookAndFeel();
    JPanel panel = new JPanel(new BorderLayout(5, 5));
    final OrderListModel<String> model = OrderListModel.create("1", "2", "3", "4");
    ACheckboxList list = new ACheckboxList(model);
    ListSpeedSearch.install(list);
    panel.add(list, BorderLayout.CENTER);
    panel.add(createActionButtons(list.getSelectionAccessor(), model, list.getCheckedAccessor()), BorderLayout.EAST);
    DebugFrame.show(panel).setTitle("ACheckBoxList");
    DebugFrame.show(new JList(new Object[] {"1", "2", "3"})).setTitle("JList");
    DebugFrame.show(new AList(FixedListModel.create("A", "B", "C"))).setTitle("AList");
  }

  private static JComponent createActionButtons(final SelectionAccessor selectionAccessor, final OrderListModel<String> model,
    final SelectionAccessor checkedAccessor) {
    ToolbarBuilder builder = ToolbarBuilder.buttonsWithText();
    builder.addAction(MoveAction.up(selectionAccessor, model));
    builder.addAction(MoveAction.down(selectionAccessor, model));
    builder.addAction(new SimpleAction("Remove") {
      {
        setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.DELETE);
        updateOnChange(selectionAccessor);
      }

      protected void customUpdate(UpdateContext context) throws CantPerformException {
        if (!selectionAccessor.hasSelection())
          throw new CantPerformException();
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        model.removeAll(selectionAccessor.getSelectedIndexes());
      }
    });
    builder.addAction(new SimpleAction("Add first") {
      private int myCounter = 0;
      {
        setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.ALT_INSERT);
      }
      protected void customUpdate(UpdateContext context) throws CantPerformException {
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        model.insert(0, "Item #" + myCounter);
        myCounter++;
      }
    });
    builder.addAction(new SimpleAction("Toggle") {
      {
        updateOnChange(selectionAccessor);
      }
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        if (!selectionAccessor.hasSelection())
          throw new CantPerformException();
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        for (int i : selectionAccessor.getSelectedIndexes()) {
          boolean selected = checkedAccessor.isSelectedAt(i);
          if (selected)
            checkedAccessor.removeSelectionAt(i);
          else
            checkedAccessor.addSelectionIndex(i);
        }
      }
    });
    return builder.createVerticalToolbar();
  }

  private static class MoveAction extends SimpleAction {
    private final SelectionAccessor mySelectionAccessor;
    private final OrderListModel<String> myModel;
    private final int myStep;

    public MoveAction(SelectionAccessor selectionAccessor, OrderListModel<String> model, int step, String name,
      KeyStroke shortcut) {
      super(name);
      mySelectionAccessor = selectionAccessor;
      myModel = model;
      myStep = step;
      updateOnChange(selectionAccessor);
      setDefaultPresentation(PresentationKey.SHORTCUT, shortcut);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      if (!mySelectionAccessor.hasSelection())
        throw new CantPerformException();
      int index = mySelectionAccessor.getSelectedIndex();
      int newIndex = index + myStep;
      if (newIndex < 0 || newIndex >= myModel.getSize())
        throw new CantPerformException();
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      int index = mySelectionAccessor.getSelectedIndex();
      int newIndex = index + myStep;
      myModel.swap(index, newIndex);
    }

    public static AnAction up(SelectionAccessor selectionAccessor, OrderListModel<String> model) {
      return new MoveAction(selectionAccessor, model, -1, "Up", createShortcut(KeyEvent.VK_UP));
    }

    public static AnAction down(SelectionAccessor selectionAccessor, OrderListModel<String> model) {
      return new MoveAction(selectionAccessor, model, 1, "Down", createShortcut(KeyEvent.VK_DOWN));
    }

    private static KeyStroke createShortcut(int key) {
      return KeyStroke.getKeyStroke(key, KeyEvent.ALT_MASK | KeyEvent.CTRL_MASK);
    }
  }
}
