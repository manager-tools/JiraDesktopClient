package com.almworks.explorer;

import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

/**
 * @author dyoma
 */
public class ShowHideDistributionTableAction extends SimpleAction {
  public ShowHideDistributionTableAction() {
    super("Show &Tabular Distribution", Icons.TRANSPOSE_SUMMARY_TABLE_ACTION_SMALL);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Show/hide tabular distribution");
    watchRole(ExplorerDistributionTable.ROLE);
    watchRole(ExplorerForm.ROLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(EnableState.INVISIBLE);
    final ExplorerForm ef = context.getSourceObject(ExplorerForm.ROLE);
    context.updateOnChange(ef.getNavigationAreaModifiable());
    final ConnectionManager cm = context.getSourceObject(Engine.ROLE).getConnectionManager();
    context.updateOnChange(cm.getConnectionsModifiable());
    context.setEnabled(cm.getConnections().getCurrentCount() > 0);
    context.putPresentationProperty(PresentationKey.TOGGLED_ON,
      ef.isNavigationAreaShown() && isTableVisible(context));
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    final ExplorerForm ef = context.getSourceObject(ExplorerForm.ROLE);
    final boolean visible = isTableVisible(context);
    if(ef.isNavigationAreaShown()) {
      ef.showDistributionTable(!visible);
    } else {
      ef.showNavigationArea(true);
      if(!isTableVisible(context)) {
        ef.showDistributionTable(true);
      }
    }
  }

  private boolean isTableVisible(ActionContext context) {
    try {
      context.getSourceObject(ExplorerDistributionTable.ROLE);
      return true;
    } catch (CantPerformException e) {
      return false;
    }
  }
}
