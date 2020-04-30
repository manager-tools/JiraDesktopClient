package com.almworks.util.ui.actions;

import com.almworks.util.collections.SimpleModifiable;
import org.almworks.util.Log;

public class AnActionProxy implements AnAction {
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private AnAction myDelegate;

  public AnActionProxy(AnAction delegate) {
    myDelegate = delegate;
  }

  public AnActionProxy() {
  }

  public void update(UpdateContext context) throws CantPerformException {
    context.updateOnChange(myModifiable);
    AnAction delegate = myDelegate;
    if (delegate != null) {
      delegate.update(context);
    } else {
      context.setEnabled(EnableState.INVISIBLE);
    }
  }

  public void perform(ActionContext context) throws CantPerformException {
    AnAction delegate = myDelegate;
    if (delegate != null) {
      delegate.perform(context);
    } else {
      Log.warn("cannot perform " + this);
    }
  }

  public void setDelegate(AnAction action) {
    myDelegate = action;
    myModifiable.fireChanged();
  }

  public void clearDelegate() {
    setDelegate(null);
  }
}
