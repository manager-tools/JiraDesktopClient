package com.almworks.jira.provider3.links.actions;

import com.almworks.api.application.LoadedItem;
import com.almworks.items.gui.edit.engineactions.EditItemAction;
import com.almworks.items.gui.edit.engineactions.NewItemAction;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.util.Pair;
import com.almworks.util.ui.actions.*;

import java.util.Collections;
import java.util.List;

class LinkTwoIssues extends SimpleAction {
  public static final AnAction INSTANCE = new LinkTwoIssues();

  public LinkTwoIssues() {
    super("Link Two Issues");
    watchRole(LoadedItem.LOADED_ITEM);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(EnableState.INVISIBLE);
    EditItemAction.doUpdate(context, getEditFeature(context));
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    AddLinksFeature.LinkIssues editor = getEditFeature(context);
    EditDescriptor descriptor = EditItemAction.preparePerform(context, editor);
    NewItemAction.peform(context, editor, descriptor);
  }

  private AddLinksFeature.LinkIssues getEditFeature(ActionContext context) throws CantPerformException {
    Pair<JiraConnection3,List<LoadedItem>> pair = JiraEditUtils.selectLoadedIssues(context);
    Pair<LoadedItem, String> issues = split(pair.getSecond());
    return new AddLinksFeature.LinkIssues(pair.getFirst(), Collections.singletonList(issues.getFirst()),
      Collections.singletonList(issues.getSecond()));
  }

  private Pair<LoadedItem, String> split(List<LoadedItem> issues) throws CantPerformException {
    if (issues == null || issues.size() != 2) throw new CantPerformException();
    String key2 = MetaSchema.issueKey(issues.get(1));
    if (key2 != null) return Pair.create(issues.get(0), key2);
    String key1 = CantPerformException.ensureNotNull(MetaSchema.issueKey(issues.get(0)));
    return Pair.create(issues.get(1), key1);
  }
}
