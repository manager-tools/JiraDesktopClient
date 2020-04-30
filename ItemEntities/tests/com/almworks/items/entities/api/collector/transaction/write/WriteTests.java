package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.Collector2TestConsts;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.edit.SyncFixture;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.tests.CollectionsCompare;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

public class WriteTests extends SyncFixture implements Collector2TestConsts {
  private static final DBNamespace NS = DBNamespace.moduleNs("test");
  private static final DBAttribute<String> aID1 = StoreBridge.toScalarAttribute(NS, ID1);
  private static final DBAttribute<String> aID2 = StoreBridge.toScalarAttribute(NS, ID2);
  private static final DBAttribute<String> asID = StoreBridge.toScalarAttribute(NS, sID);
  private static final DBAttribute<Long> aIDe = StoreBridge.toLinkAttribute(NS, IDe);
  private static final DBItemType dType1 = StoreBridge.toDBType(TYPE_1, NS);
  private static final DBIdentifiedObject CONNECTION = NS.object("sampleConnection");
  private static final CollectionsCompare CHECK = new CollectionsCompare();
  private EntityTransaction myTransaction;

  protected void setUp() throws Exception {
    super.setUp();
    createTransaction();
  }

  private void createTransaction() {
    myTransaction = new EntityTransaction();
  }

  public void testIdentify() {
    final EntityHolder h1 = add1("a", "b");
    final EntityHolder h2 = add2(h1);
    new WriteTransaction() {
      @Override
      protected void doTest(EntityWriter writer) {
        CHECK.empty(writer.getUncreatable());
        writer.write();
        ItemVersion item1 = forItem(h1);
        assertEquals("a", item1.getValue(aID1));
        assertEquals("b", item1.getValue(aID2));
        ItemVersion item2 = forItem(h2);
        assertEquals(Long.valueOf(item1.getItem()), item2.getValue(aIDe));
        assertEquals(findType(TYPE_1), item1.getValue(DBAttribute.TYPE));
        assertEquals(findType(TYPE_2), item2.getValue(DBAttribute.TYPE));
        assertEquals((Object) myDrain.materialize(CONNECTION), item1.getValue(SyncAttributes.CONNECTION));
        assertEquals((Object) myDrain.materialize(CONNECTION), item2.getValue(SyncAttributes.CONNECTION));
      }
    }.waitWrite();
  }

  public void testWriteScalars() {
    final EntityKey<Boolean> boolS = EntityKey.bool("boolShadow", EntityKeyProperties.shadowable());
    final EntityKey<Boolean> boolN = EntityKey.bool("bool", null);
    final EntityKey<Integer> intS = EntityKey.integer("intShadow", EntityKeyProperties.shadowable());
    final EntityKey<Integer> intN = EntityKey.integer("int", null);
    final EntityKey<String> strS = EntityKey.string("strShadow", EntityKeyProperties.shadowable());
    final EntityKey<String> strN = EntityKey.string("str", null);
    final EntityKey<Date> dateS = EntityKey.date("dateShadow", EntityKeyProperties.shadowable());
    final EntityKey<Date> dateN = EntityKey.date("date", null);
    {
      final EntityHolder holder = add1("a", "b");
      holder.setValue(boolS, true);
      holder.setValue(boolN, false);
      holder.setValue(intS, 1);
      holder.setValue(intN, 2);
      holder.setValue(strS, "1");
      holder.setValue(strN, "2");
      holder.setValue(dateS, new Date(1));
      holder.setValue(dateN, new Date(2));
      new WriteTransaction() {
        @Override
        protected void doTest(EntityWriter writer) {
          writer.write();
          checkScalarValue(true, boolS, holder);
          checkScalarValue(false, boolN, holder);
          checkScalarValue(1, intS, holder);
          checkScalarValue(2, intN, holder);
          checkScalarValue("1", strS, holder);
          checkScalarValue("2", strN, holder);
          checkScalarValue(new Date(1), dateS, holder);
          checkScalarValue(new Date(2), dateN, holder);
        }
      }.waitWrite();
    }
    createTransaction();
    {
      final EntityHolder holder = add1("a", "b");
      holder.setValue(boolS, null);
      holder.setValue(boolN, null);
      holder.setValue(intS, 2);
      holder.setValue(intN, 1);
      new WriteTransaction() {
        @Override
        protected void doTest(EntityWriter writer) {
          writer.ensureResolved();
          checkScalarValue(true, boolS, holder);
          checkScalarValue(false, boolN, holder);
          checkScalarValue(1, intS, holder);
          checkScalarValue(2, intN, holder);
          checkScalarValue("1", strS, holder);
          checkScalarValue("2", strN, holder);
          checkScalarValue(new Date(1), dateS, holder);
          checkScalarValue(new Date(2), dateN, holder);

          writer.write();
          checkScalarValue(null, boolS, holder);
          checkScalarValue(null, boolN, holder);
          checkScalarValue(2, intS, holder);
          checkScalarValue(1, intN, holder);
          checkScalarValue("1", strS, holder);
          checkScalarValue("2", strN, holder);
        }
      }.waitWrite();
    }
  }

