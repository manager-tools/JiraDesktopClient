package com.almworks.api.gui;

import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

/**
 * Utility class that handles dialogs that are shown only in one dialog at a time.
 * Override setupDialog() to provide content.
 */
public abstract class DialogSingleton {
  private final DialogManager myDialogManager;
  private final String myWindowId;

  private WindowController myWindow = null;

  private final Lifecycle myLifecycle = new Lifecycle();

  protected DialogSingleton(DialogManager dialogManager, String windowId) {
    myDialogManager = dialogManager;
    myWindowId = windowId;
  }

  /**
   * @return true if window has appeared, false if it already has been visible
   */
  public boolean show() {
    if (myWindow == null) {
      DialogBuilder builder = myDialogManager.createBuilder(myWindowId);
      setupDialog(builder);
      builder.showWindow(new Detach() {
        protected void doDetach() {
          DialogSingleton.this.detach();
          myWindow = null;
        }
      });
      myWindow = builder.getWindowContainer().getActor(WindowController.ROLE);
      attach();
      return true;
    } else {
      return myWindow.activate();
    }
  }

  protected abstract void setupDialog(DialogBuilder builder);

  protected void attach() {
  }

  protected void detach() {
    myLifecycle.cycle();
  }

  protected Lifespan lifespan() {
    return myLifecycle.lifespan();
  }

  public boolean isShowing() {
    return myWindow != null;
  }
}
