package com.almworks.util.components;

import com.almworks.util.Env;
import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ComboBoxModelHolder;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class RadioButtonGroup<T> implements ComponentsListController.ListAccessor<T>, SelectionListener {
  private final ComboBoxModelHolder<T> myModel = new ComboBoxModelHolder<T>();
  private AbstractButton myLastSelection = null;
  private final boolean[] myUpdateFlag = new boolean[] {false};
  private final ButtonGroup myGroup = new ButtonGroup();
  private final ComponentsListController<T> myController;

  public RadioButtonGroup() {
    myController = new ComponentsListController<T>(this);
    myModel.addSelectionListener(Lifespan.FOREVER, this);
  }

  public Detach setModel(AComboboxModel<T> model) {
    return myModel.setModel(model);
  }

  public AComboboxModel<T> getModel() {
    return myModel;
  }

  public void onSelectionChanged() {
    if (myUpdateFlag[0])
      return;
    myUpdateFlag[0] = true;
    try {
      AbstractButton button = myController.findButton(myModel.getSelectedItem());
      if (button == myLastSelection)
        return;
      setSelected(myLastSelection, false);
      myLastSelection = button;
      setSelected(button, true);
    } finally {
      myUpdateFlag[0] = false;
    }
  }

  public void onInsert(int index, int length) {
    myController.insertComponents(index, length);
  }

  public void onRemove(int index, int length, AListModel.RemovedEvent event) {
    myController.removeComponents(event);
  }

  public void onListRearranged(AListModel.AListEvent event) {
    myController.rearrangeComponents(event);
  }

  public void onItemsUpdated(AListModel.UpdateEvent event) {
    myController.updateComponents(event);
  }

  private void setSelected(@Nullable AbstractButton button, boolean select) {
    if (button != null)
      button.setSelected(select);
  }

  public static <T> RadioButtonGroup<T> create() {
    return new RadioButtonGroup<T>();
  }

  public AbstractButton doCreateButton(final T item) {
    final JRadioButton button = new JRadioButton();
    if (Env.isWindows()) {
      button.setMargin(new Insets(0, 0, 0, 0));
    }
    myGroup.add(button);
    if (Util.equals(myModel.getSelectedItem(), item))
      button.setSelected(true);
    UIUtil.addSelectedListener(button, new JointChangeListener(myUpdateFlag) {
      protected void processChange() {
        if (button.isSelected())
          myModel.setSelectedItem(item);
      }
    });
    return button;
  }

  public void onButtonRemoved(AbstractButton button) {
    myGroup.remove(button);
  }

  public T getItemAt(int index) {
    return myModel.getAt(index);
  }

  public void onButtonUpdated(AbstractButton button, T t) {}

  public void setRenderer(CanvasRenderer<T> renderer) {
    myController.setRenderer(renderer);
  }

  public JComponent getPanel() {
    return myController.getPanel();
  }

  public void setOpaque(boolean opaque) {
    myController.setOpaque(opaque);
  }
}
