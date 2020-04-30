package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.api.util.EntityUtils;

import java.util.Date;

public class ServerComment {
  public static final EntityKey<Entity> ISSUE = EntityKey.entity("comment.issue", EntityKeyProperties.master());
  public static final EntityKey<Integer> ID = EntityKey.integer("comment.id", null);
  public static final EntityKey<String> TEXT = EntityKey.string("comment.text", EntityKeyProperties.shadowable());
  public static final EntityKey<Date> CREATED = EntityKey.date("comment.created", null);
  public static final EntityKey<Date> UPDATED = EntityKey.date("comment.updated", null);
  public static final EntityKey<Entity> AUTHOR = EntityKey.entity("comment.author", null);
  public static final EntityKey<Entity> EDITOR = EntityKey.entity("comment.editor", null);
  public static final EntityKey<Entity> SECURITY = EntityKey.entity("comment.security", EntityKeyProperties.shadowable());
  public static final Entity TYPE =
    Entity.buildType("types.comment").put(EntityResolution.KEY, EntityResolution.singleIdentity(true, ISSUE, ID)).fix();

  public static EntityHolder create(EntityHolder issue, int id) {
    if (issue == null) return null;
    return issue.getTransaction().addEntity(TYPE, ISSUE, issue, ID, id);
  }

  public static EntityHolder find(EntityTransaction transaction, int issueId, int commentId) {
    return EntityUtils.findSlave(transaction, ISSUE, ServerIssue.ID, issueId, TYPE, ID, commentId);
  }
}
