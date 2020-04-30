package com.almworks.items.entities.dbwrite;

import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.edit.SyncFixture;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.tests.BaseTestCase;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class StoreBridgeTests extends SyncFixture {
  private static final DBNamespace NS = new DBNamespace("test", "sample");
  private static final DBIdentifiedObject CONNECTION_OBJECT = NS.object("connection");

  public void testStoreObjects() throws ResolutionException, ExecutionException, InterruptedException {
    DBItemType type = NS.type("type", "The type");
    DBAttribute<String> strAttr = NS.string("str");
    DBIdentifiedObject obj = NS.object("obj", "The Object");

    EntityKey<String> strKey = StoreBridge.fromScalarAttribute(strAttr);
    Entity entityType = StoreBridge.buildFromType(type);
    entityType.put(EntityResolution.KEY, EntityResolution.singleIdentity(true, strKey));
    Entity entityObj = StoreBridge.fromDBObject(obj);
    EntityKey<Entity> linkKey = EntityKey.entity("link", null);

    Entity objCopy = Entity.copy(entityObj);
    objCopy.put(strKey, "abc");
    objCopy.put(linkKey, strKey.toEntity());
    objCopy.fix();

    Entity entity = new Entity(entityType);
    entity.put(strKey, "xyz");
    entity.put(linkKey, entityObj);

    writeEntities(entity, objCopy);

    long objItem = findMaterialized(obj);
    long entityItem = querySingle(DPEquals.create(strAttr, "xyz"));
    long typeItem = findMaterialized(type);
    long strAttrItem = findMaterialized(strAttr);
    DBAttribute<Long> linkAttr = StoreBridge.toLinkAttribute(NS, linkKey);
    assertTrue(objItem > 0);
    assertTrue(entityItem > 0);
    assertTrue(typeItem > 0);
    assertTrue(strAttrItem > 0);
    checkTrunk(objItem, strAttr, "abc");
    checkTrunk(objItem, linkAttr, strAttrItem);
    checkTrunk(entityItem, strAttr, "xyz");
    checkTrunk(entityItem, DBAttribute.TYPE, typeItem);
    checkTrunk(entityItem, linkAttr, objItem);
  }

  public void testItemId() throws ResolutionException, ExecutionException, InterruptedException {
    EntityKey<String> id = EntityKey.string("id", null);
    EntityKey<String> search = EntityKey.string("val", null);
    Entity type = Entity.buildType("type");
    type.put(EntityResolution.KEY, EntityResolution.searchable(true, Collections.singleton(search),  id));
    type.fix();

    Entity entity = new Entity(type);
    entity.put(id, "ID1");
    entity.put(search, "abc");
    entity.fix();

    writeEntities(entity);
    DBAttribute<String> attrId = StoreBridge.toScalarAttribute(NS, id);
    DBAttribute<String> attrSearch = StoreBridge.toScalarAttribute(NS, search);
    long item = querySingle(DPEquals.create(attrId, "ID1"));
    assertTrue(item > 0);
    checkTrunk(item, attrSearch, "abc");

    entity = StoreBridge.buildItem(type, item);
    entity.put(search, "def");
    entity.fix();

    writeEntities(entity);
    checkScalar(NS, item, search, "def");

    EntityKey<String> attr1 = EntityKey.string("attr1", null);
    EntityKey<String> attr2 = EntityKey.string("attr2", null);
    entity = StoreBridge.buildItem(type, item);
    entity.put(attr1, "123");
    entity.fix();
    Entity entity2 = new Entity(type);
    entity2.put(search, "def");
    entity2.put(attr2, "456");
    entity2.fix();

    writeEntities(entity, entity2);
    checkScalar(NS, item, search, "def");
    checkScalar(NS, item, attr1, "123");
    checkScalar(NS, item, attr2, "456");
  }

  /**
   * Entities identified by item should be writen to DB before creating other entities because of item identified entities
   * may add data and other entities can be found by the data
   */
  public void testWriteIdentifiedByItemFirst() throws ResolutionException, InterruptedException, ExecutionException {
    EntityKey<String> strKey = EntityKey.string("str", null);
    EntityKey<Integer> intKey = EntityKey.integer("id", null);
    DBAttribute<String> strAttr = StoreBridge.toScalarAttribute(NS, strKey);
    DBAttribute<Integer> intAttr = StoreBridge.toScalarAttribute(NS, intKey);
    Entity eType = Entity.buildType("type").put(EntityResolution.KEY, EntityResolution.singleIdentity(true, intKey, strKey)).fix();
    DBItemType type = StoreBridge.toDBType(eType, NS);

    DBIdentity itemId = new DBIdentity.Builder().put(DBAttribute.TYPE, type)
      .put(strAttr, "a")
      .put(intAttr, 1)
      .put(SyncAttributes.CONNECTION, CONNECTION_OBJECT)
      .create();
    long item = materialize(itemId);
    checkTrunk(item, strAttr, "a");
    checkTrunk(item, intAttr, 1);

    Entity e1 = new Entity(eType).put(intKey, 1).put(StoreBridge.ITEM_ID, item).fix();
    Entity e2 = new Entity(eType).put(intKey, 1).put(strKey, "a").fix();
    EntityTransaction transaction = new EntityTransaction();
    EntityBag2 bag = transaction.addBagScalar(eType, strKey, "a");
    bag.delete();
    bag.exclude(transaction.addEntity(e1));
    bag.exclude(transaction.addEntity(e2));
    writeTransaction(transaction, NS, CONNECTION_OBJECT);
    LongList items = query(DPEqualsIdentified.create(DBAttribute.TYPE, type));
    CHECK.size(1, items.toList());
  }

  protected void writeEntities(Entity... entities) throws ResolutionException {
    writeEntities(NS, CONNECTION_OBJECT, entities);
  }

  protected void writeEntities(final DBNamespace ns, final DBIdentifiedObject connection, Entity... entities) throws ResolutionException {
    final EntityTransaction transaction = new EntityTransaction();
    for (Entity entity : entities) {
      transaction.addEntity(entity);
    }
    writeTransaction(transaction, ns, connection);
  }

  private void writeTransaction(final EntityTransaction transaction, final DBNamespace ns, final DBIdentifiedObject connection) {
    myManager.writeDownloaded(new DownloadProcedure<DBDrain>() {
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        com.almworks.items.entities.api.collector.transaction.write.EntityWriter writer = transaction.prepareWrite(drain, ns, DBIdentity.fromDBObject(connection));
        writer.write();
      }

      @Override
      public void onFinished(DBResult<?> result) {
      }
    }).waitForCompletion();
  }

  protected <T> void checkScalar(DBNamespace ns, Long item, EntityKey<T> key, T expected)
    throws ExecutionException, InterruptedException
  {
    DBAttribute<T> attr = StoreBridge.toScalarAttribute(ns, key);
    T value = readAttribute(item, attr);
    BaseTestCase.assertEquals(expected, value);
  }
}
