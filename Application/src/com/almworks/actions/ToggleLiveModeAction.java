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
class ToggleLiveModeAction extends SimpleAction {
  protected ToggleLiveModeAction() {
    super(L.actionName("&Auto-Refresh"), Icons.ACTION_LIVE_TABLE_MODE);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
    setDefaultPresentation(PresentationKey.TOGGLED_ON, Boolean.TRUE);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Keep query results updated by changes");
    watchRole(TableController.DATA_ROLE);
    watchRole(LifeMode.LIFE_MODE_DATA);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.getSourceObject(TableController.DATA_ROLE);
    LifeMode lifeMode = context.getSourceObject(LifeMode.LIFE_MODE_DATA);
    if (!lifeMode.isApplicable()) {
      context.setEnabled(EnableState.DISABLED);
      context.putPresentationProperty(PresentationKey.TOGGLED_ON, Boolean.FALSE);
    } else {
      context.setEnabled(EnableState.ENABLED);
      context.putPresentationProperty(PresentationKey.TOGGLED_ON, lifeMode.isLife());
    }
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    context.getSourceObject(TableController.DATA_ROLE).toggleLifeMode();
  }

  public static void registerActions(ActionRegistry registry) {
    registry.registerAction(MainMenu.Search.KEEP_LIVE_RESULTS, new ToggleLiveModeAction());
  }
}
