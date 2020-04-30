package com.almworks.sync;

import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.api.container.EventRouter;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.EngineListener;
import com.almworks.api.engine.SyncProblem;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.sync.SynchronizationWindow;
import com.almworks.util.L;
import com.almworks.util.config.ConfigAccessors;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.*;
import org.almworks.util.detach.Lifespan;
import org.picocontainer.Startable;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SynchronizationWindowImpl implements SynchronizationWindow, Startable {
  private SyncWindow myForm;
  private final DialogManager myDialogManager;
  private final EventRouter myRouter;
  private final Engine myEngine;
  private final ActionRegistry myActionRegistry;
  private final TextDecoratorRegistry myTextDecoratorRegistry;
  private final Configuration myConfiguration;
  private final ConfigAccessors.Bool myShowOnSync;
  private final SynchronizerStatusBar mySynchronizerStatusBar;

  public SynchronizationWindowImpl(Configuration configuration, DialogManager dialogManager, EventRouter router,
    Engine engine, MainWindowManager windowManager, ActionRegistry actionRegistry, TextDecoratorRegistry textDecoratorRegistry) {

    myConfiguration = configuration;
    myDialogManager = dialogManager;
    myRouter = router;
    myEngine = engine;
    myActionRegistry = actionRegistry;
    myTextDecoratorRegistry = textDecoratorRegistry;
    myShowOnSync = ConfigAccessors.bool(myConfiguration, "showOnSync", true);
    mySynchronizerStatusBar = new SynchronizerStatusBar(windowManager, engine.getSynchronizer(), this);
  }

  public void start() {
    mySynchronizerStatusBar.start();
    myActionRegistry.registerAction(MainMenu.Tools.SHOW_SYNCHRONIZATION_WINDOW,
      new SimpleAction(L.actionName("Show Synchronization &Window")) {
      {
        setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
          L.tooltip("Display synchronization status and synchronization problems"));
      }

      protected void customUpdate(UpdateContext context) throws CantPerformException {}

      protected void doPerform(ActionContext context) throws CantPerformException {
        show();
      }
    });
   myRouter.addListener(Lifespan.FOREVER, ThreadGate.AWT, new EngineListener.Adapter() {
     public void onSynchronizationRequested(boolean requireUI) {
       if (requireUI || myShowOnSync.getBool())
         show();
     }
   });
  }

  public void stop() {
  }

  public void show() {
    Threads.assertAWTThread();
    if (myForm == null)
      myForm = new SyncWindow(myDialogManager, myConfiguration, myEngine.getSynchronizer(), myShowOnSync, myTextDecoratorRegistry);
    myForm.show();
  }

  public void showProblem(SyncProblem problem) {
    show();
    assert myForm != null;
    myForm.focusOnProblem(problem);
  }
}
