package com.almworks.jira.provider3.gui.actions;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.GlobalLoginController;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.sync.JiraLoginController;
import com.almworks.util.ui.actions.*;

import java.util.List;

public class ResetLoginFailure extends SimpleAction {
  public ResetLoginFailure() {
    super("Reset Login Failure");
    watchRole(GlobalLoginController.ROLE);
    watchRole(GenericNode.NAVIGATION_NODE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(EnableState.INVISIBLE);
    context.putPresentationProperty(PresentationKey.NOT_AVALIABLE, true);
    JiraConnection3 connection = getConnection(context);
    GlobalLoginController controller = context.getSourceObject(GlobalLoginController.ROLE);
    context.updateOnChange(controller.getModifiable());
    String token = JiraLoginController.getLoginToken(connection);
    if (!controller.isLoginFailed(token)) return;
    context.putPresentationProperty(PresentationKey.NOT_AVALIABLE, false);
    context.setEnabled(EnableState.ENABLED);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    JiraConnection3 connection = getConnection(context);
    GlobalLoginController controller = context.getSourceObject(GlobalLoginController.ROLE);
    controller.clearFailureFlag(JiraLoginController.getLoginToken(connection));
  }

  private JiraConnection3 getConnection(ActionContext context) throws CantPerformException {
    List<GenericNode> nodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);
    return CantPerformException.ensureNotNull(JiraEditUtils.findConnection(context, nodes));
  }
}
