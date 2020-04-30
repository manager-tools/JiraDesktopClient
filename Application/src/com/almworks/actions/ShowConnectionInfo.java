package com.almworks.actions;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.util.L;
import com.almworks.util.commons.FunctionE;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.*;

public class ShowConnectionInfo extends ConnectionAction {
  public static final ShowConnectionInfo POPUP = new ShowConnectionInfo(SELECTED_NODE);
  public static final ShowConnectionInfo MAIN = new ShowConnectionInfo(PARENT_NODE);

  private ShowConnectionInfo(FunctionE<ActionContext, Connection, CantPerformException> connectionExtractor) {
    super(L.actionName("Show Connection &Info"), connectionExtractor);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Display connection configuration and stats"));
    watchRole(GenericNode.NAVIGATION_NODE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    Connection connection = extractConnection(context);
    if (connection == null) {
      context.setEnabled(EnableState.INVISIBLE);
      return;
    }
    UIComponentWrapper component = connection.getConnectionStateComponent();
    context.setEnabled(component != null);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    Connection connection = extractConnection(context);
    UIComponentWrapper component = connection.getConnectionStateComponent();
    if (component == null) {
      assert false : connection;
      component = UIComponentWrapper.Simple.message(L.content("No information available"));
    }
    String connectionName = context.getSourceObject(Engine.ROLE).getConnectionManager().getConnectionName(connection.getConnectionID());
    context.getSourceObject(ExplorerComponent.ROLE).showComponent(component, connectionName);
  }
}
