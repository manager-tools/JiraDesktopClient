package com.almworks.api.tray;

import com.almworks.util.Env;
import com.almworks.util.Terms;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.actions.*;

public class UseSystemTrayAction extends SimpleAction {
  private static final String NAME = Env.isMac()
    ? Local.parse("Show " + Terms.ref_Deskzilla + " in Menu Bar") 
    : "&Use System Tray";

  private final SystemTrayApplicationView myView;

  public UseSystemTrayAction(SystemTrayApplicationView view) {
    super(NAME);
    myView = view;
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.updateOnChange(myView.getModifiable());
    context.putPresentationProperty(PresentationKey.TOGGLED_ON, myView.isActive());
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    myView.setActive(!myView.isActive());
  }
}
