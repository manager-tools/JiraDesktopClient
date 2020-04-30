package com.almworks.actions;

import com.almworks.api.application.LifeMode;
import com.almworks.api.explorer.TableController;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.L;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

/**
 * @author dyoma
 */
class RefreshArtifactsTableAction extends SimpleAction {
  protected RefreshArtifactsTableAction() {
    super(L.actionName("Refresh &Table"), Icons.ACTION_REFRESH_TABLE);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Refresh table with the latest data");
    watchRole(TableController.DATA_ROLE);
    watchRole(LifeMode.LIFE_MODE_DATA);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    TableController table = context.getSourceObject(TableController.DATA_ROLE);
    LifeMode mode = context.getSourceObject(LifeMode.LIFE_MODE_DATA);
    context.updateOnChange(table.getCollectionModel());
    context.setEnabled(EnableState.DISABLED);
    if (mode.isLife())
      return;
    if (mode.hasNewElements())
      context.setEnabled(EnableState.ENABLED);
    else {
      context.setEnabled(table.isContentOutOfDate() ? EnableState.ENABLED : EnableState.DISABLED);
    }
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    context.getSourceObject(TableController.DATA_ROLE).updateAllArtifacts();
  }

  public static void registerActions(ActionRegistry registry) {
    registry.registerAction(MainMenu.Search.REFRESH_RESULTS, new RefreshArtifactsTableAction());
  }
}
