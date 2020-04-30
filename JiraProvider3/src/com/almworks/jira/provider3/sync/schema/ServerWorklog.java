package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.api.util.EntityUtils;

import java.util.Date;

public class ServerWorklog {
  public static final EntityKey<Integer> ID = EntityKey.integer("worklog.id", null);
  public static final EntityKey<Entity> ISSUE = EntityKey.entity("worklog.issue", EntityKeyProperties.master());
  public static final EntityKey<Integer> TIME_SECONDS = EntityKey.integer("worklog.timeSeconds", EntityKeyProperties.shadowable());
  public static final EntityKey<String> COMMENT = EntityKey.string("worklog.comment", EntityKeyProperties.shadowable());
  public static final EntityKey<Entity> AUTHOR = EntityKey.entity("worklog.author", null);
  public static final EntityKey<Entity> EDITOR = EntityKey.entity("worklog.editor", EntityKeyProperties.shadowable()); // NOTE: Should not be shadowable
  public static final EntityKey<Date> START_DATE = EntityKey.date("worklog.started", EntityKeyProperties.shadowable());
  public static final EntityKey<Date> CREATED = EntityKey.date("worklog.created", null);
  public static final EntityKey<Date> UPDATED = EntityKey.date("worklog.updated", null);
  public static final EntityKey<Entity> SECURITY = EntityKey.entity("worklog.security", EntityKeyProperties.shadowable());

  public static final Entity TYPE = Entity.buildType("types.worklog").put(EntityResolution.KEY, EntityResolution.singleIdentity(true, ISSUE, ID)).fix();

  public static EntityHolder find(EntityTransaction transaction, int issueId, int id) {
    return EntityUtils.findSlave(transaction, ISSUE, ServerIssue.ID, issueId, TYPE, ID, id);
  }
}
