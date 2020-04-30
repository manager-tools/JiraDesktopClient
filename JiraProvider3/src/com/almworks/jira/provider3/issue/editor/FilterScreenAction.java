package com.almworks.jira.provider3.issue.editor;

import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

class FilterScreenAction extends SimpleAction {
  public static final AnAction INSTANCE = new FilterScreenAction();

  public FilterScreenAction() {
    super(EditIssueFeature.I18N.getFactory("edit.screens.action.filterFields.name"), Icons.GENERIC_FILTER);
    watchRole(ScreenController.ROLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    ScreenController controller = context.getSourceObject(ScreenController.ROLE);
    FieldsFilter filter = controller.getFilter();
    context.updateOnChange(filter.getModifiable());
    CantPerformException.ensure(filter.canHide(IssueScreen.Tab.extractFields(controller.getCurrentTabs())));
    context.putPresentationProperty(PresentationKey.TOGGLED_ON, filter.isOn());
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    context.getSourceObject(ScreenController.ROLE).getFilter().toggle();
  }
}
