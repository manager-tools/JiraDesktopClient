package com.almworks.platform;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.exec.ExceptionsManager;
import com.almworks.util.LogHelper;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.picocontainer.Startable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class ApplicationLoadStatusImpl implements ApplicationLoadStatus, Startable {
  private final ExceptionsManager myExceptionsManager;

  private final BasicScalarModel<Boolean> myReadyModel = BasicScalarModel.createWithValue(false, true);
  private final List<MyActivity> myActivities = Collections15.arrayList();
  private boolean myStartupAllowed = false;

  public ApplicationLoadStatusImpl(ExceptionsManager exceptionsManager) {
    myExceptionsManager = exceptionsManager;
  }

  public ScalarModel<Boolean> getApplicationLoadedModel() {
    return myReadyModel;
  }

  @Override
  public StartupActivity createActivity(String debugName) {
    MyActivity activity = new MyActivity(debugName);
    LogHelper.debug("Starting:", activity);
    synchronized (myActivities) {
      myActivities.add(activity);
    }
    if (myReadyModel.getValue()) {
      LogHelper.error("Already started", debugName);
      activity.markDone();
      synchronized (myActivities) {
        myActivities.remove(activity);
      }
      return activity;
    }
    return activity;
  }

  private void onDone(MyActivity activity) {
    if (myReadyModel.getValue()) {
      LogHelper.error("Already started", activity);
      return;
    }
    boolean remove;
    boolean startup;
    synchronized (myActivities) {
      remove = myActivities.remove(activity);
      startup = myActivities.isEmpty() && myStartupAllowed;
    }
    LogHelper.debug("Startup activity finished", activity);
    LogHelper.assertError(remove, "Unknown activity", activity);
    if (startup) {
      LogHelper.debug("Normal startup. Last activity just finished");
      finishNow();
    }
  }

  public void start() {
    if (myExceptionsManager.isAnyExceptionOccured()) {
      LogHelper.warning("Forced start due to startup exceptions");
      finishNow();
    } else {
      final DetachComposite detach = new DetachComposite(true);
      detach.add(myExceptionsManager.addListener(new ExceptionsManager.Listener() {
        public void onException(ExceptionsManager.ExceptionEvent event) {
          detach.detach();
          LogHelper.warning("Forced start due to exception just occurred");
          finishNow();
        }
      }));
      detach.add(myReadyModel.getEventSource().addStraightListener(new ScalarModel.Adapter<Boolean>() {
        public void onScalarChanged(ScalarModelEvent<Boolean> event) {
          Boolean started = event.getNewValue();
          if (started != null && started) {
            detach.detach();
          }
        }
      }));
    }
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        javax.swing.Timer timer = new Timer(60000, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (finishNow()) Log.warn("application status set to 'loaded' by timeout");
          }
        });
        timer.setRepeats(false);
        timer.start();
      }
    });
  }

  public void stop() {
  }

  private boolean finishNow() {
    if (!myReadyModel.commitValue(false, true)) return false;
    clearActivities();
    return true;
  }

  private void clearActivities() {
    MyActivity[] activities;
    synchronized (myActivities) {
      activities = myActivities.toArray(new MyActivity[myActivities.size()]);
      myActivities.clear();
    }
    for (MyActivity act : activities) act.markDone();
  }

  public void allowStartup() {
    if (myReadyModel.getValue()) {
      LogHelper.warning("Already started");
      return;
    }
    boolean empty;
    synchronized (myActivities) {
      myStartupAllowed = true;
      empty = myActivities.isEmpty();
    }
    if (empty) {
      LogHelper.debug("Normal startup. No startup activity is running.");
      finishNow();
    }
  }

  private class MyActivity extends StartupActivity {
    private final AtomicBoolean myDone = new AtomicBoolean(false);
    private final DetachComposite myLife = new DetachComposite();

    private MyActivity(String debugName) {
      super(debugName);
    }

    public void markDone() {
      myDone.set(true);
      myLife.detach();
    }

    @Override
    public Lifespan getLife() {
      return myLife;
    }

    @Override
    public StartupActivity createSubActivity(String debugName) {
      return createActivity(getDebugName() + "/" + debugName);
    }

    @Override
    public void done() {
      if (myDone.compareAndSet(false, true)) {
        onDone(this);
        myLife.detach();
      }
    }

    @Override
    public String toString() {
      return super.toString() + (myDone.get() ? " (DONE)" : " (RUNNING)");
    }
  }
}
