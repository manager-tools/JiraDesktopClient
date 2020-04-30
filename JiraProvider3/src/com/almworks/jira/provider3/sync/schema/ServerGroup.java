package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;

public class ServerGroup {
  public static final EntityKey<String> ID = EntityKey.string("group.id", null);

  public static final Entity TYPE;
  static {
    TYPE = Entity.buildType("types.group");
    TYPE.put(EntityResolution.KEY, EntityResolution.singleIdentity(true, ID));
    TYPE.fix();
  }
}
