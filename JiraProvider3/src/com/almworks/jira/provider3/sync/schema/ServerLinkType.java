package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import org.jetbrains.annotations.Nullable;

public class ServerLinkType {
  public static final EntityKey<Integer> ID = Commons.ENTITY_ID;
  public static final Entity TYPE = Commons.singleIdType(true, "types.linkType", ID);
  public static final EntityKey<String> NAME = Commons.ENTITY_NAME;
  public static final EntityKey<String> INWARD_DESCRIPTION = EntityKey.string("linkType.inwardDescription", null);
  public static final EntityKey<String> OUTWARD_DESCRIPTION = EntityKey.string("linkType.outwardDescription", null);

  public static Entity create(int id, @Nullable String name, @Nullable String inName, @Nullable String outName) {
    return new Entity(TYPE).put(ID, id)
      .putIfNotNull(NAME, name)
      .putIfNotNull(INWARD_DESCRIPTION, inName)
      .putIfNotNull(OUTWARD_DESCRIPTION, outName);
  }
}
