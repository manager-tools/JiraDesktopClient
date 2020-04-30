package com.almworks.items.entities.dbwrite.impl;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DP;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.items.entities.dbwrite.EntityWriteProcess;
import com.almworks.items.entities.dbwrite.ResolutionException;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

class WriteProcess implements EntityWriteProcess {
  private final DBDrain myDrain;
  private final DBObjectsCache myCache;

  public WriteProcess(DBDrain drain, DBObjectsCache cache) {
    myDrain = drain;
    myCache = cache;
  }

  @Override
  public DBAttribute<?> getAttribute(EntityKey<?> key) {
    return myCache.getAttribute(key.toEntity());
  }

  @NotNull
  @Override
  public VersionSource getVersionSource() {
    return myDrain;
  }

  public LongList query(BoolExpr<DP> query) {
    return myDrain.getReader().query(query).copyItemsSorted();
  }

  public long materialize(DBIdentifiedObject object) {
    return myDrain.materialize(object);
  }

  public long createItem(WriteResolution itemResolution, Entity entity) throws ResolutionException {
    Entity type = entity.getType();
    DBItemType dbType = getDBType(entity);
    if (dbType == null) throw ResolutionException.error("No DB type", type, entity);
    EntityResolution resolution = type.get(EntityResolution.KEY);
    if (resolution == null) throw ResolutionException.error("Missing resolution", entity);
    checkCanCreate(resolution, entity);
    ItemVersionCreator creator = myDrain.createItem();
    LogHelper.debug("Creating entity", creator.getItem(), EntityUtils.printValue(entity));
    creator.setValue(DBAttribute.TYPE, dbType);
    if (resolution.includeConnection()) creator.setValue(SyncAttributes.CONNECTION, itemResolution.getConnectionItem());
    writeResolutionValues(itemResolution, entity, resolution, creator);
    return creator.getItem();
  }

  private DBItemType getDBType(Entity entity) throws ResolutionException {
    Entity type = entity.getType();
    if (type == null || Entity.isMetaType(type)) throw ResolutionException.error("Wrong type", entity, type);
    return myCache.getType(type);
  }

  public void writeResolution(WriteResolution resolver, Entity entity, long item) throws ResolutionException {
    Entity type = entity.getType();
    if (type == null) throw ResolutionException.error("Missing type", entity);
    EntityResolution entityResolution = type.get(EntityResolution.KEY);
    if (entityResolution == null) throw ResolutionException.error("Missing resolution", entity);
    writeResolutionValues(resolver, entity, entityResolution, myDrain.changeItem(item));
  }

  private void writeResolutionValues(WriteResolution itemResolution, Entity entity, EntityResolution resolution, ItemVersionCreator creator)
    throws ResolutionException
  {
    for (EntityKey<?> key : resolution.getAllKeys()) {
      if (!entity.hasValue(key)) continue;
      KeyCounterpart counterpart = myCache.getKeyCounterpart(key);
      if (counterpart == null) throw ResolutionException.error("No counterpart", key);
      counterpart.copyValue(itemResolution, creator, entity);
    }
  }

  private void checkCanCreate(EntityResolution resolution, Entity entity) throws ResolutionException {
    Collection<Collection<EntityKey<?>>> identities = resolution.getIdentities();
    List<EntityKey<?>> missingKeys = Collections15.arrayList();
    for (Collection<EntityKey<?>> identity : identities) {
      EntityKey<?> missingKey = null;
      for (EntityKey<?> key : identity) {
        if (entity.get(key) == null) {
          missingKey = key;
          break;
        }
      }
      if (missingKey == null) return;
      if (!missingKeys.contains(missingKey)) missingKeys.add(missingKey);
    }
    throw ResolutionException.insufficientData(entity, "Missing one of " + missingKeys);
  }
}
