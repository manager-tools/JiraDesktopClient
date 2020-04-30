package com.almworks.sysprop;

import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;

public class SystemPropertiesAction extends SimpleAction {
  public SystemPropertiesAction() {
    super("System &Properties");
    updateOnChange(SystemPropertiesDialog.SHOWING);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(!SystemPropertiesDialog.SHOWING.getValue());
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    SystemPropertiesDialog.showDialog(context);
  }
}
