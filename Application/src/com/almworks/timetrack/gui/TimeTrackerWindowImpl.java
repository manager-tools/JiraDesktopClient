package com.almworks.timetrack.gui;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.WindowController;
import com.almworks.items.api.Database;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerWindow;
import com.almworks.util.Env;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.picocontainer.Startable;

import javax.swing.*;
import java.awt.*;

public class TimeTrackerWindowImpl implements TimeTrackerWindow, Startable, ChangeListener {
  private static final String AUTO_SHOW_KEY = "autoShow";
  private static final String BIG_TIME_KEY = "bigTimeId";
  private static final String HINT_SHOWN_KEY = "hintShown";

  private final Configuration myConfiguration;
  private final Database myDb;
  private final TimeTracker myTimeTracker;
  private final DialogManager myDialogManager;
  private final ApplicationLoadStatus myLoadStatus;
  private final SimpleModifiable myShowingModifiable = new SimpleModifiable();

  private DialogBuilder myDialog;
  private boolean myShowing;
  private boolean myHintNeeded;

  public TimeTrackerWindowImpl(Configuration configuration, Database db, TimeTracker timeTracker, DialogManager dialogManager,
    ApplicationLoadStatus status)
  {
    myConfiguration = configuration;
    myDb = db;
    myTimeTracker = timeTracker;
    myDialogManager = dialogManager;
    myLoadStatus = status;
    myTimeTracker.getModifiable().addAWTChangeListener(Lifespan.FOREVER, this);
  }

  private void dispose() {
    DialogBuilder db = myDialog;
    if (db != null) {
      try {
        db.closeWindow();
      } catch (CantPerformException e) {
        // ignore
      }
    }
  }

  public void start() {
    if (!myConfiguration.getBooleanSetting(AUTO_SHOW_KEY, false))
      return;
    myLoadStatus.getApplicationLoadedModel().getEventSource().addAWTListener(new ScalarModel.Adapter<Boolean>() {
      public void onScalarChanged(ScalarModelEvent<Boolean> e) {
        if (e.getNewValue() != null && e.getNewValue()) {
          autoShow();
        }
      }
    });
  }

  private void autoShow() {
    if(!Env.isMac()) {
      show();
    } else {
      // JCO-342: A small wait before showing the Time Tracker upon
      // application launch on Mac OS X. Without it, the main menu
      // would come up with all the items disabled.
      ThreadGate.LONG.execute(
        new Runnable() {
          public void run() {
            try {
              Thread.sleep(50);
            } catch (InterruptedException ignored) {}
            ThreadGate.AWT.execute(
              new Runnable() {
                public void run() {
                  show();
                }
              }
            );
          }
        }
      );
    }
  }

  public void stop() {
    dispose();
  }

  public SimpleModifiable getShowingModifiable() {
    return myShowingModifiable;
  }

  public boolean isWindowShowing() {
    return myShowing;
  }

  public void show() {
    Threads.assertAWTThread();
    DialogBuilder db = myDialog;
    if (db == null) {
      myConfiguration.setSetting(AUTO_SHOW_KEY, true);
      final DialogBuilder newdb = create();
      myShowing = true;
      myShowingModifiable.fireChanged();
      newdb.showWindow(new Detach() {
        @Override
        protected void doDetach() throws Exception {
          myConfiguration.setSetting(AUTO_SHOW_KEY, false);
          if (myDialog == newdb) {
            myDialog = null;
            myShowing = false;
            myShowingModifiable.fireChanged();
          }
        }
      });
      myDialog = newdb;
      final WindowController wc = newdb.getWindowContainer().getActor(WindowController.ROLE);
      if (wc != null) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            wc.activate();
            showHintPopup();
          }
        });
      }
    } else {
      WindowController wc = db.getWindowContainer().getActor(WindowController.ROLE);
      if (wc != null) {
        wc.activate();
        showHintPopup();
      }
    }
  }

  public boolean shouldPreferTimeTrackingWindowForTray() {
    return myTimeTracker.getCurrentTask() != null;
  }

  private DialogBuilder create() {
    DialogBuilder dialog = myDialogManager.createBuilder("timeTracking");
    dialog.setModal(false);
    dialog.setAlwaysOnTop(myTimeTracker.isWindowAlwaysOnTop());
    TimeTrackerForm form = new TimeTrackerForm();
    form.attach(myDb, myTimeTracker);
    dialog.setContent(form);
    dialog.setTitle("Time Tracker");
    dialog.setBottomBevel(false);
    dialog.setNullOwner(true);
    dialog.setInitialFocusOwner(form.getInitialFocusOwner());
    dialog.setBorders(false);
    dialog.setBottomLineShown(false);
    dialog.setPreferredSize(new Dimension(200, 400));
    return dialog;
  }

  public String getBigTimeId() {
    return myConfiguration.getSetting(BIG_TIME_KEY, null);
  }

  public void setBigTimeId(String id) {
    myConfiguration.setSetting(BIG_TIME_KEY, id);
  }

  public void onChange() {
    if(myTimeTracker.isTracking() && !myConfiguration.getBooleanSetting(HINT_SHOWN_KEY, false)) {
      myHintNeeded = true;
      showHintPopup();
    }
  }

  private void showHintPopup() {
    if(myHintNeeded && myShowing) {
      if(myDialog == null) {
        assert false : "no dialog";
        return;
      }

      final WindowController wc = myDialog.getWindowContainer().getActor(WindowController.ROLE);
      if(wc == null) {
        assert false : "no controller";
        return;
      }

      final String p2;
      if(TimeTrackerUIConsts.AFFORDANCE_RIGHT.length() > 1) {
        p2 = "<p>Click value marked with a triangle (" +
          TimeTrackerUIConsts.AFFORDANCE_RIGHT.substring(1) + ") to adjust it.";
      } else {
        p2 = "<p>You can also click other values to adjust them.";
      }

      final String notice =
        "<html><body><p>The text at the top shows the time that you have spent working on this task. " +
          "Click the text to view or change the value it displays." + p2;

      UIUtil.showNoticePopup(notice, wc.getWindow());

      myHintNeeded = false;
      myConfiguration.setSetting(HINT_SHOWN_KEY, true);
    }
  }
}
