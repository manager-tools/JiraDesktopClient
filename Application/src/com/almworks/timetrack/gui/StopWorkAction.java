package com.almworks.timetrack.gui;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.gui.MainMenu;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.util.Terms;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

public class StopWorkAction extends SimpleAction {
  public StopWorkAction() {
    super("Stop Wor&k", Icons.STOP_WORK_ACTION);
    setDefaultText(PresentationKey.SHORT_DESCRIPTION,
      "Stop tracking time spent on the selected " + Terms.ref_artifact);
    watchRole(TimeTracker.TIME_TRACKER);
    watchRole(ItemWrapper.ITEM_WRAPPER);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    final ItemWrapper item = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    final TimeTracker tt = context.getSourceObject(TimeTracker.TIME_TRACKER);
    context.updateOnChange(tt.getModifiable());

    if(tt.isCurrentTaskForItem(item.getItem())) {
      context.setEnabled(EnableState.ENABLED);
    } else {
      final TimeTrackingCustomizer ttc = context.getSourceObject(TimeTrackingCustomizer.ROLE);
      final boolean perm = ttc.isTimeTrackingPermissionGranted(item);
      context.setEnabled(perm ? EnableState.DISABLED : EnableState.INVISIBLE);
    }
    
    IdActionProxy.setShortcut(context, MainMenu.Tools.TIME_TRACKING_STOP_WORK_ON_ISSUE);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    final ItemWrapper item = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    final TimeTracker tt = context.getSourceObject(TimeTracker.TIME_TRACKER);
    final TimeTrackerTask task = new TimeTrackerTask(item.getItem());
    if (!task.equals(tt.getCurrentTask())) {
      return;
    }
    
    tt.setTrackingAndCurrentTask(false, null);
  }
}
