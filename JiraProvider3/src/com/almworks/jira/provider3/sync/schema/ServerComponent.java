package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;

public class ServerComponent {
  public static final EntityKey<Integer> ID = Commons.ENTITY_ID;
  public static final EntityKey<String> NAME = Commons.ENTITY_NAME;
  public static final EntityKey<Entity> PROJECT = Commons.ENTITY_PROJECT;
  public static final Entity TYPE;
  static {
    TYPE = Entity.buildType("types.component");
    TYPE.put(EntityResolution.KEY, EntityResolution.singleIdentity(true, PROJECT, ID));
    TYPE.fix();
  }
}
