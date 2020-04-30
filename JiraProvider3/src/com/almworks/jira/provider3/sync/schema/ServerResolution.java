package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;

import java.util.Collection;

public class ServerResolution {
  public static final EntityKey<Integer> ID = Commons.ENTITY_ID;
  public static final EntityKey<String> NAME = Commons.ENTITY_NAME;
  public static final Entity TYPE = Commons.singleIdType(true, "types.resolution", ID);
  public static final EntityKey<String> DESCRIPTION = Commons.ENTITY_DESCRIPTION;
  public static final EntityKey<Integer> ORDER = Commons.ENTITY_ORDER;
  public static final EntityKey<Collection<Entity>> ONLY_IN_PROJECTS = Commons.ONLY_IN_PROJECTS;
  public static final EntityKey<String> ICON_URL = Commons.ICON_URL;
  public static final String UNRESOLVED_NAME = "Unresolved";

  public static final Entity UNRESOLVED = new Entity(TYPE).put(ID, -1).fix();

  public static EntityHolder createUnresolved(EntityTransaction transaction) {
    return transaction.addEntity(ServerResolution.TYPE, ServerResolution.ID, -1);
  }
}
