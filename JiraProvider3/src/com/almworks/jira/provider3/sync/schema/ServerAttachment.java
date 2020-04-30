package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.api.util.EntityUtils;

import java.util.Date;

public class ServerAttachment {
  public static final EntityKey<Entity> ISSUE = EntityKey.entity("attach.issue", EntityKeyProperties.master());
  public static final EntityKey<Integer> ID = EntityKey.integer("attach.id", null);
  public static final EntityKey<String> FILE_URL = EntityKey.string("attach.fileUrl", null);
  public static final EntityKey<String> DATE_STRING = EntityKey.string("attach.dateStr", null);
  public static final EntityKey<Date> DATE = EntityKey.date("attach.date", null);
  public static final EntityKey<String> MIME_TYPE = EntityKey.string("attach.mime", null);
  public static final EntityKey<String> SIZE_STRING = EntityKey.string("attach.sizeStr", null); // todo size seems to be integer in REST
  public static final EntityKey<String> FILE_NAME = EntityKey.string("attach.fileName", null);
  public static final EntityKey<Entity> AUTHOR = EntityKey.entity("attach.author", null);

  public static final Entity TYPE =
    Entity.buildType("types.attach").put(EntityResolution.KEY, EntityResolution.singleIdentity(true, ISSUE, FILE_URL)).fix();

  public static EntityHolder create(EntityHolder issue, String url) {
    if (issue == null) return null;
    return issue.getTransaction().addEntity(TYPE, ISSUE, issue, FILE_URL, url);
  }

  public static EntityHolder find(EntityHolder issue, int id) {
    Integer issueId = issue.getScalarValue(ServerIssue.ID);
    if (issueId == null) return null;
    return EntityUtils.findSlave(issue.getTransaction(), ISSUE, ServerIssue.ID, issueId, TYPE, ID, id);
  }
}
