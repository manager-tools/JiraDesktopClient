package com.almworks.items.entities.api;

class EntityInit {
  public static final EntityKey<Class<?>> CLASS;
  public static final EntityKey<EntityKey.Composition> COMPOSITION;
  public static final EntityKey<String> ID;
  public static final EntityKey<Entity> TYPE;
  public static final EntityKey<String> TYPE_ID;
  public static final Entity META_TYPE;
  public static final Entity T_KEY;
  public static final boolean INIT_DONE;

  static {
    Entity keyClass = new Entity(1);
    Entity keyComposition = new Entity(2);
    Entity keyId = new Entity(3);
    Entity keyType = new Entity(4);
    Entity keyTypeId = new Entity(5);
    CLASS = new EntityKey<Class<?>>(keyClass);
    COMPOSITION = new EntityKey<EntityKey.Composition>(keyComposition);
    ID = new EntityKey<String>(keyId);
    TYPE = new EntityKey<Entity>(keyType);
    TYPE_ID = new EntityKey<String>(keyTypeId);
    META_TYPE = new Entity(6);
    META_TYPE.priPut(TYPE, META_TYPE);
    META_TYPE.priPut(TYPE_ID, "sys.api.type.metaType");
    T_KEY = new Entity(7);
    T_KEY.put(TYPE, META_TYPE);
    T_KEY.put(TYPE_ID, "sys.api.type.key");
    initKey(keyClass, "sys.api.key.class", Class.class);
    initKey(keyComposition, "sys.api.key.composition",  EntityKey.Composition.class);
    initKey(keyId, "sys.api.key.id", String.class);
    initKey(keyType, "sys.api.key.type",  Entity.class);
    initKey(keyTypeId, "sys.api.key.typeId", String.class);
    META_TYPE.fix();
    T_KEY.fix();
    keyClass.fix();
    keyComposition.fix();
    keyId.fix();
    keyType.fix();
    keyTypeId.fix();
    INIT_DONE = true;
  }

  private static void initKey(Entity keyEntity, String id, Class<?> clazz) {
    keyEntity.put(ID, id);
    keyEntity.put(COMPOSITION, EntityKey.Composition.HINT);
    keyEntity.put(CLASS, clazz);
    keyEntity.put(TYPE, T_KEY);
  }
}
