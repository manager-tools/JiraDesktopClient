package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.entities.dbwrite.impl.CreateAttributeInfo;
import com.almworks.items.entities.dbwrite.impl.DBObjectsCache;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;

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

  /**
   * Converts DB type item to Entity. This method reverses {@link #toItemType(Entity)}.<br>
   * NOTE! A DB type has no {@link EntityResolution}. So you cannot create new Entities of this type with result of this method.
   * However, you may use the type Entity as value.
   */
  public static Entity dbTypeToEntity(DBItemType dbType) {
    if (dbType == null) return null;
    String id = dbType.getId();
    if (id == null) return null;
    Pair<String, String> reversed = NS.reverseFullId(id);
    if (reversed == null) {
      LogHelper.error("Wrong type ID", id);
      return null;
    }
    LogHelper.assertError("t".equals(reversed.getFirst()), "Wrong DB type ID:", id);
    Entity entity = Entity.buildType(reversed.getSecond());
    entity.put(StoreBridge.STORE_ID, id);
    String name = dbType.getName();
    if (name != null) entity.put(StoreBridge.STORE_NAME, name);
    entity.put(StoreBridge.ORIGINAL_OBJECT, DBIdentity.fromDBObject(dbType));
    return entity;
  }
}
