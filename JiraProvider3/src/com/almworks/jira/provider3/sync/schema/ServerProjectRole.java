package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;

import java.util.Arrays;

public class ServerProjectRole {
  public static final EntityKey<Integer> ID = Commons.ENTITY_ID;
  public static final EntityKey<String> NAME = Commons.ENTITY_NAME;
  public static final EntityKey<String> DESCRIPTION = Commons.ENTITY_DESCRIPTION;
//  public static final EntityKey<Entity> PROJECT = Commons.ENTITY_PROJECT;

  // Jira Comment Visibility option (true is for "Project Roles Only", false is for "Groups & Project Roles")
  public static final EntityKey<Boolean> PROJECT_ROLES_ONLY = EntityKey.bool("connection.rolesOnly", null);

  public static final Entity TYPE;
  static {
    TYPE = Entity.buildType("types.projectRole");
    TYPE.put(EntityResolution.KEY, EntityResolution.searchable(true, Arrays.asList(NAME), ID/*, PROJECT*/));
    TYPE.fix();
  }
}
