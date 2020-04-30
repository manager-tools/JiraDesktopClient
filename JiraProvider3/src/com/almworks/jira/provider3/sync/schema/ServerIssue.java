package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public class ServerIssue {
  public static final EntityKey<Integer> ID = EntityKey.integer("issue.id", null);
  public static final EntityKey<String> KEY = EntityKey.string("issue.key", EntityKey.buildKey().put(EntityHolder.MUTABLE_IDENTITY, true));
  public static final EntityKey<Entity> PARENT = EntityKey.entity("issue.parent", EntityKeyProperties.propagateChange(true));
  public static final EntityKey<String> SUMMARY = EntityKey.string("issue.summary", EntityKeyProperties.shadowable());
  public static final EntityKey<String> DESCRIPTION = EntityKey.string("issue.description", EntityKeyProperties.shadowable());
  public static final EntityKey<String> ENVIRONMENT = EntityKey.string("issue.environment", EntityKeyProperties.shadowable());
  public static final EntityKey<Date> CREATED = EntityKey.date("issue.created", null);
  public static final EntityKey<Date> UPDATED = EntityKey.date("issue.updated", null);
  public static final EntityKey<String> UPDATED_STRING = EntityKey.hint("issue.updatedText", String.class);
  public static final EntityKey<Integer> DUE = EntityKey.integer("issue.due", EntityKeyProperties.shadowable());
  public static final EntityKey<Date> RESOLVED = EntityKey.date("issue.resolved", null);
  public static final EntityKey<Entity> PROJECT = EntityKey.entity("issue.project", EntityKeyProperties.shadowable());
  public static final EntityKey<Entity> ISSUE_TYPE = EntityKey.entity("issue.type", EntityKeyProperties.shadowable());
  public static final EntityKey<Entity> STATUS = EntityKey.entity("issue.status", EntityKeyProperties.shadowable());
  public static final EntityKey<Entity> PRIORITY = EntityKey.entity("issue.priority", EntityKeyProperties.shadowable());
  public static final EntityKey<Entity> RESOLUTION = EntityKey.entity("issue.resolution", EntityKeyProperties.shadowable());
  public static final EntityKey<Entity> REPORTER = EntityKey.entity("issue.reporter", EntityKeyProperties.shadowable());
  public static final EntityKey<Entity> ASSIGNEE = EntityKey.entity("issue.assignee", EntityKeyProperties.shadowable());
  public static final EntityKey<Entity> SECURITY = EntityKey.entity("issue.security", EntityKeyProperties.shadowable());
  public static final EntityKey<Collection<Entity>> COMPONENTS = EntityKey.entityCollection("issue.components",
    EntityKeyProperties.shadowable());
  public static final EntityKey<Collection<Entity>> AFFECTED_VERSIONS = EntityKey.entityCollection(
    "issue.affectedVersions", EntityKeyProperties.shadowable());
  public static final EntityKey<Collection<Entity>> FIX_VERSIONS = EntityKey.entityCollection("issue.fixVersions",
    EntityKeyProperties.shadowable());
  /** Null value means "no value", may indicate that voting is disabled on server */
  public static final EntityKey<Integer> VOTES_COUNT = EntityKey.integer("issue.votesCount", EntityKeyProperties.shadowable());
  // Details
  /** Null value means "no value", may indicate that watching is disabled on server */
  public static final EntityKey<Integer> WATCHERS_COUNT = EntityKey.integer("issue.watchCount", EntityKeyProperties.shadowable());
  public static final EntityKey<Boolean> VOTED = EntityKey.bool("issue.voted", EntityKeyProperties.shadowable());
  public static final EntityKey<Boolean> WATCHING = EntityKey.bool("issue.watching", EntityKeyProperties.shadowable());
  public static final EntityKey<Collection<Entity>> VOTERS = EntityKey.entityCollection("issue.voters", EntityKeyProperties
    .shadowable());
  public static final EntityKey<Collection<Entity>> WATCHERS = EntityKey.entityCollection("issue.watchers", EntityKeyProperties
    .shadowable());

  public static final EntityKey<Integer> ORIGINAL_ESTIMATE = EntityKey.integer("issue.originalEstimate", EntityKeyProperties
    .shadowable());
  public static final EntityKey<Integer> REMAIN_ESTIMATE = EntityKey.integer("issue.remainEstimate", EntityKeyProperties
    .shadowable());
  public static final EntityKey<Integer> TIME_SPENT = EntityKey.integer("issue.timeSpent", EntityKeyProperties.shadowable());

  public static final EntityKey<Collection<Entity>> APPLICABLE_WORKFLOW_ACTIONS = EntityKey.entityCollection("issue.applicableWorkflowActions", null);
  /** All fields present in editmeta */
  public static final EntityKey<List<Entity>> FIELDS_FOR_EDIT = EntityUtils.emptyList("issue.customFieldsForEdit");

  public static final Entity TYPE = Entity.buildType("types.issue")
    .put(EntityResolution.KEY, EntityResolution.singleAttributeIdentities(true, ServerIssue.ID, ServerIssue.KEY))
    .fix();

  @Nullable
  public static EntityHolder create(EntityTransaction transaction, int id, @Nullable String key) {
    EntityHolder issue = transaction.addEntity(TYPE, ID, id);
    if (issue == null) return null;
    issue.setNNValue(KEY, key);
    DownloadStageMark.DUMMY.setTo(issue);
    return issue;
  }

  @Nullable
  public static EntityHolder findIssue(EntityTransaction transaction, int issueId) {
    for (EntityHolder issue : transaction.getAllEntities(TYPE)) {
      Integer id = issue.getScalarValue(ID);
      if (id != null && id == issueId) return issue;
    }
    return null;
  }
}
