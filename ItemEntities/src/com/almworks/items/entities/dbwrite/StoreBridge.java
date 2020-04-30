package com.almworks.items.entities.dbwrite;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.DataSequence;
import com.almworks.items.entities.dbwrite.impl.DBObjectsCache;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Utility methods to convert entities to ItemStorage objects and vise versa.
 */
public class StoreBridge {
  /**
   * Counterpart for {@link DBAttribute#ID}
   */
  public static final EntityKey<String> STORE_ID = EntityKey.hint("sys.store.key.storeId", String.class);
  /**
   * Counterpart for {@link DBAttribute#NAME}
   */
  public static final EntityKey<String> STORE_NAME = EntityKey.hint("sys.store.key.storeName", String.class);
  /**
   * Key to map entities to DB by item id value
   */
  public static final EntityKey<Long> ITEM_ID = EntityKey.hint("sys.store.key.storeItem", Long.class);
  /**
   * Stub type to represent no type (since this is allowed by ItemStorage)
   */
  public static final Entity NULL_TYPE = Entity.type("sys.store.type.nullType");

  public static final EntityKey<ItemProxy> ORIGINAL_OBJECT = EntityKey.hint("sys.store.key.originalObject", ItemProxy.class);

  public static final EntityKey<Integer> ITEM_DOWNLOAD_STAGE = fromScalarAttribute(SyncAttributes.ITEM_DOWNLOAD_STAGE);

  public static final EntityKey<DataSequence> UPLOAD_ATTEMPT;
  static {
    String id = SyncSchema.UPLOAD_ATTEMPT.getId();
    Entity description = EntityKey.buildKey();
    description.put(STORE_ID, id);
    description.put(ORIGINAL_OBJECT, DBIdentity.fromDBObject(SyncSchema.UPLOAD_ATTEMPT));
    String name = SyncSchema.UPLOAD_ATTEMPT.getName();
    if (name != null) description.put(STORE_NAME, name);
    UPLOAD_ATTEMPT = EntityKey.scalar(id, DataSequence.class, description);
  }

  /**
   * Counterpart for {@link SyncAttributes#CONNECTION}
   */
  public static final EntityKey<Entity> CONNECTION;
  static {
    Entity description = new Entity(EntityKey.typeKey());
    String name = SyncAttributes.CONNECTION.getName();
    description.put(STORE_ID, SyncAttributes.CONNECTION.getId());
    description.put(STORE_NAME, name);
    description.put(ORIGINAL_OBJECT, DBIdentity.fromDBObject(SyncAttributes.CONNECTION));
    CONNECTION = EntityKey.entity(name, description);
  }

  public static Entity buildFromDBObject(DBIdentifiedObject object) {
    if (object == null) return null;
    if (object instanceof DBAttribute) {
      EntityKey<?> key = fromAttribute((DBAttribute<?>) object);
      return key != null ? key.toEntity() : null;
    }
    if (object instanceof DBItemType) return buildFromType((DBItemType) object);
    String id = object.getId();
    String name = object.getName();
    if (id == null) return null;
    Entity entity = new Entity(NULL_TYPE);
    entity.put(STORE_ID, id);
    if (name != null) entity.put(STORE_NAME, name);
    entity.put(ORIGINAL_OBJECT, DBIdentity.fromDBObject(object));
    return entity;
  }

  public static Entity buildFromProxy(ItemProxy proxy) {
    if (proxy == null) return null;
    DBIdentifiedObject object = DBObjectsCache.extractIdentifiedObject(proxy);
    if (object != null) return buildFromDBObject(object);
    Entity entity = new Entity(NULL_TYPE);
    entity.put(ORIGINAL_OBJECT, proxy);
    return entity;
  }

  public static Entity fromDBObject(DBIdentifiedObject object) {
    return buildFromDBObject(object).fix();
  }

