package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class ServerWorkflowAction {
  public static final EntityKey<Integer> ID = EntityKey.integer("workflow.id", null);
  public static final EntityKey<Entity> PROJECT = EntityKey.entity("workflow.project", null);
  public static final EntityKey<Entity> ISSUE_TYPE = EntityKey.entity("workflow.issueType", null);
  public static final EntityKey<String> NAME = EntityKey.string("workflow.name", null);
  public static final EntityKey<List<Entity>> FIELDS = EntityKey.entityList("workflow.fields", null);
  public static final EntityKey<Collection<Entity>> MANDATORY_FIELDS = EntityKey.entityCollection( "workflow.mandatoryFields", null);
  public static final EntityKey<Entity> TARGET_STATUS = EntityKey.entity("workflow.targetStatus", null);

  public static final Entity TYPE = Entity.buildType("types.workflow").put(EntityResolution.KEY, EntityResolution.singleIdentity(true, ID, PROJECT, ISSUE_TYPE)).fix();

  @Nullable
  public static EntityHolder create(EntityTransaction transaction, int id, EntityHolder project, EntityHolder type) {
    EntityTransaction.IdentityBuilder builder = transaction.buildEntity(ServerWorkflowAction.TYPE);
    if (builder == null) {
      LogHelper.error("Failed to store workflow");
      return null;
    }
    builder.addValue(ServerWorkflowAction.ID, id);
    builder.addReference(ServerWorkflowAction.PROJECT, project);
    builder.addReference(ServerWorkflowAction.ISSUE_TYPE, type);
    return builder.create();
  }
}
