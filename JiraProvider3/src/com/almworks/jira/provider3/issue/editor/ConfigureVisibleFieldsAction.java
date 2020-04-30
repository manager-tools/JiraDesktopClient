package com.almworks.jira.provider3.issue.editor;

import com.almworks.integers.LongArray;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.util.commons.Procedure;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import java.util.Map;

class ConfigureVisibleFieldsAction extends SimpleAction {
  public static final AnAction INSTANCE = new ConfigureVisibleFieldsAction();

  ConfigureVisibleFieldsAction() {
    super(EditIssueFeature.I18N.getFactory("edit.screens.action.configureVisibleFields.name"), Icons.FILE_VIEW_DETAILS);
    watchRole(ScreenController.ROLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    ScreenController controller = context.getSourceObject(ScreenController.ROLE);
    EngineConsts.getConnection(JiraConnection3.class, controller.getModel());
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final ScreenController controller = context.getSourceObject(ScreenController.ROLE);
    final JiraConnection3 connection = EngineConsts.getConnection(JiraConnection3.class, controller.getModel());
    PrepareConfigureFields.start(context, connection, new Procedure<Map<ResolvedField, Boolean>>() {
      @Override
      public void invoke(Map<ResolvedField, Boolean> arg) {
        LongArray hidden = new LongArray();
        for (Map.Entry<ResolvedField, Boolean> entry : arg.entrySet()) if (!entry.getValue()) hidden.add(entry.getKey().getItem());
        controller.getFilter().setHidden(hidden);
      }
    }, true);
  }
}
