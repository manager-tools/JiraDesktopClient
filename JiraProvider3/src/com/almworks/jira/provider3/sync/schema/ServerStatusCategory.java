package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;

public class ServerStatusCategory {
  public static final EntityKey<Integer> ID = Commons.ENTITY_ID;
  public static final Entity TYPE = Commons.singleIdType(true, "types.statusCategory", ID);
  public static final EntityKey<String> KEY = Commons.ENTITY_NAME;
  public static final EntityKey<String> NAME = Commons.ENTITY_DESCRIPTION;
  public static final EntityKey<String> COLOR_NAME = EntityKey.string("statusCategory.colorName", null);
}
