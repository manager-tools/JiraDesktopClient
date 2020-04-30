package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;

import java.util.Collection;

public class ServerStatus {
  public static final EntityKey<Integer> ID = Commons.ENTITY_ID;
  public static final EntityKey<String> NAME = Commons.ENTITY_NAME;
  public static final EntityKey<String> DESCRIPTION = Commons.ENTITY_DESCRIPTION;
  public static final Entity TYPE = Commons.singleIdType(true, "types.status", ID);
  public static final EntityKey<Integer> ORDER = Commons.ENTITY_ORDER;
  public static final EntityKey<Collection<Entity>> ONLY_IN_PROJECTS = Commons.ONLY_IN_PROJECTS;
  public static final EntityKey<String> ICON_URL = Commons.ICON_URL;
  public static final EntityKey<Entity> CATEGORY = Commons.ENTITY_CATEGORY;

  public static Entity create(Integer id, String name) {
    if (id == null) return null;
    Entity status = new Entity(TYPE).put(ID, id);
    if (name != null) status.put(NAME, name);
    return status;
  }

  public static String getDisplayName(Entity status) {
    if (status == null) return "<null>";
    String name = status.get(NAME);
    if (name == null) name = String.valueOf(status.get(ID));
    return name;
  }

  public static String getDisplayName(EntityHolder status) {
    if (status == null) return "<null>";
    String name = status.getScalarValue(NAME);
    if (name == null) name = String.valueOf(status.getScalarValue(ID));
    return name;
  }
}
