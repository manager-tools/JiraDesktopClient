package com.almworks.integrate;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.misc.WorkArea;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.*;
import org.picocontainer.Startable;

public class IntegrateWithIDEAction extends SimpleAction implements Startable {
  public static final Role<IntegrateWithIDEAction> ROLE = Role.role(IntegrateWithIDEAction.class);

  private final ActionRegistry myActionRegistry;
  private final WorkArea myWorkArea;
  private final DialogManager myDialogManager;

  public IntegrateWithIDEAction(ActionRegistry actionRegistry, WorkArea workArea, DialogManager dialogManager) {
    super("&Integrate with IDE\u2026");
    myDialogManager = dialogManager;
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Installs TrackLink plug-in on a supported IDE");
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
    myActionRegistry = actionRegistry;
    myWorkArea = workArea;
  }

  public void start() {
    if (IntegrateWithIDEA.isTracklinkBundled()) {
      myActionRegistry.registerAction(MainMenu.Tools.INTEGRATE_WITH_IDE, this);
    }
  }

  public void stop() {
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    final DialogBuilder builder = myDialogManager.createBuilder("integrate");
    builder.setTitle("Integrate with IDE");
    final IntegrateDialog integrateDialog = new IntegrateDialog();
    builder.setContent(integrateDialog.getComponent());
    builder.setEmptyCancelAction();
    builder.setModal(true);
    builder.setOkAction(new SimpleAction("&Integrate") {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(integrateDialog.getSelectedIntegrationModel());
        IntegrationProcedure value = integrateDialog.getSelectedIntegrationModel().getValue();
        context.setEnabled(value != null);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        IntegrationProcedure procedure = integrateDialog.getSelectedIntegrationModel().getValue();
        if (procedure == null) {
          throw new CantPerformExceptionSilently("nothing selected");
        }
        boolean result = procedure.integrate();
        if (!result) {
          throw new CantPerformExceptionSilently("integration not successful");
        }
      }
    });
    builder.showWindow();
  }
}
