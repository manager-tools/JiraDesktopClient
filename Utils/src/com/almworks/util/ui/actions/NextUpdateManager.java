package com.almworks.util.ui.actions;

import com.almworks.util.threads.Threads;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author dyoma
*/
public class NextUpdateManager implements Updatable {
  private final UpdateService myService;
  private boolean myUpdating = false;
  private Lifecycle myLifecycle = null;

  public NextUpdateManager(UpdateService service) {
    myService = service;
  }

  public void requestUpdate() {
    Threads.assertAWTThread();
    if (myUpdating)
      return;
    myService.requestUpdate();
    Lifecycle lifecycle = myLifecycle;
    if (lifecycle != null)
      lifecycle.cycleEnd();
  }

  @NotNull
  public Lifespan getLifespan() {
    assert isStarted();
    return myLifecycle != null ? myLifecycle.lifespan() : Lifespan.NEVER;
  }

  public void start(Lifespan life) {
    assert !isStarted();
    myLifecycle = new Lifecycle();
    life.add(myLifecycle.getDisposeDetach());
  }

  public UpdateContext prepareUpdateContext(JComponent contextComponent) {
    assert isStarted();
    assert!myUpdating;
    myUpdating = true;
    myLifecycle.cycle();
    return new DefaultUpdateContext(contextComponent, this);
  }

  public UpdateRequest prepareUpdateRequest(JComponent contextComponent) {
    assert isStarted();
    Threads.assertAWTThread();
    assert!myUpdating;
    myUpdating = true;
    myLifecycle.cycle();
    return new UpdateRequest(this, new DefaultActionContext(contextComponent));
  }

  public boolean isStarted() {
    return myLifecycle != null && !myLifecycle.isDisposed();
  }

  public String toString() {
    return "NUM:" + myService;
  }

  public void updateComplete() {
    assert myUpdating;
    Threads.assertAWTThread();
    myUpdating = false;
  }
}
