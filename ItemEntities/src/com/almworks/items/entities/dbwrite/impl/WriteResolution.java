package com.almworks.items.entities.dbwrite.impl;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.dbwrite.CustomResolution;
import com.almworks.items.entities.dbwrite.ResolutionException;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

class WriteResolution {
  private final WriteProcess myProcess;
  private final Resolver myResolver;
  private final Set<Entity> mySearchStack = Collections15.hashSet();
  private final DBObjectsCache myCache;
  private final Entity myConnectionEntity;
  @Nullable
  private final HelperImpl myHelper;
  private SearchDB mySearchDB;

  WriteResolution(WriteProcess process, Entity connection, Resolver resolver, DBObjectsCache cache, @Nullable CustomResolution customResolution) {
    myProcess = process;
    myConnectionEntity = connection;
    myResolver = resolver;
    myCache = cache;
    myHelper = customResolution != null ? new HelperImpl(customResolution, myProcess.getVersionSource(), this) : null;
  }

  public long getConnectionItem() throws ResolutionException {
    long connection = getSearcher().getConnectionItem();
    if (connection <= 0) throw ResolutionException.error("Failed to resolve connection", myConnectionEntity);
    return connection;
  }

  public long findExisting(Entity entity) {
    if (entity == null) return 0;
    try {
      entity = myResolver.getGeneric(entity);
      long item = myResolver.getResolution(entity);
      if (item > 0) return item;
      Long itemId = entity.get(StoreBridge.ITEM_ID);
      if (itemId != null && itemId > 0) {
        item = itemId;
        myProcess.writeResolution(this, entity, item);
        myResolver.setResolution(entity, item);
        return item;
      }
      return getSearcher().findExisting(entity);
    } catch (ResolutionException e) {
      return 0;
    }
  }

  @NotNull
  private SearchDB getSearcher() throws ResolutionException {
    if (mySearchDB == null) {
      DBIdentifiedObject connectionObj = myCache.toDBObject(myConnectionEntity);
      long connection;
      if (connectionObj != null) {
        connection = myProcess.materialize(connectionObj);
      } else {
        SearchDB resolveConnection = SearchDB.readonlyResolve(myProcess.getVersionSource(), 0, myCache, mySearchStack, myResolver);
        connection = resolveConnection.findExisting(myConnectionEntity);
      }
      if (connection > 0) myResolver.setResolution(myConnectionEntity, connection);
      else {
        LogHelper.error("Cannot resolve connection", myConnectionEntity);
        throw ResolutionException.error("Cannot resolve connection", myConnectionEntity);
      }
      mySearchDB = SearchDB.readonlyResolve(myProcess.getVersionSource(), connection, myCache, mySearchStack, myResolver);
    }
    return mySearchDB;
  }

  /**
   * Finds existing item or creates new one. If item creation is not possible throws an exception
   * @param entity entity to resolve
   * @return positive found or created item
   * @throws com.almworks.items.entities.dbwrite.ResolutionException if item cannot be found and can not be created
   */
  public long resolve(Entity entity) throws ResolutionException {
    if (entity == null) return 0;
    long item = findExisting(entity);
    if (item > 0) return item;
    entity = myResolver.getGeneric(entity);
    DBIdentifiedObject dbObj = myCache.toDBObject(entity);
    if (dbObj != null) {
      item = myProcess.materialize(dbObj);
      if (item <= 0) throw ResolutionException.error("Cant materialize", entity, dbObj);
    } else {
      if (myHelper != null) {
        item = myHelper.customResolve(entity);
        if (item > 0) {
          myProcess.writeResolution(this, entity, item);
        }
      }
      if (item <= 0) item = myProcess.createItem(this, entity);
    }
    if (item > 0) {
      myResolver.setResolution(entity, item);
      return item;
    }
    return 0;
  }

  private static class HelperImpl implements CustomResolution.Helper {
    private static final TypedKey<Map<TypedKey<?>, Object>> CACHE_KEY = TypedKey.create("customResolutionCache");
    private final CustomResolution myResolution;
    private final VersionSource mySource;
    private final WriteResolution myResolver;

    public HelperImpl(CustomResolution resolution, VersionSource source, WriteResolution resolver) {
      myResolution = resolution;
      mySource = source;
      myResolver = resolver;
    }

    public long customResolve(Entity entity) {
      return entity == null ? 0 : myResolution.resolve(this, entity);
    }

    @Override
    @NotNull
    public ItemVersion getConnection() {
      long connection;
      try {
        connection = myResolver.getConnectionItem();
      } catch (ResolutionException e) {
        connection = 0;
      }
      return mySource.forItem(connection);
    }

    @Override
    public DBReader getReader() {
      return mySource.getReader();
    }

    @Override
    public long resolve(Entity entity) {
      try {
        return myResolver.resolve(entity);
      } catch (ResolutionException e) {
        return 0;
      }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <T> void putCachedValue(TypedKey<T> key, T value) {
      Map map = mySource.getReader().getTransactionCache();
      Map<TypedKey<?>, Object> cache = CACHE_KEY.getFrom(map);
      if (cache == null) {
        cache = Collections15.hashMap();
        CACHE_KEY.putTo(map, cache);
      }
      key.putTo(cache, value);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T getCachedValue(TypedKey<T> key) {
      Map map = mySource.getReader().getTransactionCache();
      Map<TypedKey<?>, Object> cache = CACHE_KEY.getFrom(map);
      if (cache == null) return null;
      return key.getFrom(cache);
    }
  }
}
