package com.almworks.jira.provider3.gui.actions;

import com.almworks.api.actions.ItemTableUtils;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ReplaceTabKey;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.source.IssuesByKeyItemSource;
import com.almworks.jira.provider3.gui.viewer.links.subtasks.LoadedIssue;
import com.almworks.jira.provider3.gui.viewer.links.subtasks.SubtasksFormlet;
import com.almworks.util.collections.LongSet;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;

import java.util.HashSet;
import java.util.List;

public class ViewSubtasksAction extends SimpleAction {
  static final ReplaceTabKey SUBTASK_TAB_KEY = new ReplaceTabKey("Subtasks");
  public static final AnAction ALL = new ViewSubtasksAction(true);
  public static final AnAction TRY_SELECTED = new ViewSubtasksAction(false);

  private final boolean myViewAll;

  public ViewSubtasksAction(boolean viewAll) {
    super("View This Issue and All Sub-Tasks", Icons.ACTION_VIEW_LINKED_ISSUE);
    myViewAll = viewAll;
    watchRole(ItemWrapper.ITEM_WRAPPER);
    watchRole(SubtasksFormlet.SUBTASKS);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    try {
      if (myViewAll) throw new CantPerformException("All sub-tasks only");
      CantPerformException.ensureNotEmpty(context.getSourceCollection(SubtasksFormlet.SUBTASKS));
      context.putPresentationProperty(PresentationKey.NAME, "View Selected Sub-Tasks");
    } catch (CantPerformException e) {
      ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
      CantPerformException.ensureNotEmpty(issue.getModelKeyValue(MetaSchema.subtasks(context)));
      context.putPresentationProperty(PresentationKey.NAME, "View All Sub-Tasks");
    }
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    JiraConnection3 connection = CantPerformException.cast(JiraConnection3.class, issue.getConnection());
    List<LoadedIssue> subtasks;
    long addIssue = 0;
    try {
      if (myViewAll) throw new CantPerformException("All sub-tasks only");
      subtasks = CantPerformException.ensureNotEmpty(context.getSourceCollection(SubtasksFormlet.SUBTASKS));
    } catch (CantPerformException e) {
      subtasks = CantPerformException.ensureNotEmpty(issue.getModelKeyValue(MetaSchema.subtasks(context)));
      addIssue = issue.getItem();
    }
    LongSet issues = new LongSet();
    HashSet<String> keys = Collections15.hashSet();
    for (LoadedIssue subtask : subtasks) {
      if (subtask.isDummy()) keys.add(subtask.getKey());
      else issues.add(subtask.getItem());
    }
    if (addIssue > 0) issues.add(addIssue);
    ItemSource source = IssuesByKeyItemSource.create("Sub-Tasks", connection, issues, keys);
    String key = MetaSchema.issueKey(issue);
    String tabName = key != null ? key + " Sub-Tasks" : "Sub-Tasks";
    ItemTableUtils.showArtifactSource(context, tabName, connection, source, SUBTASK_TAB_KEY);
  }
}
