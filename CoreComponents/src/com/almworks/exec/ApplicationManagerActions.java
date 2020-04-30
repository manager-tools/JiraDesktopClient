package com.almworks.exec;

import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.Env;
import com.almworks.util.ui.MacIntegration;
import com.almworks.util.ui.actions.ActionRegistry;
import org.picocontainer.Startable;

/**
 * @author : Dyoma
 */
public class ApplicationManagerActions implements Startable {
  private final ApplicationManager myApplicationManager;
  private final ActionRegistry myRegistry;

  public ApplicationManagerActions(ApplicationManager applicationManager, ActionRegistry registry) {
    myApplicationManager = applicationManager;
    myRegistry = registry;
  }

  public void start() {
    if(!Env.isMac()) {
      // On Mac there's "Quit" in the application menu.
      myRegistry.registerAction(MainMenu.File.EXIT, ApplicationManager.EXIT_ACTION);
    } else {
      MacIntegration.setQuitHandler(new Runnable() {
        public void run() {
          if (!myApplicationManager.requestExit()) {
            MacIntegration.cancelEvent();
          }
        }
      });
    }
  }

  public void stop() {

  }
}
