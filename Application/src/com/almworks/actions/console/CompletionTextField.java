package com.almworks.actions.console;

import javax.swing.*;

public class CompletionTextField<T> extends JTextField {
  private final CompletionFieldController<T> myController;
  private boolean myDisplayable = false;

  public CompletionTextField() {
    myController = new CompletionFieldController<T>(this);
  }

  public CompletionFieldController<T> getController() {
    return myController;
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (!myDisplayable) {
      myDisplayable = true;
      myController.onAddNotify();
    }
  }

  @Override
  public void removeNotify() {
    if (myDisplayable) {
      myDisplayable = false;
      myController.onRemoveNotify();
    }
    super.removeNotify();
  }
}
