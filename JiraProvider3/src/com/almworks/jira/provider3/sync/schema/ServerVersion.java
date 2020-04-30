package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;

import java.util.Date;

public class ServerVersion {
  public static final EntityKey<Integer> ID = Commons.ENTITY_ID;
  public static final EntityKey<String> NAME = Commons.ENTITY_NAME;
  /**
   * Version project
   */
  public static final EntityKey<Entity> PROJECT = Commons.ENTITY_PROJECT;
  /**
   * Planned release date
   */
  public static final EntityKey<Date> RELEASE_DATE = EntityKey.date("version.released", null);
  /**
   * The version is already released
   */
  public static final EntityKey<Boolean> RELEASED = EntityKey.bool("version.wasReleased", null);
  /**
   * The version is moved to archive
   */
  public static final EntityKey<Boolean> ARCHIVED = EntityKey.bool("version.archived", null);
  public static final EntityKey<Integer> SEQUENCE = Commons.ENTITY_ORDER;
  public static final Entity TYPE;

  static {
    Entity type = Entity.buildType("types.version");
    type.put(EntityResolution.KEY, EntityResolution.singleIdentity(true, ID, PROJECT));
    type.fix();
    TYPE = type;
  }
}