  public void testWriteCollections() {
    final EntityKey<Collection<Entity>> colS = EntityKey.entityCollection("colS", EntityKeyProperties.shadowable());
    final EntityKey<Collection<Entity>> colN = EntityKey.entityCollection("colN", null);
    final EntityHolder holder = add1("a", "b");
    final EntityHolder h1 = add1("1", "1");
    final EntityHolder h2 = add1("2", "2");
    holder.setReferenceCollection(colS, Arrays.asList(h1, h2));
    holder.setReferenceCollection(colN, Arrays.asList(h1, h2));
    new WriteTransaction() {
      @Override
      protected void doTest(EntityWriter writer) {
        writer.write();
        checkSetValue(holder, colS, h1, h2);
        checkSetValue(holder, colN, h1, h2);
      }
    }.waitWrite();
  }

  public void testDeleteBags() {
    {
      add1("a", "b");
      add1("a", "c");
      add1("b", "c");
      writeDB();
    }
    createTransaction();
    {
      final EntityHolder h1 = add1("a", "b");
      final EntityHolder h2 = add1("a", "c");
      final EntityHolder h3 = add1("b", "c");
      myTransaction.addBagScalar(TYPE_1, ID1, "a").exclude(h2).delete();
      new WriteTransaction() {
        @Override
        protected void doTest(EntityWriter writer) {
          writer.ensureResolved();
          assertTrue(forItem(h1).isAlive());
          assertTrue(forItem(h2).isAlive());
          assertTrue(forItem(h3).isAlive());
          writer.write();
          assertFalse(forItem(h1).isAlive());
          assertTrue(forItem(h2).isAlive());
          assertTrue(forItem(h3).isAlive());
        }
      }.waitWrite();
    }
  }

  public void testChangeBags() {
    {
      EntityHolder h1 = add1("a", "b");
      h1.setValue(sID, "1");
      h1.setReference(IDe, h1);
      EntityHolder h2 = add1("a", "c");
      h2.setValue(sID, "2");
      h2.setReference(IDe, h1);
      EntityHolder h3 = add1("b", "b");
      h3.setValue(sID, "3");
      h3.setReference(IDe, h1);
      writeDB();
    }
    createTransaction();
    {
      final EntityHolder h1 = add1("a", "b");
      final EntityHolder h2 = add1("a", "c");
      final EntityHolder h3 = add1("b", "b");
      EntityBag2 bag = myTransaction.addBagScalar(TYPE_1, ID1, "a");
      bag.exclude(h2);
      bag.changeValue(sID, "0");
      bag.changeValue(IDe, null);
      new WriteTransaction() {
        @Override
        protected void doTest(EntityWriter writer) {
          writer.write();
          checkScalarValue("0", sID, h1);
          checkScalarValue("2", sID, h2);
          checkScalarValue("3", sID, h3);
          long i1 = forItem(h1).getItem();
          checkRefValue(0, IDe, h1);
          checkRefValue(i1, IDe, h2);
          checkRefValue(i1, IDe, h3);
        }
      }.waitWrite();
    }
  }

