package com.almworks.util.ui.actions;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class EnabledAction extends SimpleAction {
  protected EnabledAction() {
  }

  protected EnabledAction(@Nullable String name) {
    super(name);
  }

  protected EnabledAction(@Nullable String name, @Nullable Icon icon) {
    super(name, icon);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(EnableState.ENABLED);
  }
}
