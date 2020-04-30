package com.almworks.timetrack.gui;

import com.almworks.api.application.DBDataRoles;
import com.almworks.api.engine.Engine;
import com.almworks.timetrack.api.TimeTrackerWindow;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

public class OpenTimeTrackerWindowAction extends SimpleAction {
  public OpenTimeTrackerWindowAction() {
    super("&Open Time Tracker", Icons.ACTION_TIME_TRACKING);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Open time tracking window");
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.updateOnChange(context.getSourceObject(Engine.ROLE).getConnectionManager().getConnectionsModifiable());
    context.setEnabled(
      DBDataRoles.isAnyConnectionAllowsUpload(context) ? EnableState.ENABLED :
        EnableState.INVISIBLE);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    context.getSourceObject(TimeTrackerWindow.ROLE).show();
  }
}
