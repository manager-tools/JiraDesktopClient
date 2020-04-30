package com.almworks.items.entities.api.collector.transaction;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.Collector2TestConsts;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Util;

import java.util.Collections;
import java.util.List;

public class EntityHolderTests extends BaseTestCase implements Collector2TestConsts {

  private EntityTransaction myTransaction;

  protected void setUp() throws Exception {
    super.setUp();
    myTransaction = new EntityTransaction();
  }

  public void testRestore() {
    EntityHolder h1 = add1("a", "b");
    EntityHolder h2 = add2(h1);
    DownloadStageMark.QUICK.setTo(h2);
    h2.setValue(VAL1, "val1");
    EntityKey<List<Entity>> listKey = EntityKey.entityList("list", null);
    Entity connectionAttr = StoreBridge.buildFromProxy(DBIdentity.fromDBObject(SyncAttributes.CONNECTION));
    h2.setReferenceCollection(listKey, Collections.singleton(myTransaction.addEntity(connectionAttr)));
    Entity entity = h2.restore();

    assertNotNull(entity);
    assertEquals("val1", entity.get(VAL1));
    Entity e1 = entity.get(IDe);
    assertNotNull(e1);
    assertEquals("a", e1.get(ID1));
    assertEquals("b", e1.get(ID2));
    List<Entity> list = entity.get(listKey);
    assertNotNull(list);
    assertEquals(1, list.size());
    Entity element = list.get(0);
    ItemProxy proxy = element.get(StoreBridge.ORIGINAL_OBJECT);
    assertEquals(SyncAttributes.CONNECTION, DBIdentity.extractIdentifiedObject(Util.castNotNull(DBIdentity.class, proxy)));
    assertEquals(SyncAttributes.CONNECTION.getId(), element.get(StoreBridge.STORE_ID));
  }

  private EntityHolder add1(String id1, String id2) {
    EntityTransaction.IdentityBuilder builder = myTransaction.buildEntity(TYPE_1);
    assertNotNull(builder);
    return builder.addValue(ID1, id1).addValue(ID2, id2).create();
  }

  private EntityHolder add2(EntityHolder h1) {
    EntityTransaction.IdentityBuilder builder = myTransaction.buildEntity(TYPE_2);
    assertNotNull(builder);
    return builder.addReference(IDe, h1).create();
  }
}
