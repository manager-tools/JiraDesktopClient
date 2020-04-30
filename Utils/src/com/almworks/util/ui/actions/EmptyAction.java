package com.almworks.util.ui.actions;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EmptyAction extends EnabledAction {
  public EmptyAction() {
  }

  public EmptyAction(@Nullable String name) {
    super(name);
  }

  public EmptyAction(@Nullable String name, @Nullable Icon icon) {
    super(name, icon);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
  }
}
