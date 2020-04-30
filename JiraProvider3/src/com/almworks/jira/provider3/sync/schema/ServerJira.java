package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.entities.dbwrite.impl.CreateAttributeInfo;
import com.almworks.items.entities.dbwrite.impl.DBObjectsCache;
import com.almworks.items.util.DBNamespace;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ServerJira {
  public static final DBNamespace NS = DBNamespace.moduleNs("jira");

  public static DBAttribute<Long> toLinkAttribute(EntityKey<Entity> key) {
    return StoreBridge.toLinkAttribute(NS, key);
  }

  public static <T> DBAttribute<T> toScalarAttribute(EntityKey<T> key) {
    return StoreBridge.toScalarAttribute(NS, key);
  }

  public static DBItemType toItemType(Entity type) {
    return StoreBridge.toDBType(type, NS);
  }

  public static DBAttribute<Set<Long>> toLinkSetAttribute(EntityKey<Collection<Entity>> key) {
    return StoreBridge.toLinkSetAttribute(NS, key);
  }

  public static DBAttribute<List<Long>> toLinkListAttribute(EntityKey<List<Entity>> key) {
    return StoreBridge.toLinkListAttribute(NS, key);
  }

  public static CreateAttributeInfo createAttributeInfo(EntityKey<?> key) {
    return DBObjectsCache.createAttributeInfo(key.toEntity(), NS);
  }
}
