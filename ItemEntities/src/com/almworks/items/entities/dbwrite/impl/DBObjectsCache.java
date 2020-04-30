package com.almworks.items.entities.dbwrite.impl;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.DataSequence;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.items.entities.dbwrite.ResolutionException;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DBObjectsCache {
  private final Map<EntityKey<?>, KeyCounterpart> myCounterparts = Collections15.hashMap();
  private final Map<String, DBAttribute<?>> myAttributes = Collections15.hashMap();
  private final Map<String, DBItemType> myTypes = Collections15.hashMap();
  private final Map<String, DBIdentifiedObject> myObjects = Collections15.hashMap();
  private final DBNamespace myNS;

  public DBObjectsCache(DBNamespace NS) {
    myNS = NS;
    mapStdKeyCounterpart(EntityKey.keyType(), DBAttribute.TYPE, KeyDataKind.Link.GENERIC);
    mapStdKeyCounterpart(StoreBridge.CONNECTION, SyncAttributes.CONNECTION, KeyDataKind.Link.GENERIC);
    mapStdKeyCounterpart(StoreBridge.STORE_ID, DBAttribute.ID, KeyDataKind.Scalar.STRING);
    mapStdKeyCounterpart(StoreBridge.STORE_NAME, DBAttribute.NAME, KeyDataKind.Scalar.STRING);
    mapStdKeyCounterpart(StoreBridge.ITEM_DOWNLOAD_STAGE, SyncAttributes.ITEM_DOWNLOAD_STAGE, KeyDataKind.Scalar.INT);
  }

  public DBNamespace getNamespace() {
    return myNS;
  }

  private <K, D> void mapStdKeyCounterpart(EntityKey<K> key, DBAttribute<D> attr, KeyDataKind<K, D> kind) {
    myCounterparts.put(key, KeyCounterpart.create(key, attr, kind));
  }

  public DBIdentifiedObject getIdentifiedObject(Entity entity) {
    if (entity == null) return null;
    String storeId = entity.get(StoreBridge.STORE_ID);
    if (storeId == null) return null;
    DBIdentifiedObject obj = myObjects.get(storeId);
    if (obj != null) return obj;
    obj = createIdentifiedObject(entity);
    if (obj != null) myObjects.put(storeId, obj);
    return obj;
  }

  public DBItemType getType(Entity type) {
    if (type == null) return null;
    if (StoreBridge.NULL_TYPE.equals(type)) return null;
    DBIdentifiedObject obj = getIdentifiedObject(type);
    if (obj != null) return Util.castNullable(DBItemType.class, obj);
    String typeId = type.getTypeId();
    DBItemType itemType = myTypes.get(typeId);
    if (itemType != null) return itemType;
    itemType = createType(type, myNS);
    if (typeId != null && itemType != null) myTypes.put(typeId, itemType);
    return itemType;
  }

  public <T> KeyCounterpart<T, ?> getKeyCounterpart(EntityKey<T> key) {
    if (key == null) return null;
    @SuppressWarnings( {"unchecked"}) KeyCounterpart<T, ?>
    counterpart = myCounterparts.get(key);
    if (counterpart != null) return counterpart;
    counterpart = createCounterpart(key);
    if (counterpart == null) return null;
    myCounterparts.put(key, counterpart);
    return counterpart;
  }

  private <K> KeyCounterpart<K, ?> createCounterpart(EntityKey<K> key) {
    DBAttribute<?> attribute = getAttribute(key.toEntity());
    if (attribute == null) return null;
    KeyDataKind<K, ?> dataKind = chooseDataKind(key);
    if (dataKind == null) return null;
    //noinspection unchecked
    return KeyCounterpart.create(key, attribute, dataKind);
  }

  /**
   * Maps key to DB attribute if such mapping exists. Special and {@link EntityKey.Composition#HINT hints} may have no mapping.
   * @param key key to map
   * @return DB attribute or null if no mapping exists
   */
  public DBAttribute<?> getAttribute(Entity key) {
    if (key == null) return null;
    DBIdentifiedObject obj = getIdentifiedObject(key);
    if (obj != null) return Util.castNullable(DBAttribute.class, obj);
    String id = EntityKey.getId(key);
    if (id == null) return null;
    DBAttribute<?> attribute = myAttributes.get(id);
    if (attribute == null) {
      attribute = createAttribute(key, myNS);
      if (attribute == null) return null;
      myAttributes.put(id, attribute);
    }
    return attribute;
  }

  public static DBItemType createType(Entity type, DBNamespace ns) {
    if (type == null) return null;
    DBIdentifiedObject obj = createIdentifiedObject(type);
    if (obj != null) return Util.castNullable(DBItemType.class, obj);
    if (!(Entity.isMetaType(type.getType()))) {
      LogHelper.error("Wrong type", type);
      return null;
    }
    String typeId = type.getTypeId();
    if (typeId == null) {
      LogHelper.error("Missing type id", type);
      return null;
    }
    return ns.type(typeId, typeId);
  }

  @Nullable
  public static DBIdentifiedObject extractIdentifiedObject(ItemProxy proxy) {
    if (proxy == null) return null;
    DBIdentity identity = Util.castNullable(DBIdentity.class, proxy);
    if (identity == null) {
      DBStaticObject staticObject = Util.castNullable(DBStaticObject.class, proxy);
      if (staticObject == null) return null;
      identity = staticObject.getIdentity();
    }
    return DBIdentity.extractIdentifiedObject(identity);
  }

  public static DBIdentifiedObject createIdentifiedObject(Entity entity) {
    if (entity == null) return null;
    if (Entity.isMetaType(entity)) return DBItemType.TYPE;
    if (Entity.isKeyType(entity)) return DBItemType.ATTRIBUTE;
    if (Entity.isKeyType(entity.getType())) {
      String keyId = EntityKey.getId(entity);
      if (StoreBridge.STORE_ID.getId().equals(keyId)) return DBAttribute.ID;
      if (StoreBridge.STORE_NAME.getId().equals(keyId)) return DBAttribute.NAME;
      if (EntityKey.keyType().getId().equals(keyId)) return DBAttribute.TYPE;
    }
    DBIdentifiedObject object =  extractIdentifiedObject(entity.get(StoreBridge.ORIGINAL_OBJECT));
    if (object != null) return object;
    String storeId = entity.get(StoreBridge.STORE_ID);
    if (storeId == null) return null;
    String name = entity.get(StoreBridge.STORE_NAME);
    if (name == null) name = storeId;
    Entity type = entity.getType();
    if (Entity.isMetaType(type)) return new DBItemType(storeId, name);
    if (Entity.isKeyType(entity.getType())) return createAttribute(entity, storeId, name);
    DBIdentifiedObject obj = new DBIdentifiedObject(storeId, name);
    if (type != null && Entity.isMetaType(type.getType())) {
      DBIdentifiedObject dbType = Util.castNullable(DBItemType.class, createIdentifiedObject(type));
      if (dbType != null) obj.initialize(DBAttribute.TYPE, dbType);
    }
    return obj;
  }

  public static DBAttribute<?> createAttribute(Entity key, DBNamespace ns) {
    if (key == null) return null;
    EntityKey.Composition composition = EntityKey.getComposition(key);
    if (composition == EntityKey.Composition.HINT && StoreBridge.ITEM_ID.toEntity().equals(key)) return null;
    DBIdentifiedObject stdObject = createIdentifiedObject(key);
    if (stdObject != null) return Util.castNullable(DBAttribute.class, stdObject);
    CreateAttributeInfo info = createAttributeInfo(key, ns);
    return info != null ? info.createAttribute() : null;
  }

  public static CreateAttributeInfo createAttributeInfo(Entity key, DBNamespace ns) {
    if (key == null) return null;
    EntityKey.Composition composition = EntityKey.getComposition(key);
    if (composition == EntityKey.Composition.HINT && StoreBridge.ITEM_ID.toEntity().equals(key)) return null;
    if (composition == EntityKey.Composition.HINT) return null;
    String keyId = EntityKey.getId(key);
    if (keyId == null) return null;
    String id = ns.idAttribute(keyId);
    return CreateAttributeInfo.create(key, id, keyId);
  }

  private static DBAttribute<?> createAttribute(Entity key, String id, String name) {
    CreateAttributeInfo info = CreateAttributeInfo.create(key, id, name);
    return info != null ? info.createAttribute() : null;
  }

  private static final Map<Class<?>, KeyDataKind<?, ?>> SCALARS;
  static {
    SCALARS = Collections15.hashMap();
    for (Class<?> aClass : EntityKey.SCALAR_CLASSES) {
      SCALARS.put(aClass, KeyDataKind.Scalar.create(aClass));
    }
  }
  public static KeyDataKind<?, ?> chooseDataKind(Entity key) {
    if (key == null || !Entity.isKeyType(key.getType())) {
      LogHelper.error("Not a key", key);
      return null;
    }
    EntityKey.Composition composition = EntityKey.getComposition(key);
    Class<?> aClass = EntityKey.getValueClass(key);
    if (composition == EntityKey.Composition.SCALAR) {
      KeyDataKind<?, ?> behaviour = SCALARS.get(aClass);
      if (behaviour != null) return behaviour;
      if (Entity.class.equals(aClass)) return chooseLinkKind(key);
      if (DataSequence.class.equals(aClass)) return KeyDataKind.Sequence.INSTANCE;
      if (byte[].class == aClass) return KeyDataKind.RawBytes.INSTANCE;
      LogHelper.error("Unknown class", aClass, key);
      return null;
    } else if (composition == EntityKey.Composition.COLLECTION || composition == EntityKey.Composition.ORDER) {
      return collectionKind(key);
    } else {
      LogHelper.error("Unknown composition", composition, key);
      return null;
    }
  }

  private static KeyDataKind<?, ?> collectionKind(Entity key) {
    if (key == null) return null;
    EntityKey.Composition composition = EntityKey.getComposition(key);
    Class<?> aClass = EntityKey.getValueClass(key);
    if (!Entity.class.equals(aClass)) {
      LogHelper.error("Unknown collection class", aClass, composition, key);
      return null;
    }
    boolean emptyList = Boolean.TRUE.equals(key.get(EntityUtils.EMPTY_LIST_HINT));
    if (EntityKey.Composition.ORDER == composition) return emptyList ? KeyDataKind.LinkCollection.EMPTY_ORDER : KeyDataKind.LinkCollection.ORDER;
    else if (EntityKey.Composition.COLLECTION == composition) return emptyList ? KeyDataKind.LinkCollection.EMPTY_COLLECTION : KeyDataKind.LinkCollection.COLLECTION;
    LogHelper.error("Unknown composition", composition, key);
    return null;
  }

  private static KeyDataKind.Link chooseLinkKind(Entity key) {
    int kind = EntityKeyProperties.getLinkKind(key);
    switch (kind) {
    case EntityKeyProperties.LINK_KIND_GENERIC: return KeyDataKind.Link.GENERIC;
    case EntityKeyProperties.LINK_KIND_PROPAGATE: return KeyDataKind.Link.PROPAGATE;
    case EntityKeyProperties.LINK_KIND_MASTER: return KeyDataKind.Link.MASTER;
    default:
      LogHelper.error("Unknown link kind", kind, key);
      return KeyDataKind.Link.GENERIC;
    }
  }

  static <K> KeyDataKind<K, ?> chooseDataKind(EntityKey<K> key) {
    //noinspection unchecked
    return (KeyDataKind<K, ?>) chooseDataKind(key.toEntity());
  }

  public DBIdentifiedObject toDBObject(Entity entity) throws ResolutionException {
    if (entity == null) return null;
    DBIdentifiedObject obj = getIdentifiedObject(entity);
    if (obj != null) return obj;
    Entity type = entity.getType();
    if (Entity.isMetaType(type)) {
      DBItemType dbType = getType(entity);
      if (dbType == null) throw ResolutionException.error("No type for", entity);
      return dbType;
    }
    if (Entity.isKeyType(type)) {
      DBAttribute<?> attribute = getAttribute(entity);
      if (attribute == null) throw ResolutionException.error("No attribute for", entity);
      return attribute;
    }
    return null;
  }
}
