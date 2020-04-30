package com.almworks.timetrack.gui;

import com.almworks.api.application.DBDataRoles;
import com.almworks.api.engine.Engine;
import com.almworks.api.gui.DialogResult;
import com.almworks.timetrack.api.TimeTrackerSettings;
import com.almworks.util.ui.actions.*;

public class TimeTrackingOptionsAction extends SimpleAction {
  private final TimeTrackerSettings mySettings;

  public TimeTrackingOptionsAction(TimeTrackerSettings settings) {
    super("&Time Tracking Options\u2026");
    mySettings = settings;
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.updateOnChange(context.getSourceObject(Engine.ROLE).getConnectionManager().getConnectionsModifiable());
    context.setEnabled(
      DBDataRoles.isAnyConnectionAllowsUpload(context) ? EnableState.ENABLED :
        EnableState.INVISIBLE);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    TimeTrackingOptionsForm form = new TimeTrackingOptionsForm();
    form.loadFrom(mySettings);

    DialogResult<Boolean> result = DialogResult.create(context, "timeTrackingOptions");
    result.setOkResult(true);
    result.setCancelResult(false);
    result.setInitialFocusOwner(form.getInitialFocusOwner());
    Boolean r = result.showModal("Time Tracking Options", form.getComponent());
    if (r != null && r) {
      form.saveTo(mySettings);
    }
  }
}
