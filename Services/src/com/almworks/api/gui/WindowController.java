package com.almworks.api.gui;

import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.*;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.WindowFocusListener;

/**
 * @author dyoma
 */
public interface WindowController {
  Role<WindowController> ROLE = Role.role("windowController");

  CloseWindowAction CLOSE_ACTION = new CloseWindowAction();

  /**
   * Ask window to close. If window cannot be closed right now it stays visible
   * @return null if winow was closed. Or reason why it cannot be closed now
   */
  @Nullable
  CantPerformExceptionExplained close();

  /**
   * Inhibits confirmation dialog shown before close.
   * Does nothing if window has no close confirmation dialogs.
   * @param allowClose if false, user cannot close the window. if true, window closes silently
   */
  void disableCloseConfirmation(boolean allowClose);

  /**
   * Enables confirmation dialog. By default it is enabled, may be disabled using {@link #disableCloseConfirmation}.
   * Does nothing if window has no close confirmation dialogs.
   */
  void enableCloseConfirmation();

  void show();

  Lifespan getShowLife();

  void setTitle(String title);

  boolean isVisible();

  /**
   * Shows or focuses (if already visible) the window.
   * @return true if window was not visible before method call
   */
  boolean activate();

  void addDataProvider(DataProvider provider);

  /**
   * @deprecated for temporary workarounds
   */
  Window getWindow();

  Component getContentComponent();

  String getTitle();

  void toFront();

  void hide();

  void setHideOnClose(boolean hideOnClose);

  Detach addWindowFocusListener(WindowFocusListener listener);


  public static class CloseWindowAction extends SimpleAction {
    public CloseWindowAction() {
      super("Close Window");
      watchRole(ROLE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(ROLE);
      context.setEnabled(true);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      WindowController controller;
      try {
        controller = context.getSourceObject(ROLE);
      } catch (CantPerformException e) {
        if (e instanceof CantPerformExceptionExplained)
          throw e;
        // ignore - weird
        return;
      }
      CantPerformExceptionExplained cantClose = controller.close();
      if (cantClose != null)
        throw cantClose;
    }
  }
}
