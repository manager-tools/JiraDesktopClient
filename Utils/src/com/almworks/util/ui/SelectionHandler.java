package com.almworks.util.ui;

import com.almworks.util.components.SelectionAccessor;

/**
 * @author : Dyoma
 */
public abstract class SelectionHandler<T> {
  private final SelectionAccessor<? extends T> myElementSelection;
  private T myCurrentElement = null;

  protected SelectionHandler(SelectionAccessor<? extends T> elementSelection) {
    myElementSelection = elementSelection;

    myElementSelection.addListener(new SelectionAccessor.Listener() {
      public void onSelectionChanged(Object newSelection) {
        ensureSelectionShown();
      }
    });
    ensureSelectionShown();
  }

  protected void ensureSelectionShown() {
    T selection = myElementSelection.getSelection();
    if (myCurrentElement == selection)
      return;
    myCurrentElement = selection;
    onSelectionChanged(selection);
  }

  protected abstract void onSelectionChanged(T element);
}
