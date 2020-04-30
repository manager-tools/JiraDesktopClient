package com.almworks.actions;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Log;

class RunQueryAction extends SimpleAction {
  public RunQueryAction() {
    super(L.actionName("&Run " + Terms.Query), Icons.ACTION_RUN_QUERY);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
      L.tooltip("Execute selected query and display results"));
    watchRole(GenericNode.NAVIGATION_NODE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    QueryResult result = context.getSourceObject(GenericNode.NAVIGATION_NODE).getQueryResult();
    context.getUpdateRequest().updateOnChange(result);
    if (!result.isRunnable())
      return;
    context.setEnabled(result.canRunNow() ? EnableState.ENABLED : EnableState.DISABLED);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    GenericNode node;
    try {
      node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
    } catch (CantPerformException e) {
      // there was a report of exception thrown here
      // while it is good to have this report, this essential functionality must not lead to "exceptions" dialog
      // especially for beginner users
      Log.warn(e);
      return;
    }
    QueryResult result = node.getQueryResult();
    assert result.isRunnable();
    ItemSource source = result.getItemSource();
    ItemCollectionContext collectionContext = result.getCollectionContext();
    if (source != null && collectionContext != null) {
      context.getSourceObject(ExplorerComponent.ROLE).showItemsInTab(source, collectionContext, false);
    }
  }
}
