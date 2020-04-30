package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ServerActionSet {
  public static final EntityKey<Entity> PROJECT = EntityKey.entity("actionSet.project", null);
  public static final EntityKey<Entity> ISSUE_TYPE = EntityKey.entity("actionSet.issueType", null);
  public static final EntityKey<Entity> STATUS = EntityKey.entity("actionSet.status", null);
  public static final EntityKey<List<Entity>> ACTIONS = EntityKey.entityList("actionSet.actions", null);

  public static final Entity TYPE = Entity.buildType("types.actionSet")
    .put(EntityResolution.KEY, EntityResolution.singleIdentity(true, PROJECT, ISSUE_TYPE, STATUS))
    .fix();

  /**
   * Creates new workflow action set based on issue project/type/status. Returns a set if only there is no same set already defined. Because of actual action applicability may
   * depend on other issue fields conflicting changes are possible. To avoid them set workflow actions only for first set in transaction.
   * @return null if failed to create a set or if it already defined in the issue transaction.
   */
  @Nullable
  public static EntityHolder findNewSet(EntityHolder issue) {
    EntityHolder project = issue.getReference(ServerIssue.PROJECT);
    EntityHolder type = issue.getReference(ServerIssue.ISSUE_TYPE);
    EntityHolder status = issue.getReference(ServerIssue.STATUS);
    if (project == null || type == null || status == null) {
      LogHelper.error("Bad source for query", issue, project, type, status);
      return null;
    }
    EntityTransaction.IdentityBuilder builder = issue.getTransaction().buildEntity(ServerActionSet.TYPE);
    if (builder == null) return null;
    builder.addReference(PROJECT, project);
    builder.addReference(ISSUE_TYPE, type);
    builder.addReference(STATUS, status);
    if (builder.find() != null) return null;
    return builder.create();
  }
}
