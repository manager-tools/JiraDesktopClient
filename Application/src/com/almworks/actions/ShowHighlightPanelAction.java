package com.almworks.actions;

import com.almworks.api.explorer.TableController;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

public class ShowHighlightPanelAction extends SimpleAction {
  public ShowHighlightPanelAction() {
    super("&Find in Table", Icons.LOOKING_GLASS_SMALL);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Text search within results table");
    setDefaultPresentation(PresentationKey.TOGGLED_ON, false);
    watchRole(TableController.DATA_ROLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    final TableController controller = context.getSourceObject(TableController.DATA_ROLE);
    context.updateOnChange(controller.getHighlightPanelModifiable());
    context.putPresentationProperty(PresentationKey.TOGGLED_ON, controller.isHighlightPanelVisible());
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    final TableController controller = context.getSourceObject(TableController.DATA_ROLE);
    controller.setHighlightPanelVisible(!controller.isHighlightPanelVisible());
  }
}
