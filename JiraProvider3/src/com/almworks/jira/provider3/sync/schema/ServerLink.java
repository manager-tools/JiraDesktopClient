package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ServerLink {
  public static final EntityKey<Entity> SOURCE = EntityKey.entity("link.source", EntityKeyProperties.master());
  public static final EntityKey<Entity> TARGET = EntityKey.entity("link.target", EntityKeyProperties.master());
  public static final EntityKey<Entity> LINK_TYPE = EntityKey.entity("link.type", null);
  public static final EntityKey<Integer> ID = EntityKey.integer("link.id", null);
  public static final Entity TYPE =
    Entity.buildType("types.link").put(EntityResolution.KEY, EntityResolution.singleIdentity(true, SOURCE, TARGET, LINK_TYPE)).fix();

  @NotNull
  public static List<EntityHolder> findLink(EntityTransaction transaction, int sourceId, int targetId, int typeId) {
    List<EntityHolder> allLinks = transaction.getAllEntities(TYPE);
    ArrayList<EntityHolder> result = Collections15.arrayList();
    for (EntityHolder link : allLinks) {
      Integer id = link.getScalarFromReference(SOURCE, ServerIssue.ID);
      if (id == null || sourceId != id) continue;
      id = link.getScalarFromReference(TARGET, ServerIssue.ID);
      if (id == null || targetId != id) continue;
      EntityHolder linkType = link.getReference(LINK_TYPE);
      if (linkType == null) LogHelper.error("Missing link type", link);
      else {
        Integer linkTypeId = linkType.getScalarValue(ServerLinkType.ID);
        if (linkTypeId != null && typeId == linkTypeId) result.add(link);
      }
    }
    return result;
  }
}
