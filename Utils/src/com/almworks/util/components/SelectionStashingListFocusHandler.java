package com.almworks.util.components;

import com.almworks.util.components.speedsearch.ListSpeedSearch;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

public class SelectionStashingListFocusHandler<T> implements FocusListener {
  private final SelectionAccessor<T> myAccessor;

  private List<T> myStashedSelection;

  public static <T> SelectionStashingListFocusHandler<T> speedSearchAware(SelectionAccessor<T> accessor,
    final ListSpeedSearch<T> controller)
  {
    return new SelectionStashingListFocusHandler<T>(accessor) {
      @Override
      protected boolean shouldHandle(FocusEvent e) {
        return !controller.ownsComponent(e.getOppositeComponent());
      }
    };
  }

  public SelectionStashingListFocusHandler(SelectionAccessor<T> accessor) {
    myAccessor = accessor;
  }

  @Override
  public void focusGained(FocusEvent e) {
    if(shouldHandle(e) && !myAccessor.hasSelection()) {
      if(myStashedSelection != null) {
        myAccessor.setSelected(myStashedSelection);
      }
      if(myAccessor.ensureSelectionExists()) {
        scrollSelectionToView();
      }
    }
  }

  protected boolean shouldHandle(FocusEvent e) {
    return true;
  }

  protected void scrollSelectionToView() {}

  @Override
  public void focusLost(FocusEvent e) {
    if(shouldHandle(e)) {
      myStashedSelection = myAccessor.getSelectedItems();
      myAccessor.clearSelection();
    }
  }
}
