package com.almworks.items.sync.util;

import com.almworks.items.api.*;
import com.almworks.items.sync.edit.SyncFixture;
import com.almworks.items.sync.util.identity.DBIdentity;

import java.util.concurrent.ExecutionException;

public class DBIdentityTests extends SyncFixture {
  public void testDBObjects() throws ExecutionException, InterruptedException {
    DBAttribute<String> attr = TEST_NS.string("attr");
    DBIdentifiedObject obj1 = TEST_NS.object("obj1");
    DBIdentity id1 = DBIdentity.fromDBObject(obj1);
    obj1.initialize(attr, "1");
    DBIdentifiedObject obj2 = TEST_NS.object("obj2");
    DBIdentity id2 = DBIdentity.fromDBObject(obj2);
    obj2.initialize(attr, "2");

    long item1 = materialize(obj1);
    long item2 = materialize(id2);
    checkTrunk(item1, attr, "1");
    checkTrunk(item2, attr, "2");
    assertEquals(item1, materialize(id1));
    assertEquals(item2, materialize(obj2));
    assertEquals(id1, loadIdentity(item1));
    assertEquals(id2, loadIdentity(item2));
  }

  public void testDeep() throws ExecutionException, InterruptedException {
    DBAttribute<Long> link1 = TEST_NS.link("link1");
    DBAttribute<Long> link2 = TEST_NS.link("link2");
    DBIdentifiedObject obj = TEST_NS.object("obj");

    DBIdentity id1 = new DBIdentity.Builder().put(link1, obj).create();
    DBIdentity id2 = new DBIdentity.Builder().put(link1, obj).put(link2, id1).create();
    assertFalse(id1.equals(id2));

    long item2 = materialize(id2);
    long objItem = readAttribute(item2, link1);
    long item1 = readAttribute(item2, link2);
    assertEquals(objItem, readAttribute(item1, link1).longValue());
    assertEquals(objItem, materialize(obj));
    assertEquals(item1, materialize(id1));

    assertEquals(id1, loadIdentity(item1));
    assertEquals(id2, loadIdentity(item2));
  }

  public void testWrongResolution() {
    DBAttribute<String> str = TEST_NS.string("str");
    DBAttribute<Integer> integer = TEST_NS.integer("int");
    DBIdentity id1 = new DBIdentity.Builder().put(str, "a").put(integer, 1).create();
    DBIdentity id2 = new DBIdentity.Builder().put(str, "a").create();
    long item1 = materialize(id1);
    assertEquals(0, findMaterialized(id2));
    assertEquals(id1, loadIdentity(item1));
  }

  public DBIdentity loadIdentity(final long item) {
    return db.readForeground(new ReadTransaction<DBIdentity>() {
      @Override
      public DBIdentity transaction(DBReader reader) throws DBOperationCancelledException {
        return DBIdentity.load(reader, item);
      }
    }).waitForCompletion();
  }

  public long findMaterialized(final ItemReference item) {
    return db.readForeground(new ReadTransaction<Long>() {
      @Override
      public Long transaction(DBReader reader) throws DBOperationCancelledException {
        return item.findItem(reader);
      }
    }).waitForCompletion();
  }
}
