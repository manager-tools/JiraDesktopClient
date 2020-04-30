package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityResolution;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ServerProject {
  public static final EntityKey<Integer> ID = Commons.ENTITY_ID;
  public static final EntityKey<String> KEY = EntityKey.string("project.key", null);
  public static final EntityKey<String> NAME = Commons.ENTITY_NAME;
  public static final EntityKey<String> DESCRIPTION = Commons.ENTITY_DESCRIPTION;
  public static final EntityKey<String> PROJECT_URL = EntityKey.string("project.projectUrl", null);
  public static final EntityKey<String> URL = EntityKey.string("project.url", null);
  public static final EntityKey<Entity> LEAD = EntityKey.entity("project.lead", null);
  public static final Entity TYPE;

  static {
    TYPE = Entity.buildType("types.project");
    TYPE.put(EntityResolution.KEY, EntityResolution.searchable(true, Arrays.asList(KEY), ServerProject.ID));
    TYPE.fix();
  }

  public static Entity project(int prjId) {
    Entity entity = new Entity(TYPE);
    entity.put(ID, prjId);
    entity.fix();
    return entity;
  }

  @Nullable
  public static EntityHolder project(EntityTransaction transaction, int id) {
    return transaction.addEntity(TYPE, ID, id);
  }
}
