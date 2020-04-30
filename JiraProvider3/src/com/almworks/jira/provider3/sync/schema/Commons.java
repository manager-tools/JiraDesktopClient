package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.util.LogHelper;

import java.util.Collection;

public class Commons {
  public static final EntityKey<Integer> ENTITY_ID = EntityKey.integer("entity.id", null);
  public static final EntityKey<String> ENTITY_NAME = EntityKey.string("entity.name", null);
  public static final EntityKey<String> ENTITY_DESCRIPTION = EntityKey.string("entity.description", null);
  public static final EntityKey<String> ICON_URL = StoreBridge.fromScalarAttribute(FieldInfo.ICON_URL);
  public static final EntityKey<Integer> ENTITY_ORDER = EntityKey.integer("entity.order", null);
  public static final EntityKey<Entity> ENTITY_PARENT = EntityKey.entity("entity.parent", null);
  public static final EntityKey<Entity> ENTITY_PROJECT = EntityKey.entity("entity.project", null);
  public static final EntityKey<Entity> ENTITY_CATEGORY = EntityKey.entity("entity.category", null);
  public static final EntityKey<Collection<Entity>> ONLY_IN_PROJECTS = EntityKey.entityCollection("entity.onlyProjects", null);
  public static final EntityKey<Collection<Entity>> ONLY_IN_ISSUE_TYPES = EntityKey.entityCollection("entity.onlyIssueTypes", null);

  public static Entity singleIdType(boolean includeConnection, String typeId, EntityKey<?> key) {
    Entity type = Entity.buildType(typeId);
    type.put(EntityResolution.KEY, EntityResolution.singleIdentity(includeConnection, key));
    type.fix();
    return type;
  }

  public static Integer parseInt(String strNumber) {
    if (strNumber == null) return null;
    strNumber = strNumber.trim();
    if (strNumber.length() == 0) return null;
    try {
      return Integer.parseInt(strNumber);
    } catch (NumberFormatException e) {
      LogHelper.error("expected int", strNumber);
      return null;
    }
  }
}
