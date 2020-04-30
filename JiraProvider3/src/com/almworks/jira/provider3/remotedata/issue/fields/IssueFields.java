package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.jira.provider3.remotedata.issue.fields.scalar.ScalarFieldDescriptor;
import com.almworks.jira.provider3.sync.download2.rest.*;
import com.almworks.jira.provider3.sync.schema.*;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public class IssueFields {
  public static final JsonEntityParser.Impl<Integer> P_PROJECT = JsonEntityParser.create(ServerProject.TYPE, ServerProject.ID, ServerProject.NAME, JRProject.PARSER, "id");
  public static final JsonEntityParser.Impl<Integer> P_ISSUE_TYPE = JsonEntityParser.create(ServerIssueType.TYPE, ServerIssueType.ID, ServerIssueType.NAME, JRIssueType.PARSER, "id");
  public static final JsonEntityParser.Impl<Integer> P_STATUS = JsonEntityParser.create(ServerStatus.TYPE, ServerStatus.ID, ServerStatus.NAME, JRStatus.PARSER, "id");
  public static final JsonEntityParser.Impl<Integer> P_SECURITY = JsonEntityParser.create(ServerSecurity.TYPE, ServerSecurity.ID, ServerSecurity.NAME, JRSecurity.PARSER, "id");
  public static final JsonEntityParser.Impl<Integer> P_PRIORITY = JsonEntityParser.create(ServerPriority.TYPE, ServerPriority.ID, ServerPriority.NAME, JRPriority.PARSER, "id");
  public static final JsonEntityParser.Impl<Integer> P_RESOLUTION = JsonEntityParser.create(ServerResolution.TYPE, ServerResolution.ID, ServerResolution.NAME, JRResolution.PARSER, "id");
  public static final JsonUserParser P_USER = JsonUserParser.INSTANCE;
  public static final JsonEntityParser.Impl<String> P_GROUP = JsonEntityParser.create(ServerGroup.TYPE, ServerGroup.ID, ServerGroup.ID, JRGroup.PARSER, "name");
  public static final JsonEntityParser.Impl<Integer> P_VERSION = JsonEntityParser.create(ServerVersion.TYPE, ServerVersion.ID, ServerVersion.NAME, JRVersion.PARSER, "id");
  public static final JsonEntityParser.Impl<Integer> P_COMPONENT = JsonEntityParser.create(ServerComponent.TYPE, ServerComponent.ID, ServerComponent.NAME, JRComponent.PARSER, "id");

  public static final EntityType<Integer> T_PROJECT = EntityType.create(P_PROJECT, null);
  public static final EntityType<Integer> T_ISSUE_TYPE = EntityType.create(P_ISSUE_TYPE, null);
  public static final EntityType<Integer> T_STATUS = EntityType.create(P_STATUS, null);
  public static final EntityType<Integer> T_SECURITY = EntityType.create(P_SECURITY, null);
  public static final EntityType<Integer> T_PRIORITY = EntityType.create(P_PRIORITY, null);
  public static final EntityType<Integer> T_RESOLUTION = EntityType.create(P_RESOLUTION, ServerResolution.UNRESOLVED);
  public static final EntityType<String> T_USER = EntityType.create(P_USER, null);
  public static final EntityType<Integer> T_VERSION = EntityType.create(P_VERSION, null);
  public static final EntityType<Integer> T_COMPONENT = EntityType.create(P_COMPONENT, null);

  public static final EntityFieldDescriptor<Integer> PROJECT = EntityFieldDescriptor.special("project", "Project", ServerIssue.PROJECT, T_PROJECT, EntityType.GENERIC_JSON);
  public static final EntityFieldDescriptor<Integer> ISSUE_TYPE = EntityFieldDescriptor.special("issuetype", "Type", ServerIssue.ISSUE_TYPE, T_ISSUE_TYPE, EntityType.GENERIC_JSON);
  public static final EntityFieldDescriptor<Integer> STATUS = EntityFieldDescriptor.special("status", "Status", ServerIssue.STATUS, T_STATUS, EntityType.GENERIC_JSON);
  public static final EntityFieldDescriptor<Integer> SECURITY = EntityFieldDescriptor.noConfirm("security", "Security Level", ServerIssue.SECURITY, T_SECURITY);
  public static final ScalarFieldDescriptor<Date> CREATED = ScalarFieldDescriptor.readonlyDateTime("created", "Created", ServerIssue.CREATED, false);
  public static final ScalarFieldDescriptor<Date> UPDATED = ScalarFieldDescriptor.readonlyDateTime("updated", "Updated", ServerIssue.UPDATED, true);
  public static final ScalarFieldDescriptor<Date> RESOLUTION_DATE = ScalarFieldDescriptor.readonlyDateTime("resolutiondate", "Resolved", ServerIssue.RESOLVED, false);
  public static final ScalarFieldDescriptor<Integer> DUE = ScalarFieldDescriptor.editableDays("duedate", "Due Date", ServerIssue.DUE);

  public static final ScalarFieldDescriptor<String> SUMMARY = ScalarFieldDescriptor.editableText("summary", "Summary", ServerIssue.SUMMARY);
  public static final ScalarFieldDescriptor<String> DESCRIPTION = ScalarFieldDescriptor.editableText("description", "Description", ServerIssue.DESCRIPTION);
  public static final ScalarFieldDescriptor<String> ENVIRONMENT = ScalarFieldDescriptor.editableText("environment", "Environment", ServerIssue.ENVIRONMENT);
  public static final EntityFieldDescriptor<Integer> PRIORITY = EntityFieldDescriptor.generic("priority", "Priority", ServerIssue.PRIORITY, T_PRIORITY);
  public static final EntityFieldDescriptor<Integer> RESOLUTION = EntityFieldDescriptor.generic("resolution", "Resolution", ServerIssue.RESOLUTION, T_RESOLUTION);
  public static final EntityFieldDescriptor<String> REPORTER = EntityFieldDescriptor.generic("reporter", "Reporter", ServerIssue.REPORTER, T_USER);
  public static final EntityFieldDescriptor<String> ASSIGNEE = EntityFieldDescriptor.generic("assignee", "Assignee", ServerIssue.ASSIGNEE, T_USER);
  public static final MultiEntityDescriptor FIX_VERSIONS = MultiEntityDescriptor.create("fixVersions", "Fix Versions", ServerIssue.FIX_VERSIONS, T_VERSION);
  public static final MultiEntityDescriptor AFFECTS_VERSIONS = MultiEntityDescriptor.create("versions", "Affects Versions", ServerIssue.AFFECTED_VERSIONS, T_VERSION);
  public static final MultiEntityDescriptor COMPONENTS = MultiEntityDescriptor.create("components", "Components", ServerIssue.COMPONENTS, T_COMPONENT);

  @SuppressWarnings("unchecked")
  public static final List<IssueFieldDescriptor> DESCRIPTORS = Collections15.unmodifiableListCopy(
    PROJECT, ISSUE_TYPE, STATUS, SECURITY, PRIORITY, RESOLUTION,
    CREATED, UPDATED, RESOLUTION_DATE, DUE,
    SUMMARY, DESCRIPTION, ENVIRONMENT,
    REPORTER, ASSIGNEE,
    FIX_VERSIONS, AFFECTS_VERSIONS, COMPONENTS
  );

  @Nullable
  public static IssueFormValue findFormValue(Collection<? extends IssueFieldValue> values, String fieldId) {
    if (fieldId == null) return null;
    for (IssueFieldValue value : values) {
      IssueFormValue formValue = Util.castNullable(IssueFormValue.class, value);
      if (formValue != null && fieldId.equals(formValue.getDescriptor().getFieldId())) return formValue;
    }
    return null;
  }
}
