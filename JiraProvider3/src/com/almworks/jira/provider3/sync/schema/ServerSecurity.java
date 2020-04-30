package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;

import java.util.Collection;

public class ServerSecurity {
  public static final EntityKey<Integer> ID = Commons.ENTITY_ID;
  public static final EntityKey<String> NAME = Commons.ENTITY_NAME;
  public static final EntityKey<Integer> ORDER = Commons.ENTITY_ORDER;
  public static final EntityKey<Collection<Entity>> ONLY_IN_PROJECTS = Commons.ONLY_IN_PROJECTS;

  public static final Entity TYPE = Commons.singleIdType(true, "types.security", ID);
}
