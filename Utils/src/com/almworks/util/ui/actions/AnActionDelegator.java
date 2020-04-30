package com.almworks.util.ui.actions;

public abstract class AnActionDelegator implements AnAction {
  protected final AnAction myDelegate;

  public AnActionDelegator(AnAction delegate) {
    myDelegate = delegate;
  }

  public void update(UpdateContext context) throws CantPerformException {
    myDelegate.update(context);
  }

  public void perform(ActionContext context) throws CantPerformException {
    myDelegate.perform(context);
  }
}
