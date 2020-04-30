package com.almworks.jira.provider3.sync.automerge;

import com.almworks.itemsync.MergeOperationsManager;
import com.almworks.jira.provider3.attachments.JiraAttachments;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.gui.timetrack.TimeAutoMerge;
import com.almworks.jira.provider3.links.JiraLinks;
import com.almworks.jira.provider3.schema.Comment;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.util.properties.Role;

public class JiraMergeSetup implements MergeOperationsManager.MergeProvider {
  public static final Role<MergeOperationsManager.MergeProvider> ROLE = Role.role("jiraMerge", MergeOperationsManager.MergeProvider.class);

  private final CustomFieldsComponent myCustomFieldEditors;

  public JiraMergeSetup(CustomFieldsComponent customFieldEditors) {
    myCustomFieldEditors = customFieldEditors;
  }

  @Override
  public void registerMergeOperations(MergeOperationsManager manager) {
    setupIssue(manager, myCustomFieldEditors);
    setupComments(manager);
    JiraLinks.setupMerge(manager);
    setupWorklogs(manager);
    JiraAttachments.setupMerge(manager);
  }


  private static void setupWorklogs(MergeOperationsManager manager) {
    manager.buildOperation(Worklog.DB_TYPE)
      .addConflictGroup(Worklog.TIME_SECONDS, Worklog.STARTED)
      .finish();
  }

  private static void setupComments(MergeOperationsManager manager) {
    manager.buildOperation(Comment.DB_TYPE)
      .discardEdit(Comment.AUTHOR, Comment.EDITOR, Comment.CREATED, Comment.CREATED, Comment.ID)
      .finish();
  }

  private static void setupIssue(MergeOperationsManager manager, CustomFieldsComponent customFieldEditors) {
    manager.buildOperation(Issue.DB_TYPE)
      .discardEdit(Issue.KEY, Issue.CREATED, Issue.UPDATED, Issue.RESOLVED)
      .mergeLongSets(Issue.AFFECT_VERSIONS, Issue.FIX_VERSIONS, Issue.COMPONENTS)
      .addCustom(new IssueFieldsMerge(customFieldEditors))
      .addCustom(new UserToggleSetMerge(Issue.VOTERS, Issue.VOTES_COUNT, Issue.VOTED))
      .addCustom(new UserToggleSetMerge(Issue.WATCHERS, Issue.WATCHERS_COUNT, Issue.WATCHING))
      .addCustom(new TimeAutoMerge())
      .addCustom(new MoveStepMerge())
      .finish();
  }
}
