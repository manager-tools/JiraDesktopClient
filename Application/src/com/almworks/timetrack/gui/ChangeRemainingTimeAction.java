package com.almworks.timetrack.gui;

import com.almworks.items.api.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.timetrack.impl.TaskRemainingTime;
import com.almworks.timetrack.impl.TimeTrackingUtil;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * The action to change the remaining time estimate in the Time Tracker.
 */
public class ChangeRemainingTimeAction extends SimpleAction {
  private final TimeTrackingCustomizer myCustomizer = Context.require(TimeTrackingCustomizer.ROLE);
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private TimeTrackerTask myCurrTask;
  private volatile TaskRemainingTime myRemaining;

  ChangeRemainingTimeAction() {
    super("Remaining time:");
    watchRole(TimeTracker.TIME_TRACKER);
    TimeTrackerForm.TRACKING_TICKS.install(this);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Adjust remaining time estimate");
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
    setDefaultPresentation(PresentationKey.SHORTCUT, KeyStroke.getKeyStroke(
      KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
  }

  protected void customUpdate(final UpdateContext context) throws CantPerformException {
    final TrackerSimplifier ts = new TrackerSimplifier(context);

    context.setEnabled(ts.task != null);

    if(ts.task == null) {
      return;
    }

    context.updateOnChange(ts.tracker.getModifiable());
    listenToArtifact(context, ts);

    if(!ts.task.equals(myCurrTask)) {
      // Task changed: find out its remaining time.
      myCurrTask = ts.task;

      final TaskRemainingTime rt = ts.tracker.getRemainingTime(ts.task);
      if(rt != null) {
        // There is a remaining time record for this task.
        // We can calculate from time tracker state alone.
        myRemaining = rt;
      } else {
        // There's no remaining time record for this task.
        // We have to load it from the bug/issue itself.
        context.updateOnChange(myModifiable);
        // When done, it'll fire myModifiable to update again
        loadRemainingTime(context.getSourceObject(Database.ROLE), myCurrTask);
        context.putPresentationProperty(PresentationKey.NAME, "<html><center>Remaining time: \u2026");
        return;
      }
    } else {
      final TaskRemainingTime rt = ts.tracker.getRemainingTime(ts.task);
      if(rt != null && rt != myRemaining) {
        myRemaining = rt;
      }
    }

    final StringBuilder name = new StringBuilder("<html><center>Remaining time: ");

    final Integer remaining = getCurrentRemaining(ts);
    if(remaining == null) {
      name.append("not set");
    } else {
      name.append(DateUtil.getFriendlyDuration(remaining, false));
    }
    TimeTrackerUIConsts.setAffordanceIcon(context.getComponent());

    context.putPresentationProperty(PresentationKey.NAME, name.toString());
  }

  private void listenToArtifact(UpdateContext context, final TrackerSimplifier ts) throws CantPerformException {
    final UpdateRequest ur = context.getUpdateRequest();
    context.getSourceObject(Database.ROLE).addListener(ur.getLifespan(), new DBListener() {
      public void onDatabaseChanged(DBEvent event, DBReader reader) {
        final long key = ts.task.getKey();
        if(key != 0 && TimeTrackingUtil.eventAffects(event, key)) {
          ThreadGate.AWT.execute(new Runnable() {
            @Override
            public void run() {
              myCurrTask = null;
              ur.getUpdateService().requestUpdate();
            }
          });
        }
      }
    });
  }

  private void loadRemainingTime(Database db, final TimeTrackerTask task) {
    db.readForeground(new ReadTransaction<TaskRemainingTime>() {
      @Override
      public TaskRemainingTime transaction(DBReader reader) throws DBOperationCancelledException {
        ItemVersion item = SyncUtils.readTrunk(reader, task.getKey());
        final Integer savedRemaining = myCustomizer.getRemainingTime(item);
        if(savedRemaining != null) {
          return TaskRemainingTime.old(savedRemaining);
        }
        return null;
      }
    }).finallyDo(ThreadGate.AWT, new Procedure<TaskRemainingTime>() {
      @Override
      public void invoke(TaskRemainingTime arg) {
        myRemaining = arg;
        myModifiable.fireChanged();
      }
    });
  }

  private Integer getCurrentRemaining(TrackerSimplifier ts) {
    return TimeTrackingUtil.getRemainingTimeForTimings(ts.getTimings(), myRemaining, true);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    final TrackerSimplifier ts = new TrackerSimplifier(context);
    if(ts.task == null) {
      return;
    }

    final ChangeRemainingTimeForm form = new ChangeRemainingTimeForm(getCurrentRemaining(ts));

    final JDialog dialog = BaseAdjustmentForm.setupFormDialog(form, context.getComponent());
    if(dialog != null) {
      form.attach(new Procedure<Integer> () {
        public void invoke(Integer arg) {
          dialog.dispose();

          if(arg == null) {
            assert false;
            return;
          }

          final TaskRemainingTime newRemaining;
          if(arg == ChangeRemainingTimeForm.REVERT) {
            myCurrTask = null;
            newRemaining = null;
          } else if(arg >= 0) {
            newRemaining = TaskRemainingTime.now(arg);
          } else {
            return;
          }
          
          ts.tracker.setRemainingTime(ts.task, newRemaining);
        }
      });
      dialog.pack();
      dialog.show();
      dialog.requestFocus();
    }
  }
}
