package com.almworks.jira.provider3.links.structure.ui;

import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemsTreeLayout;
import com.almworks.api.engine.Connection;
import com.almworks.api.explorer.AdditionalHierarchies;
import com.almworks.api.explorer.TableController;
import com.almworks.api.gui.DialogManager;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.jira.provider3.links.JiraLinks;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

public class CustomizeHierarchyAction extends SimpleAction {
  public static final AnAction INSTANCE = new CustomizeHierarchyAction();

  public CustomizeHierarchyAction() {
    super(JiraLinks.I18N.getFactory("hierarchy.action.customize.name"), Icons.FILE_VIEW_DETAILS);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    // The action may be updated after new tab has been inserted into Swing tree, but before tabs switches to new tab - this way the action may get Connection from previous tab,
    // so it has to be updated when tab switch is complete
    watchRole(ItemCollectionContext.ROLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    getHierarchies(context);
    context.setEnabled(EnableState.ENABLED);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    JiraComplexHierarchies hierarchies = getHierarchies(context);
    GuiFeaturesManager features = context.getSourceObject(GuiFeaturesManager.ROLE);
    ItemsTreeLayout layout;
    try {
      layout = context.getSourceObject(ItemsTreeLayout.DATA_ROLE);
    } catch (CantPerformException e) {
      layout = null;
    }
    HierarchyEditor.showWindow(context.getSourceObject(DialogManager.ROLE), hierarchies, features, layout, context.getSourceObject(TableController.DATA_ROLE));
  }

  private JiraComplexHierarchies getHierarchies(ActionContext context) throws CantPerformException {
    ItemCollectionContext collectionContext = context.getSourceObject(ItemCollectionContext.ROLE);
    Connection connection = CantPerformException.ensureNotNull(collectionContext.getSourceConnection());
    AdditionalHierarchies additional = CantPerformException.ensureNotNull(connection.getActor(AdditionalHierarchies.ROLE));
    return CantPerformException.cast(JiraComplexHierarchies.class, additional);
  }
}
