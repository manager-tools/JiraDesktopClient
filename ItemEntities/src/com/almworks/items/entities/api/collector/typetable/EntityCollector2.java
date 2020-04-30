package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.sync.ItemProxy;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityCollector2 {
  /**
   * Types are identified by String Entity type ID
   */
  private final Map<String, GenericTable> myTablesByType = Collections15.hashMap();
  private final KeyResolution myKeys = new KeyResolution(this);
  private final TypeResolution myTypes = new TypeResolution(this);
  private final IdentifiedObjectTable myObjectTable = new IdentifiedObjectTable(this);
  private final List<Entity> myIdentifyStack = Collections15.arrayList();
  private int myIndexVersion = 0;

  public EntityPlace addEntity(Entity entity) {
    HashMap<Entity, EntityPlace> identified = identifyAll(entity);
    ValueRow row = new ValueRow(this);
    for (Map.Entry<Entity, EntityPlace> entry : identified.entrySet()) {
      Entity e = entry.getKey();
      EntityPlace place = entry.getValue();
      row.clear();
      for (EntityKey<?> key : e.getValueKeys()) {
        KeyInfo info = getOrCreateKey(key);
        if (info == null) continue;
        Object value = info.getCellValue(e, this);
        if (value == null) continue;
        row.addColumn(info, value);
      }
      place.setValues(row);
    }
    return identified.get(entity);
  }

  @Nullable
  public EntityPlace identify(Entity entity) {
    if (entity == null) return null;
    if (myIdentifyStack.contains(entity)) {
      LogHelper.error("Cycle identity", entity, myIdentifyStack);
      return null;
    }
    Entity type = entity.getType();
    if (type == null) {
      LogHelper.error("Missing type", entity);
      return null;
    }
    EntityPlace special = identifySpecial(entity);
    if (special != null) return special;
    GenericTable table = getTable(type);
    if (table == null) return null;
    myIdentifyStack.add(entity);
    try {
      return table.identify(entity);
    } finally {
      Entity removed = myIdentifyStack.remove(myIdentifyStack.size() - 1);
      LogHelper.assertError(removed == entity, "Identify stack broken", entity, removed, myIdentifyStack);
    }
  }

  private EntityPlace identifySpecial(Entity entity) {
    Entity type = entity.getType();
    if (Entity.isKeyType(type)) return myKeys.getOrCreatePlace(entity);
    if (Entity.isMetaType(type)) return myTypes.getOrCreatePlace(entity);
    if (entity.getType() == StoreBridge.NULL_TYPE) {
      ItemProxy dbObject = entity.get(StoreBridge.ORIGINAL_OBJECT);
      if (dbObject == null) {
        LogHelper.error("Not supported", entity);
        return null;
      }
      return myObjectTable.identify(dbObject);
    }
    return null;
  }

  @Nullable
  public EntityPlace addEntityRow(Entity type, ValueRow entityRow) {
    GenericTable table = getTable(type);
    if (table == null) return null;
    EntityPlace place = table.identify(entityRow);
    if (place == null) return null;
    table.setValues(place, entityRow);
    return place;
  }

  @Nullable
  public EntityPlace find(Entity type, ValueRow entityRow) {
    GenericTable table = getTable(type);
    if (table == null) return null;
    return table.tryFind(entityRow);
  }

  public EntityPlace addIdentifiedObject(ItemProxy object) {
    return myObjectTable.identify(object);
  }

  public void mergeIdentities() {
    if (!myIdentifyStack.isEmpty()) {
      LogHelper.error("Not stable state", myIdentifyStack);
      return;
    }
    int indexVersion;
    do {
      indexVersion = myIndexVersion;
      for (GenericTable table : myTablesByType.values()) table.validateIdentities();
    } while (indexVersion != myIndexVersion);
  }

  private HashMap<Entity, EntityPlace> identifyAll(Entity entity) {
    ArrayList<Entity> all = Collections15.arrayList();
    collectAllEntities(entity, all);
    HashMap<Entity, EntityPlace> identified = Collections15.hashMap();
    for (Entity e : all) {
      EntityPlace place = identify(e);
      if (place != null) identified.put(e, place);
      else LogHelper.error("Failed to identify", EntityUtils.printValue(e));
    }
    return identified;
  }

  private void collectAllEntities(Entity entity, ArrayList<Entity> target) {
    if (entity == null) return;
    entity.fix();
    if (target.contains(entity)) return;
    target.add(entity);
    Collection<EntityKey<?>> keys = entity.getValueKeys();
    for (EntityKey<?> key : keys) {
      if (key.getValueClass() != Entity.class) continue;
      if (key == EntityKey.keyType()) continue;
      EntityKey<Entity> eKey = EntityKey.castScalar(key, Entity.class);
      if (eKey != null) {
        collectAllEntities(entity.get(eKey), target);
        continue;
      }
      EntityKey<? extends Collection<Entity>> mKey = EntityKey.castMulti(key, Entity.class);
      if (mKey != null) for (Entity e : Util.NN(entity.get(mKey), Collections.<Entity>emptyList())) collectAllEntities(e, target);
      else LogHelper.error("Unknown key", key);
    }
  }

  @Nullable
  public GenericTable getTable(Entity type) {
    String typeId = checkTableType(type);
    if (typeId == null) return null;
    GenericTable table = myTablesByType.get(typeId);
    if (table != null) return table;
    table = GenericTable.create(this, type);
    if (table == null) return null;
    myTablesByType.put(typeId, table);
    return table;
  }

  @Nullable
  public GenericTable getTableIfExists(Entity type) {
    String typeId = checkTableType(type);
    return typeId != null ? myTablesByType.get(typeId) : null;
  }

  private String checkTableType(Entity type) {
    if (type == null || StoreBridge.NULL_TYPE == type)
      return null;
    if (!Entity.isMetaType(type.getType())) {
      LogHelper.error("No an entity type", type);
      return null;
    }
    String typeId = type.getTypeId();
    if (typeId == null) {
      LogHelper.error("Missing typeId", type);
      return null;
    }
    if (Entity.isKeyType(type) || Entity.isMetaType(type)) {
      LogHelper.error("Not a generic type", type);
      return null;
    }
    return typeId;
  }

  public int incIndexVersion() {
    myIndexVersion++;
    return myIndexVersion;
  }

  public int getIndexVersion() {
    return myIndexVersion;
  }

  public Collection<EntityTable> getAllTables() {
    ArrayList<EntityTable> tables = Collections15.<EntityTable>arrayList(myTablesByType.values());
    tables.add(myKeys);
    tables.add(myTypes);
    tables.add(myObjectTable);
    return tables;
  }

  public KeyInfo getKnownKeyInfo(EntityKey<?> key) {
    return myKeys.getKnownKeyInfo(key);
  }
  
  public KeyInfo getOrCreateKey(EntityKey<?> key) {
    KeyInfo info = myKeys.getOrCreateKey(key);
    if (info != null) return info;
    LogHelper.error("Cannot create key info", key);
    return null;
  }

  public KeyInfo.IdKeyInfo getOrCreateIdKey(EntityKey<?> key) {
    KeyInfo info = getOrCreateKey(key);
    if (info == null) return null;
    KeyInfo.IdKeyInfo idInfo = Util.castNullable(KeyInfo.IdKeyInfo.class, info);
    if (idInfo == null) {
      LogHelper.error("Can not create id key", info);
      return null;
    }
    return idInfo;
  }
}
