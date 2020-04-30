package com.almworks.jira.provider3.issue.features.move;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.UiItem;
import com.almworks.integers.LongArray;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.edit.editors.move.MoveController;
import com.almworks.jira.provider3.issue.features.BaseEditIssueFeature;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.jira.provider3.schema.IssueType;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class MoveKind {
  private static final LocalizedAccessor.MessageInt PRJTYPE_NAME = EditIssueFeature.I18N.messageInt("edit.screens.action.move.action.projectType.name");
  private static final LocalizedAccessor.MessageInt PRJTYPE_DESCRIPTION = EditIssueFeature.I18N.messageInt("edit.screens.action.move.action.projectType.description");
  private static final LocalizedAccessor.MessageInt SUBTASK_NAME = EditIssueFeature.I18N.messageInt("edit.screens.action.move.action.subtask.name");
  private static final LocalizedAccessor.MessageInt SUBTASK_DESCRIPTION = EditIssueFeature.I18N.messageInt("edit.screens.action.move.action.subtask.description");
  private static final LocalizedAccessor.MessageInt SUBTASK_GENERIC_NAME = EditIssueFeature.I18N.messageInt("edit.screens.action.move.action.subtaskGeneric.name");
  private static final LocalizedAccessor.MessageInt SUBTASK_GENERIC_DESCRIPTION = EditIssueFeature.I18N.messageInt("edit.screens.action.move.action.subtaskGeneric.description");
  private static final LocalizedAccessor.MessageInt PRJTYPESUB_NAME = EditIssueFeature.I18N.messageInt("edit.screens.action.move.action.projectTypeSub.name");
  private static final LocalizedAccessor.MessageInt PRJTYPESUB_DESCRIPTION = EditIssueFeature.I18N.messageInt("edit.screens.action.move.action.projectTypeSub.description");

  private final JiraConnection3 myConnection;
  private final List<ItemWrapper> myIssues;
  private final LocalizedAccessor.MessageInt myActionName;
  private final LocalizedAccessor.MessageInt myActionDescription;

  private MoveKind(JiraConnection3 connection, List<ItemWrapper> issues, LocalizedAccessor.MessageInt actionName, LocalizedAccessor.MessageInt actionDescription) {
    myConnection = connection;
    myIssues = issues;
    myActionName = actionName;
    myActionDescription = actionDescription;
  }

  @NotNull
  public static MoveKind chooseMove(ActionContext context) throws CantPerformException {
    Pair<JiraConnection3, List<ItemWrapper>> pair =  BaseEditIssueFeature.getContextIssues(context);
    ItemActionUtils.checkNotLocked(context, pair.getSecond());
    ArrayList<ItemWrapper> issues = Collections15.arrayList(pair.getSecond());
    JiraConnection3 connection = pair.getFirst();
    ModelKey<ItemKey> typeKey = MetaSchema.issueType(context);
    Boolean allSubtasks = areAllSubtasks(issues, typeKey);
    if (allSubtasks == null) { // Both subtasks and generic issues to be moved
      if (!isSameProject(context, issues)) throw new CantPerformException();
      return new MoveKind(connection, issues, SUBTASK_GENERIC_NAME, SUBTASK_GENERIC_DESCRIPTION);
    }
    if (!allSubtasks) // Only generic issues
      return isSameProject(context, issues) ?
        new MoveKind(connection, issues, PRJTYPESUB_NAME, PRJTYPESUB_DESCRIPTION) :
        new MoveKind(connection, issues, PRJTYPE_NAME, PRJTYPE_DESCRIPTION);
    if (!isSameProject(context, issues)) throw new CantPerformException();
    return new MoveKind(connection, issues, SUBTASK_NAME, SUBTASK_DESCRIPTION);
  }

  private static Boolean areAllSubtasks(ArrayList<ItemWrapper> issues, ModelKey<ItemKey> typeKey)
    throws CantPerformException
  {
    Boolean hasSubtasks = null;
    Boolean hasGeneric = null;
    for (Iterator<ItemWrapper> it = issues.iterator(); it.hasNext();) {
      ItemWrapper issue = it.next();
      LoadedItemKey type = getEnumValue(issue, typeKey);
      boolean isSubtask = IssueType.isSubtask(type, true);
      boolean isGeneric = IssueType.isSubtask(type, false);
      if (!isSubtask && !isGeneric) it.remove();
      hasSubtasks = MoveController.changeFlag(hasSubtasks, isSubtask, isGeneric);
      hasGeneric = MoveController.changeFlag(hasGeneric, isGeneric, isSubtask);
    }
    if (issues.isEmpty()) throw new CantPerformException();
    if (hasSubtasks == null || hasGeneric == null) {
      LogHelper.error("Should not happen", hasSubtasks, hasGeneric, issues);
      throw new CantPerformException();
    }
    if (hasSubtasks) return hasGeneric ? null : true;
    if (hasGeneric) return false;
    else {
      LogHelper.error("Should not happen", issues);
      throw new CantPerformException();
    }
  }

  private static boolean isSameProject(ActionContext context, List<ItemWrapper> issues)
    throws CantPerformException
  {
    ModelKey<ItemKey> projectKey = MetaSchema.project(context);
    ItemKey project = CantPerformException.ensureNotNull(issues.get(0).getModelKeyValue(projectKey));
    boolean sameProject = true;
    for (ItemWrapper issue : issues) {
      if (!Util.equals(project, issue.getModelKeyValue(projectKey))) sameProject = false;
    }
    return sameProject;
  }

  private static LoadedItemKey getEnumValue(ItemWrapper issue, ModelKey<ItemKey> key) throws CantPerformException {
    return CantPerformException.ensureNotNull(Util.castNullable(LoadedItemKey.class, issue.getModelKeyValue(key)));
  }

  public JiraConnection3 getConnection() {
    return myConnection;
  }

  public String getActionName() {
    return myActionName.formatMessage(myIssues.size());
  }

  public String getActionDescription() {
    return myActionDescription.formatMessage(myIssues.size());
  }

  public LongArray getIssueItems() {
    return  LongArray.create(UiItem.GET_ITEM.collectList(myIssues));
  }

  public List<ItemWrapper> getIssues() {
    return myIssues;
  }
}
