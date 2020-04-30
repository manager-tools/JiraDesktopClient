package com.almworks.items.entities.dbwrite.impl;

import com.almworks.integers.LongArray;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.items.entities.dbwrite.ResolutionException;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.items.wrapper.DatabaseUnwrapper;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

public class SearchDB implements ItemResolution {
  private final VersionSource mySource;
  private final long myConnectionItem;
  private final DBObjectsCache myCache;
  private final Set<Entity> mySearchStack;
  private final Resolver myResolver;

  private SearchDB(VersionSource source, long connectionItem, DBObjectsCache cache, Set<Entity> searchStack, Resolver resolver) {
    mySource = source;
    myConnectionItem = connectionItem;
    myCache = cache;
    mySearchStack = searchStack;
    myResolver = resolver;
  }

  public static SearchDB readonlyResolve(VersionSource source, long connectionItem, DBObjectsCache cache, @Nullable Set<Entity> stack, @Nullable Resolver resolver) {
    if (stack == null) stack = Collections15.hashSet();
    if (resolver == null) resolver = Resolver.create(Collections15.<Entity>emptyCollection());
    return new SearchDB(source, connectionItem, cache, stack, resolver);
  }

  public long getConnectionItem() {
    return myConnectionItem;
  }

  public long findExistingSafe(Entity entity) {
    try {
      return findExisting(entity);
    } catch (ResolutionException e) {
      return 0;
    }
  }

  @Override
  public long resolve(Entity entity) throws ResolutionException {
    return findExisting(entity);
  }

  public long findExisting(Entity entity) throws ResolutionException {
    if (entity == null) return 0;
    long item = myResolver.getResolution(entity);
    if (item > 0) return item;
    entity = myResolver.getGeneric(entity);
    Long itemId = entity.get(StoreBridge.ITEM_ID);
    if (itemId != null && itemId > 0) item = itemId;
    if (item <= 0) item = findDBObject(entity);
    if (item <= 0) {
      if (mySearchStack.contains(entity)) throw ResolutionException.error("Resolution loop", entity, mySearchStack);
      mySearchStack.add(entity);
      try {
        Entity type = entity.getType();
        if (type == null) throw ResolutionException.error("Missing type", entity);
        EntityResolution resolution = type.get(EntityResolution.KEY);
        if (resolution == null) throw ResolutionException.error("Missing resolution", entity);
        item = searchDB(resolution, entity);
      } finally {
        mySearchStack.remove(entity);
      }
    }
    if (item > 0) {
      myResolver.setResolution(entity, item);
      return item;
    }
    return 0;
  }

  private long findDBObject(Entity entity) throws ResolutionException {
    DBIdentifiedObject dbObj = myCache.toDBObject(entity);
    if (dbObj != null) {
      long item = mySource.findMaterialized(dbObj);
      if (item <= 0) throw ResolutionException.error("Not materialized", entity, dbObj);
      return item;
    }
    return 0;
  }

  private long searchDB(EntityResolution resolution, Entity entity) throws ResolutionException {
    boolean includeConnection = resolution.includeConnection();
    for (Collection<EntityKey<?>> keys : resolution.getIdentities()) {
      long item = searchDB(includeConnection, keys, entity, true);
      if (item > 0) return item;
    }
    for (Collection<EntityKey<?>> keys : resolution.getSearchBy()) {
      long item = searchDB(includeConnection, keys, entity, false);
      if (item > 0) return item;
    }
    return 0;
  }

  private long searchDB(boolean connection, Collection<EntityKey<?>> keys, Entity entity, boolean unique) throws ResolutionException {
    BoolExpr<DP> query;
    if (connection) {
      if (myConnectionItem <= 0) throw ResolutionException.insufficientData(entity, "Missing connection");
      query = DPEquals.create(SyncAttributes.CONNECTION, myConnectionItem);
    } else
      query = null;
    DBItemType type = myCache.getType(entity.getType());
    if (type == null) throw ResolutionException.error("Missing type", entity);
    query = and(query, DPEqualsIdentified.create(DBAttribute.TYPE, type));
    for (EntityKey<?> key : keys) {
      KeyCounterpart counterpart = myCache.getKeyCounterpart(key);
      if (counterpart == null) throw ResolutionException.error("No counterpart", key);
      BoolExpr<DP> q = counterpart.buildQuery(this, entity);
      if (q == null) return 0;
      query = and(query, q);
    }
    DBReader reader = mySource.getReader();
    DBQuery result = unique ? DatabaseUnwrapper.query(reader, query) : reader.query(query);
    LongArray items = result.copyItemsSorted();
    if (items.isEmpty()) return 0;
    if (items.size() < 2) return items.isEmpty() ? 0 : items.get(0);
    LogHelper.assertError(!unique, "Expected unique", EntityUtils.printValue(entity), items);
    LongArray alive = new LongArray();
    for (int i = 0; i < items.size(); i++) {
      long item = items.get(i);
      if (!SyncUtils.isRemoved(mySource.forItem(item))) alive.add(item);
    }
    if (alive.size() == 1) return alive.get(0);
    if (!unique && alive.isEmpty()) return 0;
    LogHelper.error("Unsure resolution", entity, keys, unique, items, alive);
    return (alive.isEmpty() ? items : alive).get(0);
  }

  private static BoolExpr<DP> and(BoolExpr<DP> q1, BoolExpr<DP> q2) {
    if (q1 == null || q2 == null) return q1 != null ? q1 : q2;
    return q1.and(q2);
  }

  public ItemVersion forItem(long item) {
    return mySource.forItem(item);
  }
}
