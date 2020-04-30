package com.almworks.util.components.completion;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.ColumnComboBox;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.components.renderer.ListCanvasWrapper;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class CompletingComboBox<T> extends ColumnComboBox {
  private final ListCanvasWrapper<T> myCanvasWrapper = new ListCanvasWrapper<T>(false) {
    @Override
    protected Component prepareRendererComponent(JList list, Object value, int index, boolean isSelected,
      boolean cellHasFocus, boolean useBgRenderer)
    {
      final Component c = super.prepareRendererComponent(list, value, index, isSelected, cellHasFocus, useBgRenderer);
      getRenderer().transferCanvasDecorationsToBackground();
      return c;
    }
  };
  private CompletingComboBoxController<T> myController;

  public CompletingComboBox() {
    myCanvasWrapper.setLafRenderer(getRenderer());
    setRenderer(myCanvasWrapper);
    myController = new CompletingComboBoxController<T>(this);
  }

  CompletingComboBox(Object o) {
    assert o == null;
    myCanvasWrapper.setLafRenderer(getRenderer());
    setRenderer(myCanvasWrapper);
  }

  CompletingComboBox(CompletingComboBoxController<T> controller) {
    myController = controller;
  }

  public void updateUI() {
    if (myCanvasWrapper == null) {
      super.updateUI();
      return;
    }
    ListCellRenderer cellRenderer = getRenderer();
    boolean temporaryRemove = cellRenderer == myCanvasWrapper;
    if (temporaryRemove)
      setRenderer(null);
    super.updateUI();
    if (temporaryRemove) {
      myCanvasWrapper.setLafRenderer(getRenderer());
      setRenderer(myCanvasWrapper);
    }
  }

  public void setCanvasRenderer(CanvasRenderer<? super T> canvasRenderer) {
    myCanvasWrapper.setCanvasRenderer(canvasRenderer);
    setRenderer(myCanvasWrapper);
  }

  public CompletingComboBoxController<T> getController() {
    return myController;
  }

  public AComboboxModel<T> getAModel() {
    return myController.getModel();
  }

  public void setCasesensitive(boolean casesensitive) {
    if (myController != null)
      myController.setCasesensitive(casesensitive);
  }

  public boolean isCasesensitive() {
    return myController == null || myController.isCasesensitive();
  }

  void setController(CompletingComboBoxController<T> controller) {
    assert myController == null || myController == controller;
    assert controller != null;
    myController = controller;
  }

  public RecentController<T> getRecents() {
    return myController.getRecents();
  }
}
