package com.almworks.jira.provider3.issue.editor;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.Util;

class VisibleFieldsAction extends SimpleAction {
  VisibleFieldsAction() {
    super(EditIssueFeature.I18N.getFactory("edit.screens.action.configureFields.name"), null);
    watchRole(GenericNode.NAVIGATION_NODE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    JiraConnection3 connection = getConnection(context);
    CantPerformException.ensure(connection.isUploadAllowed());
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final JiraConnection3 connection = getConnection(context);
    PrepareConfigureFields.start(context, connection, null, false);
  }

  private JiraConnection3 getConnection(ActionContext context) throws CantPerformException {
    GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
    return CantPerformException.ensureNotNull(Util.castNullable(JiraConnection3.class, node.getConnection()));
  }
}