  public void testDBIdentifiedObject() {
    DBIdentifiedObject o1 = NS.object("abc1");
    DBIdentifiedObject o2 = NS.object("abc2");
    {
      final EntityHolder h1 = myTransaction.addIdentifiedObject(o1);
      h1.setValue(ID1, "a");
      final EntityHolder h2 = myTransaction.addIdentifiedObject(o2);
      h2.setValue(ID2, "b");
      new WriteTransaction() {
        @Override
        protected void doTest(EntityWriter writer) {
          writer.write();
          checkScalarValue("a", ID1, h1);
          checkScalarValue("b", ID2, h2);
          checkScalarValue(null, ID2, h1);
          checkScalarValue(null, ID1, h2);
        }
      }.waitWrite();
    }
    createTransaction();
    {
      final EntityHolder h1 = myTransaction.addIdentifiedObject(o1);
      h1.setValue(ID1, null);
      final EntityHolder h2 = myTransaction.addIdentifiedObject(o2);
      new WriteTransaction() {
        @Override
        protected void doTest(EntityWriter writer) {
          writer.write();
          checkScalarValue(null, ID1, h1);
          checkScalarValue("b", ID2, h2);
          checkScalarValue(null, ID2, h1);
          checkScalarValue(null, ID1, h2);
        }
      }.waitWrite();
    }
  }

  public void testKeyAndTypeValues() {
    final EntityKey<Entity> tVal1 = EntityKey.entity("typeValue1", null);
    final EntityKey<Entity> tVal2 = EntityKey.entity("typeValue2", null);
    final EntityKey<Entity> kVal1 = EntityKey.entity("keyValue1", null);
    final EntityKey<Entity> kVal2 = EntityKey.entity("keyValue2", null);
    final EntityHolder holder = add1("a", "b");
    holder.setValue(kVal1, ID1.toEntity());
    holder.setValue(kVal2, IDe.toEntity());
    holder.setValue(tVal1, TYPE_1);
    holder.setValue(tVal2, TYPE_2);
    new WriteTransaction() {
      @Override
      protected void doTest(EntityWriter writer) {
        writer.write();
        checkRefValue(findType(TYPE_1), tVal1, holder);
        checkRefValue(findType(TYPE_2), tVal2, holder);
        checkRefValue(findKey(ID1), kVal1, holder);
        checkRefValue(findKey(IDe), kVal2, holder);
      }
    }.waitWrite();
  }

  public void testClearNoValue() {
    final EntityKey<String> shadowable = EntityKey.string("shadowable", EntityKeyProperties.shadowable());
    add1("a", "b").setValue(sID, "1").setValue(shadowable, "1");
    add1("a", "c").setValue(sID, "2").setValue(shadowable, "2");
    add1("a", "d").setValue(sID, "3").setValue(shadowable, "3");
    writeDB();

    createTransaction();
    final EntityHolder h1 = add1("a", "b");
    h1.setValue(sID, "10").setValue(shadowable, "10");
    final EntityHolder h2 = add1("a", "c");
    final EntityHolder h3 = add1("a", "d");
    new WriteTransaction() {
      @Override
      protected void doTest(EntityWriter writer) {
        DBAttribute<?> attrSID = getAttribute(sID);
        DBAttribute<?> attrShadowable = getAttribute(shadowable);
        writer.clearNoValue(h2, Arrays.asList(attrSID, attrShadowable));
        writer.write();
        checkScalarValue("10", sID, h1);
        checkScalarValue(null, sID, h2);
        checkScalarValue(null, shadowable, h2);
        checkScalarValue("3", sID, h3);
      }
    }.waitWrite();

    createTransaction();
    final EntityHolder holder = add1("a", "b");
    new WriteTransaction() {
      @Override
      protected void doTest(EntityWriter writer) {
        checkScalarValue("10", sID, holder);
        checkScalarValue("10", shadowable, holder);
        DBAttribute<?> attrSID = getAttribute(sID);
        DBAttribute<?> attrShadowable = getAttribute(shadowable);
        writer.clearNoValue(holder, Arrays.asList(attrSID, attrShadowable));
        writer.write();
        checkScalarValue(null, sID, holder);
        checkScalarValue(null, shadowable, holder);
      }
    }.waitWrite();
  }

