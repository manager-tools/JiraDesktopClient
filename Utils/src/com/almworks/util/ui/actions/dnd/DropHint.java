package com.almworks.util.ui.actions.dnd;

public abstract class DropHint {
  protected boolean myValid = true;

  public boolean isValid() {
    return myValid;
  }

  public void setValid(boolean valid) {
    myValid = valid;
  }
}
