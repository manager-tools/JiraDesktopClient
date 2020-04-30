package com.almworks.explorer;

import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

public class ShowHideNavigationAreaAction extends SimpleAction {
  public static final ShowHideNavigationAreaAction INSTANCE = new ShowHideNavigationAreaAction();

  private ShowHideNavigationAreaAction() {
    super("Show &Navigation Area", Icons.ACTION_SHOW_NAVIGATION_AREA);
    watchRole(ExplorerForm.ROLE);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Show/hide navigation area");
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(EnableState.INVISIBLE);
    final ExplorerForm ef = context.getSourceObject(ExplorerForm.ROLE);
    context.updateOnChange(ef.getNavigationAreaModifiable());

    final ConnectionManager cm = context.getSourceObject(Engine.ROLE).getConnectionManager();
    context.updateOnChange(cm.getConnectionsModifiable());
    context.setEnabled(cm.getConnections().getCurrentCount() > 0);

    context.putPresentationProperty(PresentationKey.TOGGLED_ON, ef.isNavigationAreaShown());
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    final ExplorerForm ef = context.getSourceObject(ExplorerForm.ROLE);
    ef.showNavigationArea(!ef.isNavigationAreaShown());
  }
}