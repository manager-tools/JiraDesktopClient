package com.almworks.util.ui.actions;

import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.BottleneckJobs;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 * @author dyoma
 */
public class ComponentUpdateController implements UpdateService {
  private final JComponent myContextComponent;
  private final NextUpdateManager myNextUpdate;
  private final Lifecycle myNotifyCycle = new Lifecycle(false);
  private final FireEventSupport<UpdateRequestable> myUpdatables = FireEventSupport.create(UpdateRequestable.class);
  private final ContextWatcher myWatcher = new ContextWatcher();
  private boolean myUpToDate = true;
  private boolean myContextAttached = false;

  public ComponentUpdateController(JComponent contextComponent) {
    myContextComponent = contextComponent;
    myNextUpdate = new NextUpdateManager(this);
  }

  public void attachContext(Lifespan life) {
    assert !myContextAttached;
    if (life.isEnded())
      return;
    myUpToDate = false;
    final HierarchyListener listener = new HierarchyListener() {
      public void hierarchyChanged(HierarchyEvent e) {
        if (myContextComponent.isDisplayable() && !myNotifyCycle.isCycleStarted())
          onAddNotify();
        else if (!myContextComponent.isDisplayable() && myNotifyCycle.isCycleStarted())
          onRemoveNotify();
      }
    };
    myContextComponent.addHierarchyListener(listener);
    life.add(myNotifyCycle.getAnyCycleDetach());
    life.add(new Detach() {
      protected void doDetach() throws Exception {
        myContextComponent.removeHierarchyListener(listener);
        myNotifyCycle.cycleEnd();
      }
    });
  }

  private void onAddNotify() {
    myNotifyCycle.cycleStart();
    myNextUpdate.start(myNotifyCycle.lifespan());
    myUpToDate = false;
    ensureUpToDate();
  }

  private void onRemoveNotify() {
    myUpToDate = false;
    ensureUpToDate();
    myNotifyCycle.cycleEnd();
  }

  public ContextWatcher getWatcher() {
    return myWatcher;
  }

  public void requestUpdate() {
    myUpToDate = false;
    ourToUpdate.addJob(this);
  }

  private static final BottleneckJobs<ComponentUpdateController> ourToUpdate = new BottleneckJobs<ComponentUpdateController>(50, ThreadGate.AWT_QUEUED) {
    protected void execute(ComponentUpdateController job) {
      job.ensureUpToDate();
    }
  };

  private void ensureUpToDate() {
    if (myUpToDate || !myNotifyCycle.isCycleStarted())
      return;
    UpdateRequest request = myNextUpdate.prepareUpdateRequest(myContextComponent);
    myWatcher.requestUpdates(request);
    myUpdatables.getDispatcher().update(request);
    myUpToDate = true;
    myNextUpdate.updateComplete();
  }

  public void addUpdatable(Lifespan life, UpdateRequestable updatable) {
    myUpdatables.addStraightListener(updatable);
  }

  public static void connectUpdatable(Lifespan lifespan, UpdateRequestable updatable, JComponent component) {
    ComponentUpdateController controller = new ComponentUpdateController(component);
    controller.addUpdatable(lifespan, updatable);
    controller.attachContext(lifespan);
  }
}
