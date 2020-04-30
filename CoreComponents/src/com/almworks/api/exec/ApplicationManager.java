package com.almworks.api.exec;

import com.almworks.util.L;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAbstractAction;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionSilently;
import org.almworks.util.detach.Detach;

/**
 * @author : Dyoma
 */
public interface ApplicationManager {
  Role<ApplicationManager> ROLE = Role.role(ApplicationManager.class);

  AnAbstractAction EXIT_ACTION = new AnAbstractAction(L.actionName("E&xit ")) {
   public void perform(ActionContext context) throws CantPerformException {
     if(!context.getSourceObject(ROLE).requestExit()) {
       throw new CantPerformExceptionSilently("Exit cancelled");
     }
   }
 };

  /**
   * Ask {@link ApplicationManager} to exit. Probably won't return.
   * @return false if the user has cancelled the exit.
   */
  boolean requestExit();

  /**
   * "Emergency" exit, no questions asked.
   */
  void forceExit();

  Detach addListener(Listener listener);

  void removeListener(Listener listener);

  int getFocusSwitchCount();

  class ExitRequest {
    private volatile boolean myCancelled = false;

    public void cancel() {
      myCancelled = true;
    }

    public boolean isCancelled() {
      return myCancelled;
    }
  }

  interface Listener {
    /**
     * Called when the app is asked to exit.
     * @param r A request that you can {@link ExitRequest#cancel()}.
     */
    void onExitRequested(ExitRequest r);

    /**
     * Called right before application exit
     */
    void onBeforeExit();
  }

  class Adapter implements Listener {
    @Override
    public void onExitRequested(ExitRequest r) {}

    @Override
    public void onBeforeExit() {}
  }
}