  public void testExternalResolve() {
    EntityKey<String> key = EntityKey.string("sample", null);
    final DBAttribute<String> attribute = StoreBridge.toScalarAttribute(NS, key);
    final long item[] = new long[1];
    myManager.writeDownloaded(new DownloadProcedure<DBDrain>() {
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        ItemVersionCreator creator = drain.createItem();
        creator.setValue(DBAttribute.TYPE, dType1);
        creator.setValue(SyncAttributes.CONNECTION, drain.materialize(CONNECTION));
        creator.setValue(attribute, "1");
        item[0] = creator.getItem();
      }

      @Override
      public void onFinished(DBResult<?> result) {
      }
    }).waitForCompletion();
    final EntityHolder holder = add1("a", "b");
    new WriteTransaction() {
      @Override
      protected void doTest(EntityWriter writer) {
        writer.ensureResolved();
        Collection<EntityHolder> uncreatable = writer.getUnresolved(TYPE_1);
        assertEquals(1,uncreatable.size());
        assertSame(holder.getPlace(), uncreatable.iterator().next().getPlace());

        writer.addExternalResolution(holder, item[0]);
        assertEquals(0, writer.getUnresolved(TYPE_1).size());
        writer.write();
        assertEquals(item[0], forItem(holder).getItem());
      }
    }.waitWrite();
  }

  public void testIdentifyByPrimary1() {
    add3("a", "X");
    writeDB();

    createTransaction();
    add3("b", "X");
    writeDB();
  }

  public void testIdentifyByPrimary2() {
    add1("a", "b").setValue(sID, "X");
    writeDB();

    createTransaction();
    add1("a", "c").setNNValue(sID, "X");
    writeDB();
  }

  public void testIdentityConflict() {
    EntityKey<String> idConst = EntityKey.string("idConst", null);
    EntityKey<String> idMutable = EntityKey.string("idMutable", EntityKey.buildKey().put(EntityHolder.MUTABLE_IDENTITY, true));
    Entity type = Entity.buildType("twoIdentityType")
      .put(EntityResolution.KEY, EntityResolution.singleAttributeIdentities(true, idConst, idMutable))
      .fix();
    
    addEntity(type, idConst, "a").setValue(idMutable, "X");
    writeDB();
    long itemA = findSingle(idConst, "a");
    assertEquals("X", readValue(itemA, idMutable));
    
    createTransaction();
    addEntity(type, idConst, "a").setValue(idMutable, "Y");
    addEntity(type, idConst, "b").setValue(idMutable, "X");
    writeDB();
    assertEquals(itemA, findSingle(idConst, "a"));
    long itemB = findSingle(idConst, "b");
    assertEquals("Y", readValue(itemA, idMutable));
    assertEquals("X", readValue(itemB, idMutable));
  }
  
  private <T> long findSingle(EntityKey<T> key, final T value) {
    final LongArray result = new LongArray();
    final DBAttribute<T> attribute = StoreBridge.toScalarAttribute(NS, key);
    myManager.enquireRead(DBPriority.FOREGROUND, new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        LongArray items = reader.query(DPEqualsIdentified.create(SyncAttributes.CONNECTION, CONNECTION)
          .and(DPEquals.create(attribute, value))).copyItemsSorted();
        result.addAll(items);
        return null;
      }
    }).waitForCompletion();
    assertEquals("Size: " + result.size(), 1, result.size());
    return result.get(0);
  }
  
  private <T> T readValue(final long item, EntityKey<T> key) {
    final DBAttribute<T> attribute = StoreBridge.toScalarAttribute(NS, key);
    final T[] result = (T[]) new Object[1];
    myManager.enquireRead(DBPriority.FOREGROUND, new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        result[0] = reader.getValue(item, attribute);
        return null;
      }
    }).waitForCompletion();
    return result[0];
  }
  
  private void writeDB() {
    new WriteTransaction() {
      @Override
      protected void doTest(EntityWriter writer) {
        writer.write();
      }
    }.waitWrite();
  }

  private EntityHolder add2(EntityHolder h1) {
    EntityTransaction.IdentityBuilder builder = myTransaction.buildEntity(TYPE_2);
    assertNotNull(builder);
    return builder.addReference(IDe, h1).create();
  }

  private EntityHolder add3(String id, String search) {
    EntityHolder holder = addEntity(TYPE_3, ID1, id);
    holder.setValue(sID, search);
    return holder;
  }

  private EntityHolder addEntity(Entity type, EntityKey<String> key, String id) {
    EntityHolder holder = myTransaction.addEntity(type, key, id);
    assertNotNull(holder);
    return holder;
  }

  private EntityHolder add1(String id1, String id2) {
    EntityTransaction.IdentityBuilder builder = myTransaction.buildEntity(TYPE_1);
    assertNotNull(builder);
    return builder.addValue(ID1, id1).addValue(ID2, id2).create();
  }

  private Entity identified1(String id1, String id2) {
    return new Entity(TYPE_1).put(ID1, id1).put(ID2, id2);
  }

  private Entity identified2(Entity idE) {
    return new Entity(TYPE_2).put(IDe, idE);
  }

  private Entity searchable(String sID) {
    return new Entity(TYPE_1).put(Collector2TestConsts.sID, sID);
  }
  
  private abstract class WriteTransaction implements DownloadProcedure<DBDrain> {
    protected DBDrain myDrain;
    private EntityWriter myWriter;

    private WriteTransaction() {
    }

    @Override
    public void write(DBDrain drain) throws DBOperationCancelledException {
      myDrain = drain;
      myWriter = myTransaction.prepareWrite(drain, NS, DBIdentity.fromDBObject(CONNECTION));
      doTest(myWriter);
    }

    protected abstract void doTest(EntityWriter writer);

    @Override
    public void onFinished(DBResult<?> result) {
    }

    public void waitWrite() {
      DBResult<Object> result = myManager.writeDownloaded(this);
      result.waitForCompletion();
      if (!result.isSuccessful()) throw new RuntimeException(result.getError());
    }

    protected ItemVersion forItem(EntityHolder holder) {
      long item = myWriter.getItem(holder);
      assertTrue(String.valueOf(item) + " " + holder, item > 0);
      return myDrain.forItem(item);
    }

    protected DBAttribute<?> getAttribute(EntityKey<?> key) {
      return myWriter.getAttributeCache().getCounterpart(KeyInfo.create(key)).getAttribute();
    }
    
    protected <T> void checkScalarValue(@Nullable T expected, EntityKey<T> key, EntityHolder holder) {
      ItemVersion item = forItem(holder);
      DBAttribute<T> attribute = StoreBridge.toScalarAttribute(NS, key);
      assertEquals(expected, item.getValue(attribute));
    }

    protected void checkRefValue(long expected, EntityKey<Entity> key, EntityHolder holder) {
      ItemVersion item = forItem(holder);
      DBAttribute<Long> attribute = StoreBridge.toLinkAttribute(NS, key);
      Long longExpected = Long.valueOf(expected);
      if (longExpected <= 0) longExpected = null;
      Long actual = item.getValue(attribute);
      assertEquals(longExpected, actual);
    }

    protected void checkSetValue(EntityHolder holder, EntityKey<? extends Collection<? extends Entity>> key, EntityHolder ... expected) {
      ItemVersion item = forItem(holder);
      DBAttribute<Set<Long>> attribute = StoreBridge.toLinkSetAttribute(NS, key);
      LongList value = item.getLongSet(attribute);
      LongArray array = new LongArray();
      for (EntityHolder h : expected) {
        long itm = myWriter.getItem(h);
        assertTrue(h.toString(), itm > 0);
        array.add(itm);
      }
      array.sortUnique();
      CHECK.unordered(array.toNativeArray(), value.toNativeArray());
    }
    
    protected Long findType(Entity type) {
      long materialized = myDrain.findMaterialized(StoreBridge.toDBType(type, NS));
      assertTrue(type.toString(), materialized > 0);
      return materialized;
    }
    
    protected Long findKey(EntityKey<?> key) {
      DBAttribute<?> attribute = StoreBridge.toAttribute(key, NS);
      assertNotNull(key.toString(), attribute);
      long materialized = myDrain.findMaterialized(attribute);
      assertTrue(key.toString(), materialized > 0);
      return materialized;
    }
  }
}
