package com.almworks.status;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Engine;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.gui.StatusBar;
import com.almworks.api.gui.StatusBarComponent;
import com.almworks.api.platform.ProductInformation;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.properties.Role;
import com.almworks.util.rt.MemStatusComponent;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;
import org.picocontainer.Startable;

class StatusComponentInitializer implements Startable {
  public static final Role<StatusComponentInitializer> ROLE = Role.role("StatusComponentInitializer");
  private final DetachComposite myDetach = new DetachComposite();
  private final MainWindowManager myWindowManager;

  private MemStatusComponent myStatusPanel;
  private final ComponentContainer myContainer;
  private final ProductInformation myInfo;
  private final Engine myEngine;

  public StatusComponentInitializer(MainWindowManager windowManager, ComponentContainer container,
    ProductInformation info, Engine engine)
  {
    myEngine = engine;
    assert windowManager != null;
    myInfo = info;
    myWindowManager = windowManager;
    myContainer = container.createSubcontainer("status");
  }

  public void start() {
    Log.debug("assuming " + myEngine + " is started");
    addMemoryBar();
    addArtifactStatsBar();
  }

  private void addArtifactStatsBar() {
    addCounter(TotalItemsCounter.create(myContainer), 100);
    addCounter(LocallyChangedItemsCounter.create(myContainer), 70);
    addCounter(RemoteConflictItemsCounter.create(myContainer), 60);
    addCounter(SyncProblemItemsCounter.create(myContainer), 50);
  }

  private void addCounter(StatComponent counter, double weight) {
    myWindowManager.getStatusBar().addComponent(StatusBar.Section.ARTIFACT_STATS, counter, weight);
  }

  public void stop() {
    myDetach.detach();
  }

  private void addMemoryBar() {
    if (!myInfo.isProductionVersion() || Env.getBoolean(GlobalProperties.SHOW_MEM)) {
      myStatusPanel = new MemStatusComponent();
      StatusBarComponent.Simple component = new StatusBarComponent.Simple(myStatusPanel);
      myDetach.add(myWindowManager.getStatusBar().addComponent(StatusBar.Section.MEMORY_BAR, component));
    }
  }
}
