package com.almworks.items.entities.api.collector.transaction;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.entities.api.collector.typetable.EntityCollector2;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EntityTransaction {
  private final EntityCollector2 myCollector = new EntityCollector2();
  private final List<EntityBag2> myBags = Collections15.arrayList();
  private final UserDataHolder myUserData = new UserDataHolder();
  private final List<Procedure<EntityWriter>> myPostWriteProcedures = Collections15.arrayList();

  public EntityTransaction() {
  }

  public void addPostWriteProcedure(Procedure<EntityWriter> procedure) {
    myPostWriteProcedures.add(procedure);
  }

  public List<Procedure<EntityWriter>> getPostWriteProcedures() {
    return Collections15.unmodifiableListCopy(myPostWriteProcedures);
  }

  @Nullable
  public <T> EntityHolder addEntity(Entity type, EntityKey<T> id, T idValue) {
    if (idValue == null) return null;
    return addEntity(new Entity(type).put(id, idValue).fix());
  }

  public EntityHolder addEntityByItem(Entity type, long item) {
    if (item <= 0) return null;
    return addEntity(new Entity(type).put(StoreBridge.ITEM_ID, item).fix());
  }

  public EntityHolder addEntity(Entity entity) {
    if (entity == null) return null;
    EntityPlace place = myCollector.addEntity(entity);
    if (place == null) {
      LogHelper.error("Cannot identify", EntityUtils.printValue(entity));
      return null;
    }
    return new EntityHolder(this, place);
  }

  public EntityHolder addIdentifiedObject(DBIdentifiedObject object) {
    if (object == null) return null;
    return addIdentifiedObject(DBIdentity.fromDBObject(object));
  }

  public EntityHolder addIdentifiedObject(ItemProxy object) {
    EntityPlace place = myCollector.addIdentifiedObject(object);
    return place != null ? new EntityHolder(this, place) : null;
  }
  
  @Nullable
  public EntityHolder addEntityRef(Entity type, EntityKey<Entity> refId, EntityHolder idValue) {
    if (idValue == null) return null;
    EntityTable table = myCollector.getTable(type);
    ValueRow row = new ValueRow(myCollector);
    EntityQuery2.setReference(table, row, refId, idValue);
    EntityPlace place = myCollector.addEntityRow(type, row);
    if (place == null) {
      LogHelper.error("Failed to identify", type, refId, idValue);
      return null;
    }
    return new EntityHolder(this, place);
  }

  @Nullable
  public <T> EntityHolder addEntity(Entity type, EntityKey<Entity> refId, EntityHolder refValue, EntityKey<T> keyId, T keyValue) {
    if (refValue == null || keyValue == null || type == null) return null;
    IdentityBuilder builder = buildEntity(type);
    if (builder == null) return null;
    builder.addReference(refId, refValue);
    builder.addValue(keyId, keyValue);
    return builder.create();
  }

  @Nullable
  public IdentityBuilder buildEntity(Entity type) {
    EntityTable table = myCollector.getTable(type);
    if (table == null) return null;
    return new IdentityBuilder(type, table);
  }

  // todo unite bags by query
  private EntityQuery2 createQuery(Entity type) {
    EntityTable table = myCollector.getTable(type);
    if (table == null) return null;
    return new EntityQuery2(table);
  }

  private EntityBag2 addBag(EntityQuery2 query) {
    EntityBag2 bag = new EntityBag2(this, query);
    myBags.add(bag);
    return bag;
  }

  @NotNull
  public List<EntityBag2> getBags() {
    return Collections.unmodifiableList(myBags);
  }

  public EntityBag2 addBag(Entity type) {
    return addBag(createQuery(type));
  }

  public EntityBag2 addBagRef(Entity type, EntityKey<Entity> constraintKey, EntityHolder constraintValue) {
    EntityQuery2 query = createQuery(type);
    query.addRefConstraint(constraintKey, constraintValue);
    return addBag(query);
  }
  
  public <T> EntityBag2 addBagScalar(Entity type, EntityKey<T> key, T value) {
    EntityQuery2 query = createQuery(type);
    query.addConstraint(key, value);
    return addBag(query);
  }

  public UserDataHolder getUserData() {
    return myUserData;
  }

  @NotNull
  public EntityWriter prepareWrite(DBDrain drain, DBNamespace namespace, DBIdentity connectionIdentity) {
    long connection = drain.materialize(connectionIdentity);
    return new EntityWriter(drain, this, myBags, namespace, connection);
  }

  @NotNull
  public List<EntityHolder> getAllEntities(Entity type) {
    EntityTable table = myCollector.getTableIfExists(type);
    if (table == null) return Collections.emptyList();
    Collection<EntityPlace> places = table.getAllPlaces();
    List<EntityHolder> result = Collections15.arrayList(places.size());
    int index = 0;
    for (EntityPlace place : places) {
      if (place != null) result.add(new EntityHolder(this, place));
      else LogHelper.error("Missing place at", index, type);
      index++;
    }
    return result;
  }

  public Collection<EntityTable> getAllTables() {
    return myCollector.getAllTables();
  }

  public int getIndexVersion() {
    return myCollector.getIndexVersion();
  }

  public List<EntityHolder> addAllEntities(List<Entity> entities) {
    if (entities == null || entities.isEmpty()) return Collections.emptyList();
    ArrayList<EntityHolder> result = Collections15.arrayList();
    for (Entity entity : entities) result.add(addEntity(entity));
    return result;
  }

  public class IdentityBuilder {
    private final ValueRow myRow = new ValueRow(myCollector);
    private final Entity myType;
    private final EntityTable myTable;

    public IdentityBuilder(Entity type, EntityTable table) {
      myType = type;
      myTable = table;
    }
    
    public <T> IdentityBuilder addValue(EntityKey<T> key, T value) {
      EntityQuery2.setValue(myTable, myRow, key, value);
      return this;
    }

    public <T> IdentityBuilder addNNValue(EntityKey<T> key, T value) {
      if (value != null) addValue(key, value);
      return this;
    }
    
    public IdentityBuilder addReference(EntityKey<Entity> key, EntityHolder value) {
      EntityQuery2.setReference(myTable, myRow, key, value);
      return this;
    }

    public EntityTransaction getTransaction() {
      return EntityTransaction.this;
    }
    
    @Nullable
    public EntityHolder find() {
      EntityPlace place = myCollector.find(myType, myRow);
      return place != null ? new EntityHolder(EntityTransaction.this, place) : null; 
    }
    
    @Nullable
    public EntityHolder create() {
      EntityPlace place = myCollector.addEntityRow(myType, myRow);
      if (place == null) {
        LogHelper.error("Cannot identify", myType, myRow);
        return null;
      }
      return new EntityHolder(EntityTransaction.this, place);
    }

    public IdentityBuilder copy(Entity entity) {
      for (EntityKey<?> key : entity.getValueKeys()) copyKeyValue(key, entity);
      return this;
    }

    private <T> void copyKeyValue(EntityKey<T> key, Entity source) {
      if (!source.hasValue(key)) return;
      T value = source.get(key);
      addValue(key, value);
    }
  }
}
