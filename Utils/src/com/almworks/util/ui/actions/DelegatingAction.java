package com.almworks.util.ui.actions;

import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
public abstract class DelegatingAction implements AnAction {
  public void update(UpdateContext context) throws CantPerformException {
    AnAction delegate = getDelegate(context);
    if (delegate == null)
      throw new CantPerformException();
    delegate.update(context);
  }

  public void perform(ActionContext context) throws CantPerformException {
    AnAction delegate = getDelegate(context);
    if (delegate == null)
      throw new CantPerformException();
    delegate.perform(context);
  }

  @Nullable
  protected abstract AnAction getDelegate(ActionContext context) throws CantPerformException;
}
