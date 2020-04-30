package com.almworks.actions;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionState;
import com.almworks.api.engine.InitializationState;
import com.almworks.util.commons.FunctionE;
import com.almworks.util.ui.actions.*;

public class RetryInitializationAction extends ConnectionAction {
  public static final RetryInitializationAction POPUP = new RetryInitializationAction(SELECTED_NODE);
  public static final RetryInitializationAction MAIN = new RetryInitializationAction(PARENT_NODE);

  public RetryInitializationAction(FunctionE<ActionContext, Connection, CantPerformException> connectionExtractor) {
    super("Retry Initialization", connectionExtractor);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Retry connection initialization");
    watchRole(GenericNode.NAVIGATION_NODE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    final Connection connection = extractConnection(context);
    if(isReinitializationRequired(connection)) {
      context.setEnabled(EnableState.ENABLED);
    } else {
      context.setEnabled(EnableState.INVISIBLE);
      context.putPresentationProperty(PresentationKey.NOT_AVALIABLE, true);
    }
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final Connection connection = extractConnection(context);
    if(isReinitializationRequired(connection)) {
      connection.requestReinitialization();
    }
  }

  private boolean isReinitializationRequired(Connection connection) {
    if(connection == null) {
      return false;
    }

    final ConnectionState cState = connection.getState().getValue();
    if(cState == null || !cState.isReady()) {
      return false;
    }

    final InitializationState iState = connection.getInitializationState().getValue();
    if(iState == null || !iState.isInitializationRequired()) {
      return false;
    }

    return true;
  }
}
