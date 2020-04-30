package com.almworks.actions;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.util.commons.FunctionE;
import com.almworks.util.ui.actions.*;

/**
 * "Edit Connection" menu action.
 */
public class EditConnectionAction extends ConnectionAction {
  public static final EditConnectionAction POPUP = new EditConnectionAction(SELECTED_NODE);
  public static final EditConnectionAction MAIN = new EditConnectionAction(PARENT_NODE);

  public EditConnectionAction(FunctionE<ActionContext, Connection, CantPerformException> connectionExtractor) {
    super("&Edit Connection Settings\u2026", connectionExtractor);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Change connection configuration");
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.watchRole(GenericNode.NAVIGATION_NODE);
    final Connection connection = extractConnection(context);
    if(connection == null || connection.getProvider().isEditingConnection(connection)) {
      context.setEnabled(EnableState.INVISIBLE);
    } else {
      context.setEnabled(EnableState.ENABLED);
    }
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final Connection connection = extractConnection(context);
    if(connection != null) {
      connection.getProvider().showEditConnectionWizard(connection);
    }
  }
}