  public static <T> EntityKey<T> fromScalarAttribute(DBAttribute<T> attribute) {
    EntityKey<?> key = fromAttribute(attribute);
    if (key == null) return null;
    LogHelper.assertError(attribute.getComposition() == DBAttribute.ScalarComposition.SCALAR, "Wrong composition",
      attribute.getComposition());
    LogHelper.assertError(attribute.getScalarClass().equals(key.getValueClass()), "Wrong class", attribute.getScalarClass(), key);
    //noinspection unchecked
    return (EntityKey<T>) key;
  }

  public static EntityKey<?> fromAttribute(DBAttribute<?> attribute) {
    if (attribute == null) return null;
    if (DBAttribute.ID.equals(attribute)) return STORE_ID;
    if (DBAttribute.NAME.equals(attribute)) return STORE_NAME;
    if (DBAttribute.TYPE.equals(attribute)) return EntityKey.keyType();
    String id = attribute.getId();
    if (id == null) return null;
    String name = attribute.getName();
    Class<?> aClass = attribute.getScalarClass();
    DBAttribute.ScalarComposition composition = attribute.getComposition();
    Entity entity = EntityKey.buildKey();
    entity.put(STORE_ID, id);
    entity.put(ORIGINAL_OBJECT, DBIdentity.fromDBObject(attribute));
    if (name != null) entity.put(STORE_NAME, name);
    switch (composition) {
    case SCALAR: return EntityKey.scalar(id, aClass, entity);
    case SET: return EntityKey.collection(id, aClass, entity);
    case LIST:
      LogHelper.error("Not implemented", attribute);
      return null;
    default:
      LogHelper.error("Unknown composition", composition);
      return null;
    }
  }

  public static Entity buildFromType(DBItemType type) {
    if (type == null) return null;
    String id = type.getId();
    if (id == null) return null;
    Entity entity = Entity.buildType(id);
    entity.put(STORE_ID, id);
    String name = type.getName();
    if (name != null) entity.put(STORE_NAME, name);
    entity.put(ORIGINAL_OBJECT, DBIdentity.fromDBObject(type));
    return entity;
  }

  public static Entity buildItem(Entity type, long item) {
    Entity entity = new Entity(type);
    entity.put(StoreBridge.ITEM_ID, item);
    return entity;
  }

  public static DBItemType toDBType(Entity type, DBNamespace ns) {
    return DBObjectsCache.createType(type, ns);
  }

  public static DBAttribute<?> toAttribute(EntityKey<?> key, DBNamespace ns) {
    return DBObjectsCache.createAttribute(key.toEntity(), ns);
  }

  @SuppressWarnings( {"unchecked"})
  public static <T> DBAttribute<T> toScalarAttribute(DBNamespace ns, EntityKey<T> key) {
    return (DBAttribute<T>) toAttribute(key, ns);
  }

  @SuppressWarnings( {"unchecked"})
  public static DBAttribute<Long> toLinkAttribute(DBNamespace ns, EntityKey<Entity> key) {
    return (DBAttribute<Long>) toAttribute(key, ns);
  }

  @SuppressWarnings( {"unchecked"})
  public static DBAttribute<Set<Long>> toLinkSetAttribute(DBNamespace ns, EntityKey<? extends Collection<? extends Entity>> refKey) {
    return (DBAttribute<Set<Long>>) toAttribute(refKey, ns);
  }

  @SuppressWarnings( {"unchecked"})
  public static DBAttribute<List<Long>> toLinkListAttribute(DBNamespace ns, EntityKey<? extends List<? extends Entity>> refKey) {
    return (DBAttribute<List<Long>>) toAttribute(refKey, ns);
  }

  public static class Sequence {
    private final DataSequence.Builder myBuilder = new DataSequence.Builder();

    public Sequence append(Entity entity) {
      myBuilder.appendEntity(entity);
      return this;
    }

    public Sequence append(boolean bool) {
      myBuilder.appendBoolean(bool);
      return this;
    }

    public DataSequence create() {
      return myBuilder.create();
    }
  }
}
