package com.almworks.items.entities.api.collector;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.EntityValueMerge;
import com.almworks.items.entities.api.collector.typetable.EntityCollector2;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Util;

import java.util.Arrays;

@SuppressWarnings("unchecked")
public class EntityCollector2Tests extends BaseTestCase implements Collector2TestConsts {

  private EntityCollector2 myCollector;

  protected void setUp() throws Exception {
    super.setUp();
    myCollector = new EntityCollector2();
  }

  public void testSimpleMergeIdentity() {
    EntityPlace e1 = addEntity(searchable("s1"));
    EntityPlace e2 = addEntity(identified1("i1-1", "i2-1"));
    assertNotEqual(e1.getIndex(), e2.getIndex());
    checkIdentify(e1, searchable("s1"));
    checkIdentify(e2, identified1("i1-1", "i2-1"));

    EntityPlace e3 = addEntity(identified1("i1-1", "i2-1").put(sID, "s1"));
    assertSamePlace(e1, e3);
    assertSamePlace(e2, e3);
    assertSamePlace(e1, e2);

    EntityPlace e21 = addEntity(identified1("a", "b"));
    EntityPlace e22 = addEntity(searchable("s2"));
    assertNotEqual(e21.getIndex(), e22.getIndex());
    assertNotEqual(e3.getIndex(), e22.getIndex());
    assertNotEqual(e3.getIndex(), e21.getIndex());
    EntityPlace e23 = addEntity(identified1("a", "b").put(sID, "s2"));
    assertSamePlace(e21, e23);
    assertSamePlace(e22, e23);
    assertSamePlace(e21, e22);
  }

  public void testSecondLevelMergeIdentity() {
    EntityPlace e1 = addEntity(identified2(identified1("a", "b")));
    EntityPlace e2 = addEntity(identified2(searchable("s")));
    assertNotEqual(e1.getIndex(), e2.getIndex());
    checkIdentify(e1, identified2(identified1("a", "b")));
    checkIdentify(e2, identified2(searchable("s")));

    EntityPlace e3 = addEntity(identified2(identified1("a", "b").put(sID, "s")));
    myCollector.mergeIdentities();
    assertSamePlace(e1, e3);
    assertSamePlace(e2, e3);
  }

  public void testAddValues() {
    EntityPlace e1 = addEntity(identified1("a", "b"));
    assertNull(getValue(e1, iVAL1));
    assertSamePlace(e1, addEntity(identified1("a", "b").put(VAL1, "v1")));
    assertEquals("v1", getValue(e1, iVAL1));
    assertSamePlace(e1, addEntity(identified1("a", "b").put(VAL2, "v2")));
    assertEquals("v1", getValue(e1, iVAL1));
    assertEquals("v2", getValue(e1, iVAL2));
  }

  public void testMergeValues() {
    EntityPlace e1 = addEntity(identified1("a", "b").put(VAL1, "v1"));
    addEntity(searchable("s").put(VAL2, "v2"));
    assertSamePlace(e1, addEntity(identified1("a", "b").put(sID, "s")));
    assertEquals("v1", getValue(e1, iVAL1));
    assertEquals("v2", getValue(e1, iVAL2));
  }

  public void testMergeDifferentValues() {
    Entity keyEntity = EntityKey.buildKey().put(EntityValueMerge.KEY, new EntityValueMerge() {
      @SuppressWarnings({"unchecked"})
      @Override
      public <T> T mergeValues(T value1, T value2) {
        if (value1 == value2)
          return value1;
        if (value1 == null || value2 == null)
          return value1 != null ? value1 : value2;
        Integer int1 = Util.castNullable(Integer.class, value1);
        Integer int2 = Util.castNullable(Integer.class, value2);
        Integer result;
        if (int1 == null || int2 == null)
          result = int1 == null ? int2 : int1;
        else
          result = Math.max(int1, int2);
        return (T) result;
      }
    });
    EntityKey<Integer> key = EntityKey.hint("test.maxInt", Integer.class, keyEntity);
    KeyInfo iKey = KeyInfo.create(key);
    EntityPlace e1 = addEntity(identified1("a", "b").put(key, 1));
    assertEquals(1, getValue(e1, iKey));
    assertSamePlace(e1, addEntity(identified1("a", "b")));
    assertSamePlace(e1, addEntity(identified1("a", "b").put(key, null)));
    assertEquals(1, getValue(e1, iKey));
    assertSamePlace(e1, addEntity(identified1("a", "b").put(key, 0)));
    assertEquals(1, getValue(e1, iKey));
    assertSamePlace(e1, addEntity(identified1("a", "b").put(key, 2)));
    assertEquals(2, getValue(e1, iKey));
  }

  public void testMergeTwoIdentities() {
    EntityKey<String> priId = EntityKey.string("priId", null);
    EntityKey<String> secId = EntityKey.string("secId", null);
    Entity type = Entity.buildType("type");
    type.put(EntityResolution.KEY, EntityResolution.searchable(true, Arrays.asList(secId), priId));
    type.fix();

    EntityPlace place1 = myCollector.addEntity(new Entity(type).put(priId, "1").put(secId, "a"));
    EntityPlace place2 = myCollector.addEntity(new Entity(type).put(priId, "2"));
    KeyInfo secInfo = myCollector.getKnownKeyInfo(secId);
    assertEquals("a", place1.getValue(secInfo));
    place2.setConvertedValue(secInfo, "a");
    assertTrue(place1.getIndex() != place2.getIndex());

    EntityPlace place3 = myCollector.addEntity(new Entity(type).put(secId, "a"));
    // Behaviour of place3 is not clear now
  }
  
  private Object getValue(EntityPlace place, KeyInfo info) {
    return place.getValue(info);
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

  private EntityPlace addEntity(Entity entity) {
    return myCollector.addEntity(entity);
  }

  private void checkIdentify(EntityPlace expected, Entity test) {
    assertSamePlace(expected, myCollector.addEntity(test));
  }

  private void assertSamePlace(EntityPlace e1, EntityPlace e2) {
    assertEquals(e1 + " == " + e2, e1.getIndex(), e2.getIndex());
  }
}
