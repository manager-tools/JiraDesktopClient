package com.almworks.jira.provider3.links.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.jira.provider3.links.LoadedLink2;
import com.almworks.util.DECL;
import com.almworks.util.L;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import java.util.List;

class ViewLinkedIssuesAction extends SimpleAction {
  public static final AnAction INSTANCE = new ViewLinkedIssuesAction();

  private ViewLinkedIssuesAction() {
    super(L.actionName("View Issues"), Icons.ACTION_VIEW_LINKED_ISSUE);
    watchRole(LoadedLink2.DB_LINK);
    watchRole(ItemWrapper.ITEM_WRAPPER);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    try {
      getSelectedLinks(context);
      context.putPresentationProperty(PresentationKey.NAME, "View Selected Issues");
      return;
    } catch (CantPerformException e) {
      DECL.ignoreException();
    }
    ViewAllLinkedAction.getAllTargetLinks(context, issue);
    context.putPresentationProperty(PresentationKey.NAME, "View All Linked Issues");
  }

  private List<LoadedLink2> getSelectedLinks(ActionContext context) throws CantPerformException {
    return CantPerformException.ensureNotEmpty(context.getSourceCollection(LoadedLink2.DB_LINK));
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    try {
      List<LoadedLink2> links = getSelectedLinks(context);
      ViewAllLinkedAction.showLinked(context, "Linked Issues", issue.getConnection(), links, 0);
      return;
    } catch (CantPerformException e) {
      DECL.ignoreException();
    }
    ViewAllLinkedAction.showAllLinked(context);
  }
}
