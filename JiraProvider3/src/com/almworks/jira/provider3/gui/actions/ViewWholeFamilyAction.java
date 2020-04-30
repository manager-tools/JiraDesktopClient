package com.almworks.jira.provider3.gui.actions;

import com.almworks.api.actions.ItemTableUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.source.LoadSubtasksSource;
import com.almworks.jira.provider3.gui.viewer.links.subtasks.LoadedIssue;
import com.almworks.util.LogHelper;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import org.jetbrains.annotations.NotNull;

public class ViewWholeFamilyAction extends SimpleAction {
  public static final AnAction ACTION = new ViewWholeFamilyAction();

  private ViewWholeFamilyAction() {
    super("View Parent Task and All Sub-Tasks", Icons.ACTION_VIEW_LINKED_ISSUE);
    watchRole(ItemWrapper.ITEM_WRAPPER);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    getParent(context, context.getSourceObject(ItemWrapper.ITEM_WRAPPER));
  }

  @NotNull
  private LoadedIssue getParent(ActionContext context, ItemWrapper issue) throws CantPerformException {
    return CantPerformException.ensureNotNull(issue.getModelKeyValue(MetaSchema.parentTask(context)));
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    LoadedIssue parentTask = getParent(context, issue);
    long parent = parentTask.getItem();
    String parentKey = parentTask.getKey();
    if (parent < 0 || parentKey == null) {
      LogHelper.error("Missing parent info", parent, parentKey, parentTask);
      throw new CantPerformException();
    }
    final JiraConnection3 connection = CantPerformException.cast(JiraConnection3.class, issue.getConnection());
    ItemTableUtils.showArtifactSource(context, "Sub-Tasks", issue.getConnection(),
      new LoadSubtasksSource(connection, parent), ViewSubtasksAction.SUBTASK_TAB_KEY);
  }
}
